package org.example.transactionapp.service;

import groovy.util.logging.Slf4j;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.example.transactionapp.entity.PaymentType;
import org.example.transactionapp.entity.Transaction;
import org.example.transactionapp.entity.TransactionStatus;
import org.example.transactionapp.entity.Wallet;
import org.example.transactionapp.exceptions.InsufficientFundsException;
import org.example.transactionapp.exceptions.TransactionNotFoundException;
import org.example.transactionapp.repository.TransactionRepository;
import org.example.transactionapp.repository.WalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final MeterRegistry meterRegistry;
    private final WalletRepository walletRepository;
    private final SystemWalletService systemWalletProvider;
    private final TransactionRepository transactionRepository;

    @Transactional(readOnly = true)
    public Transaction getTransactionStatus(UUID transactionId) {
        return transactionRepository.findById(transactionId).orElseThrow(() -> new TransactionNotFoundException(
                "Транзакция не найдена: %s".formatted(transactionId)));
    }
    @Transactional
    public Transaction confirmTransaction(UUID transactionId) {
        return confirmTimer().record(() -> doConfirm(transactionId));
    }
    private Transaction doConfirm(UUID transactionId) {
        Transaction transaction = transactionRepository.findByIdWithLock(transactionId)
                .orElseThrow(() -> new TransactionNotFoundException("Транзакция не найдена: %s".formatted(transactionId)));

        validateTransactionForConfirmation(transaction);

        try {
            executeTransaction(transaction);

            transaction.setStatus(TransactionStatus.COMPLETED);
            confirmedCounter(transaction.getType()).increment();
            amountSummary(transaction.getType()).record(transaction.getAmount().doubleValue());

        } catch (Exception e) {
            transaction.setStatus(TransactionStatus.FAILED);
            failedCounter(transaction.getType()).increment();
            throw e;
        } finally {
            transaction.setUpdated(Instant.now());
        }

        return transactionRepository.save(transaction);
    }

    /* ===================== VALIDATION ===================== */

    private void validateTransactionForConfirmation(Transaction transaction) {
        if (transaction.getStatus() != TransactionStatus.PENDING) {
            throw new IllegalStateException("Транзакция уже обработана. Статус: %s".formatted(transaction.getStatus()));
        }

        if (transaction.getAmount() == null ||
                transaction.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Сумма должна быть положительной");
        }

        if (transaction.getType() == PaymentType.TRANSFER) {
            if (transaction.getTargetWallet() == null) {
                throw new IllegalArgumentException("Кошелёк получателя не указан");
            }
            if (transaction.getWallet().getId().equals(transaction.getTargetWallet().getId())) {
                throw new IllegalArgumentException("Нельзя переводить самому себе");
            }
        }
    }

    /* ===================== EXECUTION ===================== */

    private void executeTransaction(Transaction transaction) {
        switch (transaction.getType()) {
            case DEPOSIT -> executeDeposit(transaction);
            case WITHDRAWAL -> executeWithdrawal(transaction);
            case TRANSFER -> executeTransfer(transaction);
        }
    }

    private void executeDeposit(Transaction transaction) {
        Wallet wallet = walletRepository.findByIdForUpdate(transaction.getWallet().getId()).orElseThrow();

        BigDecimal fee = safeFee(transaction);

        wallet.setBalance(wallet.getBalance().add(transaction.getAmount()));
        walletRepository.save(wallet);

        transferFeeToSystemWallet(fee);
    }

    private void executeWithdrawal(Transaction transaction) {
        Wallet wallet = walletRepository.findByIdForUpdate(transaction.getWallet().getId()).orElseThrow();

        BigDecimal fee = safeFee(transaction);
        BigDecimal total = transaction.getAmount().add(fee);

        validateSufficientFunds(wallet, total);

        wallet.setBalance(wallet.getBalance().subtract(total));
        walletRepository.save(wallet);

        transferFeeToSystemWallet(fee);
    }

    private void executeTransfer(Transaction transaction) {
        Wallet from = walletRepository.findByIdForUpdate(transaction.getWallet().getId()).orElseThrow();

        Wallet to = walletRepository.findByIdForUpdate(transaction.getTargetWallet().getId()).orElseThrow();

        BigDecimal fee = safeFee(transaction);
        BigDecimal total = transaction.getAmount().add(fee);

        validateSufficientFunds(from, total);

        from.setBalance(from.getBalance().subtract(total));
        to.setBalance(to.getBalance().add(transaction.getAmount()));

        walletRepository.saveAll(List.of(from, to));

        transferFeeToSystemWallet(fee);
    }

    /* ===================== HELPERS ===================== */

    private void validateSufficientFunds(Wallet wallet, BigDecimal amount) {
        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException("Недостаточно средств. Требуется: %s, доступно: %s"
                    .formatted(amount, wallet.getBalance()));
        }
    }

    private void transferFeeToSystemWallet(BigDecimal fee) {
        if (fee.compareTo(BigDecimal.ZERO) <= 0) return;

        Wallet systemWallet = walletRepository.findByIdForUpdate(
                systemWalletProvider.getSystemWallet().getId()).orElseThrow();

        systemWallet.setBalance(systemWallet.getBalance().add(fee));
        walletRepository.save(systemWallet);
    }

    private BigDecimal safeFee(Transaction transaction) {
        return Optional.ofNullable(transaction.getFee()).orElse(BigDecimal.ZERO);
    }

    /* ===================== METRICS ===================== */

    private Counter confirmedCounter(PaymentType type) {
        return Counter.builder("transactions.confirmed.total").tag("type", type.name()).register(meterRegistry);
    }

    private Counter failedCounter(PaymentType type) {
        return Counter.builder("transactions.failed.total").tag("type", type.name()).register(meterRegistry);
    }

    private Timer confirmTimer() {
        return Timer.builder("transactions.confirm.duration")
                .publishPercentileHistogram()
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry);
    }

    private DistributionSummary amountSummary(PaymentType type) {
        return DistributionSummary.builder("transactions.amount")
                .baseUnit("currency")
                .tag("type", type.name())
                .register(meterRegistry);
    }
}
