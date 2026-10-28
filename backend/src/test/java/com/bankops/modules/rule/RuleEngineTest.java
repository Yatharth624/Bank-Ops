package com.bankops.modules.rule;

import com.bankops.modules.transaction.Transaction;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RuleEngineTest {

    private RuleEngine ruleEngine;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        ruleEngine = new RuleEngine(objectMapper);
        ReflectionTestUtils.setField(ruleEngine, "defaultAmountThreshold", new BigDecimal("50000.00"));
        ReflectionTestUtils.setField(ruleEngine, "defaultAllowedCurrencies", "USD,EUR,GBP,CAD");
    }

    @Test
    @DisplayName("Should flag transaction exceeding demo amount threshold")
    void shouldFlagAmountThresholdBreach() {
        ValidationRule rule = ValidationRule.builder()
                .ruleCode("RULE_AMOUNT_THRESHOLD")
                .ruleName("High-Value Demo Threshold")
                .category("HIGH_VALUE_BREACH")
                .severity("HIGH")
                .parameters("{\"threshold\": 50000.00}")
                .active(true)
                .build();

        Transaction highValueTx = Transaction.builder()
                .externalReference("TX-HIGH-01")
                .accountReference("ACT-001")
                .amount(new BigDecimal("75000.00"))
                .currency("USD")
                .transactionType("WIRE_TRANSFER")
                .channel("BRANCH")
                .status("COMPLETED")
                .transactionTimestamp(Instant.now())
                .build();

        List<RuleExecutionResult> results = ruleEngine.evaluate(highValueTx, List.of(rule));

        assertThat(results).hasSize(1);
        assertThat(results.get(0).isMatched()).isTrue();
        assertThat(results.get(0).getCategory()).isEqualTo("HIGH_VALUE_BREACH");
        assertThat(results.get(0).getSeverity()).isEqualTo("HIGH");
    }

    @Test
    @DisplayName("Should not flag transaction within demo amount threshold")
    void shouldNotFlagNormalAmount() {
        ValidationRule rule = ValidationRule.builder()
                .ruleCode("RULE_AMOUNT_THRESHOLD")
                .active(true)
                .build();

        Transaction normalTx = Transaction.builder()
                .externalReference("TX-NORM-01")
                .amount(new BigDecimal("1200.00"))
                .currency("USD")
                .status("COMPLETED")
                .build();

        List<RuleExecutionResult> results = ruleEngine.evaluate(normalTx, List.of(rule));
        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("Should flag transaction with FAILED status")
    void shouldFlagProcessingFailure() {
        ValidationRule rule = ValidationRule.builder()
                .ruleCode("RULE_FAILED_STATUS")
                .ruleName("Processing Failure Detection")
                .category("PROCESSING_FAILURE")
                .severity("CRITICAL")
                .active(true)
                .build();

        Transaction failedTx = Transaction.builder()
                .externalReference("TX-FAIL-01")
                .amount(new BigDecimal("350.00"))
                .currency("USD")
                .status("FAILED")
                .failureReason("CLEARINGHOUSE_TIMEOUT")
                .build();

        List<RuleExecutionResult> results = ruleEngine.evaluate(failedTx, List.of(rule));

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getCategory()).isEqualTo("PROCESSING_FAILURE");
        assertThat(results.get(0).getSeverity()).isEqualTo("CRITICAL");
        assertThat(results.get(0).getFailureReason()).contains("CLEARINGHOUSE_TIMEOUT");
    }

    @Test
    @DisplayName("Should flag transaction with unapproved currency")
    void shouldFlagUnapprovedCurrency() {
        ValidationRule rule = ValidationRule.builder()
                .ruleCode("RULE_CURRENCY_CHECK")
                .category("CURRENCY_COMPLIANCE")
                .severity("MEDIUM")
                .parameters("{\"allowedCurrencies\": [\"USD\", \"EUR\"]}")
                .active(true)
                .build();

        Transaction foreignTx = Transaction.builder()
                .externalReference("TX-JPY-01")
                .amount(new BigDecimal("500000.00"))
                .currency("JPY")
                .status("COMPLETED")
                .build();

        List<RuleExecutionResult> results = ruleEngine.evaluate(foreignTx, List.of(rule));

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getCategory()).isEqualTo("CURRENCY_COMPLIANCE");
    }

    @Test
    @DisplayName("Should flag mobile transaction exceeding mobile channel threshold")
    void shouldFlagMobileChannelAnomaly() {
        ValidationRule rule = ValidationRule.builder()
                .ruleCode("RULE_CHANNEL_LIMIT")
                .category("CHANNEL_ANOMALY")
                .severity("LOW")
                .parameters("{\"maxMobileAmount\": 25000.00}")
                .active(true)
                .build();

        Transaction mobileTx = Transaction.builder()
                .externalReference("TX-MOB-01")
                .amount(new BigDecimal("30000.00"))
                .currency("USD")
                .channel("MOBILE")
                .status("COMPLETED")
                .build();

        List<RuleExecutionResult> results = ruleEngine.evaluate(mobileTx, List.of(rule));

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getCategory()).isEqualTo("CHANNEL_ANOMALY");
    }
}
