package com.bankops.modules.rule;

import com.bankops.modules.transaction.Transaction;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Operational Validation Rules Engine.
 * Note: These rule checks represent operational exception triggers for workflow triage,
 * not definitive proof of financial fraud or regulatory non-compliance.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RuleEngine {

    private final ObjectMapper objectMapper;

    @Value("${bankops.rules.demo-amount-threshold:50000.00}")
    private BigDecimal defaultAmountThreshold;

    @Value("${bankops.rules.allowed-currencies:USD,EUR,GBP,CAD}")
    private String defaultAllowedCurrencies;

    public List<RuleExecutionResult> evaluate(Transaction tx, List<ValidationRule> activeRules) {
        List<RuleExecutionResult> matchedRules = new ArrayList<>();

        for (ValidationRule rule : activeRules) {
            try {
                RuleExecutionResult result = evaluateSingleRule(tx, rule);
                if (result.isMatched()) {
                    matchedRules.add(result);
                }
            } catch (Exception ex) {
                log.error("Error evaluating rule '{}' for transaction '{}': {}",
                        rule.getRuleCode(), tx.getExternalReference(), ex.getMessage());
            }
        }

        return matchedRules;
    }

    private RuleExecutionResult evaluateSingleRule(Transaction tx, ValidationRule rule) {
        String code = rule.getRuleCode();

        switch (code) {
            case "RULE_AMOUNT_THRESHOLD":
                BigDecimal threshold = defaultAmountThreshold;
                if (rule.getParameters() != null && !rule.getParameters().isBlank()) {
                    try {
                        JsonNode node = objectMapper.readTree(rule.getParameters());
                        if (node.has("threshold")) {
                            threshold = new BigDecimal(node.get("threshold").asText());
                        }
                    } catch (Exception ignored) {}
                }
                if (tx.getAmount() != null && tx.getAmount().compareTo(threshold) > 0) {
                    return RuleExecutionResult.matched(rule,
                            String.format("Transaction amount %s %s exceeds demonstration threshold of %s",
                                    tx.getAmount(), tx.getCurrency(), threshold));
                }
                break;

            case "RULE_FAILED_STATUS":
                if ("FAILED".equalsIgnoreCase(tx.getStatus()) || "REJECTED".equalsIgnoreCase(tx.getStatus())) {
                    String reason = tx.getFailureReason() != null ? tx.getFailureReason() : "Processing gateway failed or rejected";
                    return RuleExecutionResult.matched(rule,
                            String.format("Transaction terminated in %s state: %s", tx.getStatus(), reason));
                }
                break;

            case "RULE_CURRENCY_CHECK":
                Set<String> allowed = Arrays.stream(defaultAllowedCurrencies.split(","))
                        .map(String::trim)
                        .map(String::toUpperCase)
                        .collect(Collectors.toSet());
                if (rule.getParameters() != null && !rule.getParameters().isBlank()) {
                    try {
                        JsonNode node = objectMapper.readTree(rule.getParameters());
                        if (node.has("allowedCurrencies") && node.get("allowedCurrencies").isArray()) {
                            allowed.clear();
                            node.get("allowedCurrencies").forEach(c -> allowed.add(c.asText().toUpperCase()));
                        }
                    } catch (Exception ignored) {}
                }
                if (tx.getCurrency() == null || !allowed.contains(tx.getCurrency().toUpperCase())) {
                    return RuleExecutionResult.matched(rule,
                            String.format("Currency '%s' is not in permitted settlement currencies: %s",
                                    tx.getCurrency(), allowed));
                }
                break;

            case "RULE_CHANNEL_LIMIT":
                BigDecimal maxMobile = new BigDecimal("25000.00");
                if (rule.getParameters() != null && !rule.getParameters().isBlank()) {
                    try {
                        JsonNode node = objectMapper.readTree(rule.getParameters());
                        if (node.has("maxMobileAmount")) {
                            maxMobile = new BigDecimal(node.get("maxMobileAmount").asText());
                        }
                    } catch (Exception ignored) {}
                }
                if ("MOBILE".equalsIgnoreCase(tx.getChannel()) && tx.getAmount() != null && tx.getAmount().compareTo(maxMobile) > 0) {
                    return RuleExecutionResult.matched(rule,
                            String.format("Mobile channel transaction of %s %s exceeds mobile limit of %s",
                                    tx.getAmount(), tx.getCurrency(), maxMobile));
                }
                break;

            default:
                // Custom or generic rule match based on JSON criteria if specified
                break;
        }

        return RuleExecutionResult.notMatched();
    }
}
