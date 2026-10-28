package com.bankops.modules.transaction;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    Optional<Transaction> findByExternalReference(String externalReference);

    boolean existsByExternalReference(String externalReference);

    @Query("SELECT t FROM Transaction t WHERE " +
           "(:status IS NULL OR t.status = :status) AND " +
           "(:transactionType IS NULL OR t.transactionType = :transactionType) AND " +
           "(:channel IS NULL OR t.channel = :channel) AND " +
           "(:minAmount IS NULL OR t.amount >= :minAmount) AND " +
           "(:maxAmount IS NULL OR t.amount <= :maxAmount) AND " +
           "(:fromDate IS NULL OR t.transactionTimestamp >= :fromDate) AND " +
           "(:toDate IS NULL OR t.transactionTimestamp <= :toDate) AND " +
           "(:search IS NULL OR LOWER(t.externalReference) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(t.accountReference) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(t.merchantOrDestination) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Transaction> searchTransactions(
            @Param("status") String status,
            @Param("transactionType") String transactionType,
            @Param("channel") String channel,
            @Param("minAmount") BigDecimal minAmount,
            @Param("maxAmount") BigDecimal maxAmount,
            @Param("fromDate") Instant fromDate,
            @Param("toDate") Instant toDate,
            @Param("search") String search,
            Pageable pageable);

    long countByStatus(String status);

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.transactionTimestamp >= :since")
    long countTransactionsSince(@Param("since") Instant since);

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.status = :status AND t.transactionTimestamp >= :since")
    long countTransactionsByStatusSince(@Param("status") String status, @Param("since") Instant since);

    @Query("SELECT t.status, COUNT(t) FROM Transaction t GROUP BY t.status")
    List<Object[]> countGroupByStatus();
}
