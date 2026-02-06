package org.example.transactionapp.rest;

import groovy.util.logging.Slf4j;
import lombok.RequiredArgsConstructor;
import org.example.transactionapp.dto.TransactionConfirmRequest;
import org.example.transactionapp.dto.TransactionConfirmResponse;
import org.example.transactionapp.dto.TransactionInitResponse;
import org.example.transactionapp.dto.TransactionStatusResponse;
import org.example.transactionapp.service.HasAmount;
import org.example.transactionapp.service.TransactionService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/transactions")
public class TransactionRestController {

    private final TransactionService transactionService;

    // Инициализация транзакции
    @PostMapping("/transactions/{type}/init")
    public ResponseEntity<TransactionInitResponse> initTransaction(@PathVariable String type,
                                                                   @RequestBody HasAmount request) {
        return ResponseEntity.ok(transactionService.init(type, request));
    }

    // Подтверждение транзакции
    @PostMapping("/transactions/{type}/confirm")
    public ResponseEntity<TransactionConfirmResponse> confirmTransaction(@PathVariable String type,
                                                                         @RequestBody TransactionConfirmRequest request) {
        return ResponseEntity.ok(transactionService.confirm(type, request));
    }

    // Получение статуса транзакции
    @GetMapping("/transactions/{transactionId}/status")
    public ResponseEntity<TransactionStatusResponse> getTransactionStatus(@PathVariable UUID transactionId) {
        return ResponseEntity.ok(transactionService.status(transactionId));
    }

}
