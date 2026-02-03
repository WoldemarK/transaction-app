package org.example.transactionapp.repository;

import org.example.transactionapp.entity.WalletType;
import org.example.transactionapp.entity.WalletTypeStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface WalletTypeRepository extends JpaRepository<WalletType, UUID> {
    Optional<WalletType> findByCurrencyCodeAndStatus(String currencyCode, WalletTypeStatus status);
}
