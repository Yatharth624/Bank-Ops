package com.bankops.modules.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummaryDto {
    private long totalTransactions;
    private long successfulTransactions;
    private long failedTransactions;
    private long openExceptions;
    private long inReviewExceptions;
    private long resolvedExceptions;
    private double exceptionRatePercentage; // (Total Exceptions / Total Transactions) * 100
    private double averageResolutionTimeHours; // Sum of (resolvedAt - createdAt) / count of resolved cases
    private Map<String, Long> casesBySeverity;
    private Map<String, Long> exceptionsByCategory;
    private Map<String, Long> transactionsByStatus;
}
