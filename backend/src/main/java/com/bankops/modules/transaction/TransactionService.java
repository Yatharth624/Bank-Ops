package com.bankops.modules.transaction;

import com.bankops.common.ResourceNotFoundException;
import com.bankops.modules.audit.AuditService;
import com.bankops.modules.exceptioncase.ExceptionCaseService;
import com.bankops.modules.idempotency.IdempotencyRecord;
import com.bankops.modules.idempotency.IdempotencyService;
import com.bankops.modules.rule.RuleEngine;
import com.bankops.modules.rule.RuleExecutionResult;
import com.bankops.modules.rule.ValidationRule;
import com.bankops.modules.rule.ValidationRuleService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final TransactionCsvParser csvParser;
    private final ValidationRuleService ruleService;
    private final RuleEngine ruleEngine;
    private final ExceptionCaseService caseService;
    private final AuditService auditService;
    private final IdempotencyService idempotencyService;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public Page<TransactionDto> searchTransactions(
            String status, String transactionType, String channel,
            BigDecimal minAmount, BigDecimal maxAmount,
            Instant fromDate, Instant toDate, String search, Pageable pageable) {
        return transactionRepository.searchTransactions(
                status, transactionType, channel, minAmount, maxAmount, fromDate, toDate, search, pageable)
                .map(TransactionDto::fromEntity);
    }

    @Transactional(readOnly = true)
    public TransactionDto getTransactionById(UUID id) {
        Transaction tx = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", "id", id));
        return TransactionDto.fromEntity(tx);
    }

    @Transactional
    public TransactionImportResultDto importTransactionsCsv(InputStream csvStream, String idempotencyKey) {
        // 1. Check Idempotency Cache
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            Optional<IdempotencyRecord> existing = idempotencyService.getRecord(idempotencyKey);
            if (existing.isPresent()) {
                try {
                    log.info("Idempotent replay for import key '{}'", idempotencyKey);
                    return objectMapper.readValue(existing.get().getResponsePayload(), TransactionImportResultDto.class);
                } catch (Exception ex) {
                    log.warn("Failed to deserialize idempotency payload, continuing import", ex);
                }
            }
        }

        // 2. Parse CSV
        TransactionCsvParser.ParseResult parseResult = csvParser.parse(csvStream);
        List<ValidationRule> activeRules = ruleService.getActiveRules();

        int importedCount = 0;
        int duplicateCount = 0;
        int exceptionCasesCreated = 0;

        for (Transaction tx : parseResult.validTransactions) {
            // Check for duplicate external reference
            if (transactionRepository.existsByExternalReference(tx.getExternalReference())) {
                duplicateCount++;
                parseResult.rowErrors.add(new TransactionImportResultDto.RowErrorDetail(
                        0, tx.getExternalReference(), "Duplicate transaction external reference detected. Skipped."));
                continue;
            }

            // Persist valid synthetic transaction
            Transaction saved = transactionRepository.save(tx);
            importedCount++;

            // Evaluate operational rules
            List<RuleExecutionResult> matches = ruleEngine.evaluate(saved, activeRules);
            for (RuleExecutionResult match : matches) {
                var createdCase = caseService.createCaseForTransaction(saved, match.getRule(), match.getFailureReason());
                if (createdCase.isPresent()) {
                    exceptionCasesCreated++;
                }
            }
        }

        TransactionImportResultDto result = TransactionImportResultDto.builder()
                .totalRowsProcessed(parseResult.totalRows)
                .successfullyImportedCount(importedCount)
                .duplicateSkippedCount(duplicateCount)
                .failedValidationCount(parseResult.rowErrors.size() - duplicateCount)
                .exceptionCasesTriggeredCount(exceptionCasesCreated)
                .rowErrors(parseResult.rowErrors)
                .idempotencyKey(idempotencyKey)
                .build();

        // 3. Log Audit Event
        auditService.logEvent("IMPORT_TRANSACTIONS_CSV", "TRANSACTION_BATCH",
                idempotencyKey != null ? idempotencyKey : "BATCH-" + System.currentTimeMillis(),
                String.format("Imported %d transactions, %d duplicates skipped, %d validation errors, %d exceptions created",
                        importedCount, duplicateCount, result.getFailedValidationCount(), exceptionCasesCreated),
                "SUCCESS");

        // 4. Save Idempotency Record
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            try {
                idempotencyService.saveRecord(idempotencyKey, "TRANSACTION_IMPORT",
                        objectMapper.writeValueAsString(result), 200);
            } catch (Exception ex) {
                log.warn("Could not cache idempotency payload: {}", ex.getMessage());
            }
        }

        return result;
    }
}
