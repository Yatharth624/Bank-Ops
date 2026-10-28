package com.bankops.modules.transaction;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionDto {
    private UUID id;
    private String externalReference;
    private String accountReference;
    private BigDecimal amount;
    private String currency;
    private String transactionType;
    private String channel;
    private String status;
    private String merchantOrDestination;
    private String failureReason;
    private Instant transactionTimestamp;
    private Instant createdAt;
    private Instant updatedAt;

    public static TransactionDto fromEntity(Transaction tx) {
        return TransactionDto.builder()
                .id(tx.getId())
                .externalReference(tx.getExternalReference())
                .accountReference(tx.getAccountReference())
                .amount(tx.getAmount())
                .currency(tx.getCurrency())
                .transactionType(tx.getTransactionType())
                .channel(tx.getChannel())
                .status(tx.getStatus())
                .merchantOrDestination(tx.getMerchantOrDestination())
                .failureReason(tx.getFailureReason())
                .transactionTimestamp(tx.getTransactionTimestamp())
                .createdAt(tx.getCreatedAt())
                .updatedAt(tx.getUpdatedAt())
                .build();
    }
}
