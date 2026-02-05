package org.example.transactionapp.dto;

import org.example.transactionapp.entity.TransactionStatus;

import java.util.UUID;

public record TransactionStatusResponseDTO
        (
                UUID transactionId,
                TransactionStatus status
        ){
}
