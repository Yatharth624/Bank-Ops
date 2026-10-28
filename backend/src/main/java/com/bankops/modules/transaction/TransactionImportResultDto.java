package com.bankops.modules.transaction;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionImportResultDto {
    private int totalRowsProcessed;
    private int successfullyImportedCount;
    private int duplicateSkippedCount;
    private int failedValidationCount;
    private int exceptionCasesTriggeredCount;
    @Builder.Default
    private List<RowErrorDetail> rowErrors = new ArrayList<>();
    private String idempotencyKey;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RowErrorDetail {
        private int rowNumber;
        private String externalReference;
        private String errorMessage;
    }
}
