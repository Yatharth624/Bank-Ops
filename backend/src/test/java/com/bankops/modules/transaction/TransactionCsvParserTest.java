package com.bankops.modules.transaction;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionCsvParserTest {

    private TransactionCsvParser csvParser;

    @BeforeEach
    void setUp() {
        csvParser = new TransactionCsvParser();
    }

    @Test
    @DisplayName("Should successfully parse valid CSV rows")
    void shouldParseValidCsvRows() {
        String csv = "external_reference,account_reference,amount,currency,transaction_type,channel,status,merchant_or_destination,failure_reason,transaction_timestamp\n" +
                "TX-CSV-001,ACT-881,150.00,USD,PAYMENT,WEB,COMPLETED,AMAZON,,2026-10-25T10:00:00Z\n" +
                "TX-CSV-002,ACT-882,90000.00,EUR,WIRE_TRANSFER,BRANCH,COMPLETED,CORP_HQ,,2026-10-25T11:00:00Z\n";

        InputStream stream = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));
        TransactionCsvParser.ParseResult result = csvParser.parse(stream);

        assertThat(result.totalRows).isEqualTo(2);
        assertThat(result.validTransactions).hasSize(2);
        assertThat(result.rowErrors).isEmpty();

        Transaction tx1 = result.validTransactions.get(0);
        assertThat(tx1.getExternalReference()).isEqualTo("TX-CSV-001");
        assertThat(tx1.getAmount()).isEqualByComparingTo(new BigDecimal("150.00"));
        assertThat(tx1.getCurrency()).isEqualTo("USD");
    }

    @Test
    @DisplayName("Should report row-level errors for invalid amounts and missing mandatory fields")
    void shouldReportRowLevelErrors() {
        String csv = "external_reference,account_reference,amount,currency,transaction_type,channel,status,merchant_or_destination,failure_reason,transaction_timestamp\n" +
                "TX-GOOD-01,ACT-01,500.00,USD,PAYMENT,MOBILE,COMPLETED,GROCERY,,2026-10-25T12:00:00Z\n" +
                "TX-BAD-01,ACT-02,-50.00,USD,PAYMENT,WEB,COMPLETED,STORE,,2026-10-25T12:00:00Z\n" +
                ",ACT-03,100.00,USD,PAYMENT,WEB,COMPLETED,STORE,,2026-10-25T12:00:00Z\n" +
                "TX-BAD-CURR,ACT-04,100.00,US,PAYMENT,WEB,COMPLETED,STORE,,2026-10-25T12:00:00Z\n";

        InputStream stream = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));
        TransactionCsvParser.ParseResult result = csvParser.parse(stream);

        assertThat(result.totalRows).isEqualTo(4);
        assertThat(result.validTransactions).hasSize(1);
        assertThat(result.validTransactions.get(0).getExternalReference()).isEqualTo("TX-GOOD-01");

        // 3 errors captured
        assertThat(result.rowErrors).hasSize(3);
        assertThat(result.rowErrors).anyMatch(e -> e.getErrorMessage().contains("Amount must be greater than zero"));
        assertThat(result.rowErrors).anyMatch(e -> e.getErrorMessage().contains("Missing mandatory field 'external_reference'"));
        assertThat(result.rowErrors).anyMatch(e -> e.getErrorMessage().contains("3-letter ISO code"));
    }
}
