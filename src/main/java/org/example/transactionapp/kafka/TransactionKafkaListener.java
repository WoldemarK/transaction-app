package org.example.transactionapp.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.transactionapp.dto.DepositCompletedEvent;
import org.example.transactionapp.dto.WithdrawalFailedEvent;
import org.example.transactionapp.entity.Transaction;
import org.example.transactionapp.entity.TransactionStatus;
import org.example.transactionapp.entity.Wallet;
import org.example.transactionapp.exceptions.TransactionIsNotPresent;
import org.example.transactionapp.exceptions.WalletNotFoundException;
import org.example.transactionapp.metrics.Loggable;
import org.example.transactionapp.repository.TransactionRepository;
import org.example.transactionapp.repository.WalletRepository;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionKafkaListener {

    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;


    @Transactional
    @Loggable("")
    @KafkaListener(topics = "deposit.completed", groupId = "transaction-service")
    public void onDepositCompleted(DepositCompletedEvent event) {
        Transaction transaction = transactionRepository.findById(event.getTransactionId())
                .orElseThrow(() -> new TransactionIsNotPresent("Транзакция не существует %s"
                        .formatted(event.getTransactionId())));
// проверка на статут
        Wallet wallet = walletRepository.findById(transaction.getWalletUid())
                .orElseThrow(() -> new WalletNotFoundException("Кошелек с ID  не найден%s"
                        .formatted(transaction.getWalletUid())));

        wallet.setBalance(wallet.getBalance().add(event.getAmount()));
        transaction.setStatus(TransactionStatus.COMPLETED);

        walletRepository.save(wallet);
        transactionRepository.save(transaction);
    }

    @KafkaListener(topics = "withdrawal.failed", groupId = "transaction-service")
    @Transactional
    public void onWithdrawalFailed(WithdrawalFailedEvent event) {
        Transaction transaction = transactionRepository.findById(event.getTransactionId())
                .orElseThrow(() -> new TransactionIsNotPresent("Транзакция не существует %s"
                        .formatted(event.getTransactionId())));

        transaction.setStatus(TransactionStatus.FAILED);
        transaction.setFailureReason(event.getFailureReason());
        transactionRepository.save(transaction);
    }
}
