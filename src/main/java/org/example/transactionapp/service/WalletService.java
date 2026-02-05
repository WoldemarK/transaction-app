package org.example.transactionapp.service;

import lombok.RequiredArgsConstructor;
import org.example.transactionapp.dto.CreateWalletRequest;
import org.example.transactionapp.entity.Wallet;
import org.example.transactionapp.entity.WalletType;
import org.example.transactionapp.entity.WalletTypeStatus;
import org.example.transactionapp.exceptions.WalletNotFoundException;
import org.example.transactionapp.repository.WalletRepository;
import org.example.transactionapp.repository.WalletTypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;
    private final WalletTypeRepository walletTypeRepository;

    public Wallet createWallet(CreateWalletRequest request) {
        WalletType walletType = walletTypeRepository.findByCurrencyCodeAndStatus
                (
                        request.getCurrency(),
                        WalletTypeStatus.ACTIVE
                ).orElseThrow(() ->
                new IllegalArgumentException("Active WalletType not found for currency: %s"
                        .formatted(request.getCurrency())));

        if (walletRepository.existsByUserUidAndWalletType(request.getUserUid(), walletType)) {
            throw new IllegalArgumentException("Wallet already exists for currency %s".formatted(request.getCurrency()));
        }
        Wallet wallet = Wallet.builder()
                .userUid(request.getUserUid())
                .name("%s wallet".formatted(request.getCurrency()))
                .walletType(walletType)
                .status(WalletTypeStatus.ACTIVE)
                .balance(
                        request.getInitialBalance() != null
                                ? BigDecimal.valueOf(request.getInitialBalance())
                                : BigDecimal.ZERO
                ).build();

        return walletRepository.save(wallet);
    }

    public Wallet getInformationByWalletId(UUID walletId) {
        Optional.ofNullable(walletId)
                .orElseThrow(() -> new IllegalArgumentException("Идентификатор кошелька не может быть пустым"));
        return walletRepository.findById(walletId)
                .filter(wallet -> wallet.getStatus() == WalletTypeStatus.ACTIVE)
                .orElseThrow(() -> new WalletNotFoundException(
                        String.format("Активный кошелек с идентификатором %s не найден", walletId)));
    }
}
