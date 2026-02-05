package org.example.transactionapp.rest;

import lombok.RequiredArgsConstructor;
import org.example.transactionapp.dto.TransactionResponse;
import org.example.transactionapp.dto.TransactionStatusResponseDTO;
import org.example.transactionapp.entity.Transaction;
import org.example.transactionapp.service.TransactionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/transactions")
public class TransactionRestController {

    private final TransactionService transactionService;

    /**
     * GET /transactions/{transactionId}/status
     * Получение статуса транзакции
     */
    @GetMapping("/{transactionId}/status")
    public ResponseEntity<TransactionStatusResponseDTO> getStatus(@PathVariable UUID transactionId) {
        Transaction transaction = transactionService.getTransactionStatus(transactionId);
        return ResponseEntity.ok(new TransactionStatusResponseDTO(transaction.getId(), transaction.getStatus()));
    }
    /**
     * POST /transactions/{transactionId}/confirm
     * Подтверждение и выполнение транзакции
     */
    @PostMapping("/{transactionId}/confirm")
    public ResponseEntity<TransactionResponse> confirm(
            @PathVariable UUID transactionId) {

        Transaction transaction = transactionService.confirmTransaction(transactionId);

        return ResponseEntity.ok(TransactionResponse.from(transaction));
    }
}
