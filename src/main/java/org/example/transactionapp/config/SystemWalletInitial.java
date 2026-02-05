package org.example.transactionapp.config;

import lombok.RequiredArgsConstructor;
import org.example.transactionapp.entity.Wallet;
import org.example.transactionapp.repository.WalletRepository;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.UUID;

@Configuration
@RequiredArgsConstructor
public class SystemWalletInitial {

    private final WalletRepository walletRepository;

    @Bean
    public ApplicationRunner initSystemWallet(){
        return args -> {
            UUID systemWalletId = UUID.fromString("00000000-0000-0000-0000-000000000001");

            if (!walletRepository.existsById(systemWalletId)){
                Wallet systemWallet = Wallet.builder()
                        .balance(BigDecimal.ZERO)
                        .build();
                walletRepository.save(systemWallet);
            }
        };
    }

}
