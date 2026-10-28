package com.bankops.modules.dashboard;

import com.bankops.modules.exceptioncase.ExceptionCaseRepository;
import com.bankops.modules.transaction.Transaction;
import com.bankops.modules.transaction.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final TransactionRepository transactionRepository;
    private final ExceptionCaseRepository caseRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC);

    @Transactional(readOnly = true)
    public DashboardSummaryDto getSummary() {
        long totalTx = transactionRepository.count();
        long completedTx = transactionRepository.countByStatus("COMPLETED");
        long failedTx = transactionRepository.countByStatus("FAILED") + transactionRepository.countByStatus("REJECTED");

        long openCases = caseRepository.countByStatus("OPEN");
        long inReviewCases = caseRepository.countByStatus("IN_REVIEW");
        long resolvedCases = caseRepository.countByStatus("RESOLVED");
        long totalCases = openCases + inReviewCases + resolvedCases;

        double exceptionRate = totalTx > 0 ? ((double) totalCases / totalTx) * 100.0 : 0.0;
        // Round to 2 decimal places
        exceptionRate = Math.round(exceptionRate * 100.0) / 100.0;

        // Calculate average case resolution duration in hours
        List<Object[]> durations = caseRepository.findResolvedCaseDurations();
        double avgResolutionHours = 0.0;
        if (!durations.isEmpty()) {
            double totalMinutes = 0.0;
            for (Object[] pair : durations) {
                Instant start = (Instant) pair[0];
                Instant end = (Instant) pair[1];
                if (start != null && end != null) {
                    totalMinutes += Duration.between(start, end).toMinutes();
                }
            }
            avgResolutionHours = Math.round((totalMinutes / durations.size() / 60.0) * 10.0) / 10.0;
        }

        // Group by severity
        Map<String, Long> severityMap = new HashMap<>();
        caseRepository.countGroupBySeverity().forEach(row -> severityMap.put((String) row[0], (Long) row[1]));

        // Group by category
        Map<String, Long> categoryMap = new HashMap<>();
        caseRepository.countGroupByCategory().forEach(row -> categoryMap.put((String) row[0], (Long) row[1]));

        // Group transactions by status
        Map<String, Long> statusMap = new HashMap<>();
        transactionRepository.countGroupByStatus().forEach(row -> statusMap.put((String) row[0], (Long) row[1]));

        return DashboardSummaryDto.builder()
                .totalTransactions(totalTx)
                .successfulTransactions(completedTx)
                .failedTransactions(failedTx)
                .openExceptions(openCases)
                .inReviewExceptions(inReviewCases)
                .resolvedExceptions(resolvedCases)
                .exceptionRatePercentage(exceptionRate)
                .averageResolutionTimeHours(avgResolutionHours)
                .casesBySeverity(severityMap)
                .exceptionsByCategory(categoryMap)
                .transactionsByStatus(statusMap)
                .build();
    }

    @Transactional(readOnly = true)
    public List<TrendPointDto> getTrends(int days) {
        int windowDays = (days <= 0 || days > 90) ? 7 : days;
        Instant since = Instant.now().minus(windowDays, ChronoUnit.DAYS);

        List<Transaction> transactions = transactionRepository.findAll().stream()
                .filter(t -> t.getTransactionTimestamp().isAfter(since))
                .toList();

        Map<String, TrendPointDto> points = new LinkedHashMap<>();

        // Initialize timeline points
        for (int i = windowDays - 1; i >= 0; i--) {
            Instant date = Instant.now().minus(i, ChronoUnit.DAYS);
            String formatted = DATE_FORMATTER.format(date);
            points.put(formatted, TrendPointDto.builder().date(formatted).build());
        }

        for (Transaction tx : transactions) {
            String dateKey = DATE_FORMATTER.format(tx.getTransactionTimestamp());
            TrendPointDto point = points.get(dateKey);
            if (point != null) {
                point.setTotal(point.getTotal() + 1);
                if ("COMPLETED".equalsIgnoreCase(tx.getStatus())) {
                    point.setCompleted(point.getCompleted() + 1);
                } else if ("FAILED".equalsIgnoreCase(tx.getStatus()) || "REJECTED".equalsIgnoreCase(tx.getStatus())) {
                    point.setFailed(point.getFailed() + 1);
                }
            }
        }

        // Count exceptions by created date
        caseRepository.findAll().stream()
                .filter(c -> c.getCreatedAt().isAfter(since))
                .forEach(c -> {
                    String dateKey = DATE_FORMATTER.format(c.getCreatedAt());
                    TrendPointDto point = points.get(dateKey);
                    if (point != null) {
                        point.setExceptions(point.getExceptions() + 1);
                    }
                });

        return new ArrayList<>(points.values());
    }
}
