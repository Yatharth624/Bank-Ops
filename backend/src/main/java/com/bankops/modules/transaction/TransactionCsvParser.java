package com.bankops.modules.transaction;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class TransactionCsvParser {

    public static class ParseResult {
        public final List<Transaction> validTransactions = new ArrayList<>();
        public final List<TransactionImportResultDto.RowErrorDetail> rowErrors = new ArrayList<>();
        public int totalRows = 0;
    }

    public ParseResult parse(InputStream inputStream) {
        ParseResult result = new ParseResult();

        try (Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT
                     .builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .setIgnoreHeaderCase(true)
                     .setTrim(true)
                     .build())) {

            int rowNumber = 1;
            for (CSVRecord record : csvParser) {
                rowNumber++;
                result.totalRows++;
                try {
                    Transaction tx = parseRow(record, rowNumber);
                    result.validTransactions.add(tx);
                } catch (Exception ex) {
                    String ref = record.isSet("external_reference") ? record.get("external_reference") : "ROW-" + rowNumber;
                    result.rowErrors.add(new TransactionImportResultDto.RowErrorDetail(rowNumber, ref, ex.getMessage()));
                }
            }
        } catch (Exception ex) {
            log.error("Failed to parse CSV stream: {}", ex.getMessage());
            result.rowErrors.add(new TransactionImportResultDto.RowErrorDetail(0, "CSV_FILE", "Invalid CSV file format: " + ex.getMessage()));
        }

        return result;
    }

    private Transaction parseRow(CSVRecord record, int rowNumber) {
        String externalRef = getField(record, "external_reference", true, rowNumber);
        String accountRef = getField(record, "account_reference", true, rowNumber);
        String amountStr = getField(record, "amount", true, rowNumber);
        String currency = getField(record, "currency", true, rowNumber).toUpperCase();
        String txType = getField(record, "transaction_type", true, rowNumber).toUpperCase();
        String channel = getField(record, "channel", true, rowNumber).toUpperCase();
        String status = getField(record, "status", true, rowNumber).toUpperCase();
        String destination = record.isSet("merchant_or_destination") ? record.get("merchant_or_destination") : null;
        String failureReason = record.isSet("failure_reason") ? record.get("failure_reason") : null;
        String timestampStr = getField(record, "transaction_timestamp", false, rowNumber);

        BigDecimal amount;
        try {
            amount = new BigDecimal(amountStr);
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Amount must be greater than zero");
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid decimal amount: " + amountStr);
        }

        if (currency.length() != 3) {
            throw new IllegalArgumentException("Currency code must be a 3-letter ISO code: " + currency);
        }

        Instant timestamp;
        if (timestampStr != null && !timestampStr.isBlank()) {
            try {
                timestamp = Instant.parse(timestampStr);
            } catch (DateTimeParseException e) {
                try {
                    timestamp = Instant.ofEpochMilli(Long.parseLong(timestampStr));
                } catch (Exception ex2) {
                    throw new IllegalArgumentException("Invalid timestamp format (ISO-8601 required): " + timestampStr);
                }
            }
        } else {
            timestamp = Instant.now();
        }

        return Transaction.builder()
                .externalReference(externalRef)
                .accountReference(accountRef)
                .amount(amount)
                .currency(currency)
                .transactionType(txType)
                .channel(channel)
                .status(status)
                .merchantOrDestination(destination)
                .failureReason(failureReason)
                .transactionTimestamp(timestamp)
                .build();
    }

    private String getField(CSVRecord record, String fieldName, boolean required, int rowNumber) {
        if (!record.isSet(fieldName) || record.get(fieldName) == null || record.get(fieldName).isBlank()) {
            if (required) {
                throw new IllegalArgumentException(String.format("Row %d: Missing mandatory field '%s'", rowNumber, fieldName));
            }
            return null;
        }
        return record.get(fieldName).trim();
    }
}
