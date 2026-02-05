package org.example.transactionapp.dto;

import org.example.transactionapp.entity.Transaction;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionResponse
        (
                UUID transactionId,
                BigDecimal amount,
                BigDecimal fee,
                String type,
                String status,
                Instant updatedAt
        ) {
    public static TransactionResponse from(Transaction tx) {
        return new TransactionResponse(
                tx.getId(),
                tx.getAmount(),
                tx.getFee(),
                tx.getType().name(),
                tx.getStatus().name(),
                tx.getUpdated()
        );
    }
}