package com.bankops.modules.exceptioncase;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExceptionCaseRepository extends JpaRepository<ExceptionCase, UUID> {

    Optional<ExceptionCase> findByCaseNumber(String caseNumber);

    boolean existsByTransactionIdAndRuleId(UUID transactionId, UUID ruleId);

    List<ExceptionCase> findByTransactionId(UUID transactionId);

    @Query("SELECT c FROM ExceptionCase c " +
           "LEFT JOIN FETCH c.transaction t " +
           "LEFT JOIN FETCH c.assignedAnalyst u " +
           "WHERE (:status IS NULL OR c.status = :status) AND " +
           "(:severity IS NULL OR c.severity = :severity) AND " +
           "(:category IS NULL OR c.category = :category) AND " +
           "(:assignedAnalystId IS NULL OR (c.assignedAnalyst IS NOT NULL AND c.assignedAnalyst.id = :assignedAnalystId)) AND " +
           "(:search IS NULL OR LOWER(c.caseNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(t.externalReference) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(t.accountReference) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<ExceptionCase> searchCases(
            @Param("status") String status,
            @Param("severity") String severity,
            @Param("category") String category,
            @Param("assignedAnalystId") UUID assignedAnalystId,
            @Param("search") String search,
            Pageable pageable);

    long countByStatus(String status);

    @Query("SELECT c.status, COUNT(c) FROM ExceptionCase c GROUP BY c.status")
    List<Object[]> countGroupByStatus();

    @Query("SELECT c.severity, COUNT(c) FROM ExceptionCase c GROUP BY c.severity")
    List<Object[]> countGroupBySeverity();

    @Query("SELECT c.category, COUNT(c) FROM ExceptionCase c GROUP BY c.category")
    List<Object[]> countGroupByCategory();

    @Query("SELECT c.createdAt, c.resolutionTimestamp FROM ExceptionCase c WHERE c.status = 'RESOLVED' AND c.resolutionTimestamp IS NOT NULL")
    List<Object[]> findResolvedCaseDurations();
}
