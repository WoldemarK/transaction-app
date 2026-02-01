package org.example.transactionapp.service;

import lombok.RequiredArgsConstructor;
import org.example.transactionapp.entity.Wallet;
import org.example.transactionapp.repository.WalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;

    public Wallet createWallet(UUID userId, Wallet request) {
        Wallet wallet = Wallet.builder()
                .userUid(userId)
                .walletType(request.getWalletType())
                .build();
        return walletRepository.save(wallet);
    }
}
