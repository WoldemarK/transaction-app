package org.example.transactionapp.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.errors.ResourceNotFoundException;
import org.example.transactionapp.dto.*;
import org.example.transactionapp.entity.PaymentType;
import org.example.transactionapp.entity.Transaction;
import org.example.transactionapp.entity.TransactionStatus;
import org.example.transactionapp.entity.Wallet;
import org.example.transactionapp.metrics.Loggable;
import org.example.transactionapp.repository.TransactionRepository;
import org.example.transactionapp.repository.WalletRepository;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;


import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService{

    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;


    @Loggable("transaction.init")
    public TransactionInitResponse init(String type, HasAmount request) {
        PaymentType paymentType = PaymentType.valueOf(type.toUpperCase());
        BigDecimal amount = request.getAmount();
        BigDecimal fee = calculateFee(paymentType, amount);

        TransactionInitResponse response = new TransactionInitResponse();
        response.setTransactionId(UUID.randomUUID());
        response.setType(TransactionInitResponse.TypeEnum.fromValue(paymentType.name()));
        response.setAmount(amount);
        response.setFee(fee);
        response.setTotalAmount(amount.add(fee));

        return response;
    }


    @Loggable("transaction.confirm")
    public TransactionConfirmResponse confirm(String type, TransactionConfirmRequest request) {

        PaymentType paymentType = PaymentType.valueOf(type.toUpperCase());

        Transaction transaction = Transaction.builder()
                .id(request.getTransactionId())
                .userUid(request.getUserUid())
                .walletUid(request.getWalletUid())
                .targetWalletUid(request.getTargetWalletUid())
                .amount(request.getAmount())
                .fee(request.getFee())
                .type(paymentType)
                .status(TransactionStatus.PROCESSING)
                .createdAt(OffsetDateTime.now())
                .build();

        transactionRepository.save(transaction);

        switch (paymentType) {
            case DEPOSIT -> sendDepositRequested(transaction);
            case WITHDRAWAL -> sendWithdrawalRequested(transaction);
            case TRANSFER -> processTransfer(transaction);
        }
        TransactionConfirmResponse response = new TransactionConfirmResponse();
        response.setTransactionId(transaction.getId());
        response.setStatus(transaction.getStatus().toString());
        response.setConfirmedAt(OffsetDateTime.now());

        return response;
    }

    @Override
    @Loggable("transaction.status")
    public TransactionStatusResponse status(UUID transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id " + transactionId));

        TransactionStatusResponse response = new TransactionStatusResponse();
        response.setTransactionId(transaction.getId());
        response.setStatus(transaction.getStatus().name());
        response.setAmount(transaction.getAmount());
        response.setCreatedAt(transaction.getCreatedAt());
        return response;
    }

    private void processTransfer(Transaction transaction) {
        Wallet from = walletRepository.findByIdForUpdate(transaction.getWalletUid()).orElseThrow();
        Wallet to = walletRepository.findByIdForUpdate(transaction.getTargetWalletUid()).orElseThrow();

        BigDecimal total = transaction.getAmount().add(transaction.getFee());
        if (from.getBalance().compareTo(total) < 0) {
            transaction.setStatus(TransactionStatus.FAILED);
            transaction.setFailureReason("INSUFFICIENT_FUNDS");
            return;
        }

        from.setBalance(from.getBalance().subtract(total));
        to.setBalance(to.getBalance().add(transaction.getAmount()));

        walletRepository.save(from);
        walletRepository.save(to);

        transaction.setStatus(TransactionStatus.COMPLETED);
    }

    private void sendDepositRequested(Transaction transaction) {
        Wallet wallet = walletRepository.findByIdForUpdate(transaction.getWalletUid()).orElseThrow();

        DepositRequestedEvent requestedEvent = new DepositRequestedEvent();
        requestedEvent.setTransactionId(transaction.getId());
        requestedEvent.setUserId(transaction.getUserUid());
        requestedEvent.setWalletId(transaction.getWalletUid());
        requestedEvent.setAmount(transaction.getAmount().doubleValue());
        requestedEvent.setCurrency(wallet.getWalletType().getCurrencyCode());
        requestedEvent.setTimestamp(OffsetDateTime.now());

        kafkaTemplate.send("deposit.requested", requestedEvent);
    }

    private void sendWithdrawalRequested(Transaction transaction) {
        kafkaTemplate.send("withdrawal.requested", transaction.getId().toString(), transaction);
    }

    private BigDecimal calculateFee(PaymentType type, BigDecimal amount) {
        return switch (type) {
            case DEPOSIT -> amount.multiply(BigDecimal.valueOf(0.01));
            case WITHDRAWAL -> amount.multiply(BigDecimal.valueOf(0.02));
            case TRANSFER -> amount.multiply(BigDecimal.valueOf(0.015));
        };
    }
}


