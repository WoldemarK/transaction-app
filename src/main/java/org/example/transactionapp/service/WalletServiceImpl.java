package org.example.transactionapp.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.transactionapp.dto.CreateWalletRequest;
import org.example.transactionapp.entity.Wallet;
import org.example.transactionapp.entity.WalletType;
import org.example.transactionapp.entity.WalletTypeStatus;
import org.example.transactionapp.exceptions.WalletNotFoundException;
import org.example.transactionapp.metrics.Loggable;
import org.example.transactionapp.metrics.MetricsFacade;
import org.example.transactionapp.repository.WalletRepository;
import org.example.transactionapp.repository.WalletTypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    private final MetricsFacade metrics;
    private final WalletRepository walletRepository;
    private final WalletTypeRepository walletTypeRepository;

    @Override
    @Loggable("wallet.create")
    public Wallet createWallet(CreateWalletRequest request) {

        WalletType walletType = walletTypeRepository.findByCurrencyCodeAndStatus(
                        request.getCurrency(), WalletTypeStatus.ACTIVE)
                .orElseThrow(() -> {
                    metrics.walletCreateError("wallet_type_not_found");
                    return new IllegalArgumentException("Active WalletType not found for currency: %s"
                                    .formatted(request.getCurrency())
                    );
                });

        if (walletRepository.existsByUserUidAndWalletType(request.getUserUid(), walletType)) {
            metrics.walletCreateError("already_exists");
            throw new IllegalArgumentException("Wallet already exists for currency %s".formatted(request.getCurrency())
            );
        }

        Wallet wallet = Wallet.builder()
                .userUid(request.getUserUid())
                .name("%s wallet".formatted(request.getCurrency()))
                .walletType(walletType)
                .status(WalletTypeStatus.ACTIVE)
                .balance(Optional.ofNullable(request.getInitialBalance())
                                .map(BigDecimal::valueOf)
                                .orElse(BigDecimal.ZERO)).build();

        Wallet saved = walletRepository.save(wallet);
        metrics.walletCreated(request.getCurrency());

        log.info("Создан кошелёк id={}, user={}", saved.getId(), saved.getUserUid());
        return saved;
    }

    @Override
    @Loggable("wallet.get")
    public Wallet getInformationByWalletId(UUID walletId) {
        return walletRepository.findById(walletId)
                .filter(wallet -> wallet.getStatus() == WalletTypeStatus.ACTIVE)
                .map(wallet -> {
                    metrics.walletFetched(true);
                    return wallet;
                })
                .orElseThrow(() -> {
                    metrics.walletFetched(false);
                    return new WalletNotFoundException("Активный кошелек не найден: " + walletId);
                });
    }
}
