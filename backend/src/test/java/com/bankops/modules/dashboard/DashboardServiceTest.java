package com.bankops.modules.dashboard;

import com.bankops.modules.exceptioncase.ExceptionCaseRepository;
import com.bankops.modules.transaction.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class DashboardServiceTest {

    private TransactionRepository transactionRepository;
    private ExceptionCaseRepository caseRepository;
    private DashboardService dashboardService;

    @BeforeEach
    void setUp() {
        transactionRepository = Mockito.mock(TransactionRepository.class);
        caseRepository = Mockito.mock(ExceptionCaseRepository.class);
        dashboardService = new DashboardService(transactionRepository, caseRepository);
    }

    @Test
    @DisplayName("Should correctly calculate dashboard summary KPIs and exception rate formula")
    void shouldCalculateSummaryMetrics() {
        when(transactionRepository.count()).thenReturn(100L);
        when(transactionRepository.countByStatus("COMPLETED")).thenReturn(85L);
        when(transactionRepository.countByStatus("FAILED")).thenReturn(12L);
        when(transactionRepository.countByStatus("REJECTED")).thenReturn(3L);

        when(caseRepository.countByStatus("OPEN")).thenReturn(5L);
        when(caseRepository.countByStatus("IN_REVIEW")).thenReturn(3L);
        when(caseRepository.countByStatus("RESOLVED")).thenReturn(12L);

        // 2 resolved cases: one took 2 hours, one took 4 hours -> avg 3.0 hours
        Instant now = Instant.now();
        List<Object[]> durations = List.of(
                new Object[]{now.minus(2, ChronoUnit.HOURS), now},
                new Object[]{now.minus(4, ChronoUnit.HOURS), now}
        );
        when(caseRepository.findResolvedCaseDurations()).thenReturn(durations);

        when(caseRepository.countGroupBySeverity()).thenReturn(List.of(
                new Object[]{"HIGH", 8L},
                new Object[]{"CRITICAL", 12L}
        ));
        when(caseRepository.countGroupByCategory()).thenReturn(List.of(
                new Object[]{"PROCESSING_FAILURE", 15L},
                new Object[]{"HIGH_VALUE_BREACH", 5L}
        ));
        when(transactionRepository.countGroupByStatus()).thenReturn(List.of(
                new Object[]{"COMPLETED", 85L},
                new Object[]{"FAILED", 15L}
        ));

        DashboardSummaryDto summary = dashboardService.getSummary();

        assertThat(summary.getTotalTransactions()).isEqualTo(100L);
        assertThat(summary.getSuccessfulTransactions()).isEqualTo(85L);
        assertThat(summary.getFailedTransactions()).isEqualTo(15L);

        assertThat(summary.getOpenExceptions()).isEqualTo(5L);
        assertThat(summary.getInReviewExceptions()).isEqualTo(3L);
        assertThat(summary.getResolvedExceptions()).isEqualTo(12L);

        // Exception rate = (20 cases / 100 tx) * 100 = 20.0%
        assertThat(summary.getExceptionRatePercentage()).isEqualTo(20.0);

        // Average resolution time = (2h + 4h) / 2 = 3.0h
        assertThat(summary.getAverageResolutionTimeHours()).isEqualTo(3.0);

        assertThat(summary.getCasesBySeverity()).containsEntry("CRITICAL", 12L);
        assertThat(summary.getExceptionsByCategory()).containsEntry("PROCESSING_FAILURE", 15L);
    }
}
