package org.example.transactionapp.service;

import lombok.RequiredArgsConstructor;
import org.example.transactionapp.entity.Wallet;
import org.example.transactionapp.repository.WalletRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Предоставляет доступ к системному кошельку для учёта комиссий.
 * Системный кошелёк имеет фиксированный ID: 00000000-0000-0000-0000-000000000001
 */
@Service
@RequiredArgsConstructor
public class SystemWalletService {
    private final WalletRepository walletRepository;
    private static final UUID SYSTEM_WALLET_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Cacheable("systemWallet")
    public Wallet getSystemWallet() {
        return walletRepository.findById(SYSTEM_WALLET_ID)
                .orElseThrow(() -> new IllegalStateException(
                        "Системный кошелёк не найден. Проверьте инициализацию БД."));
    }
}
