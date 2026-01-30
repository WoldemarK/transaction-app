package org.example.transactionapp.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "wallets")
public class Wallet extends AuditableEntity{
    @Column(name = "name", length = 32, nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_type_uid", nullable = false)
    private WalletType walletType;

    @Column(name = "user_uid", nullable = false)
    private UUID userUid; // или связь @ManyToOne с User, если есть сущность

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    private WalletTypeStatus status;

    @Column(name = "balance", precision = 19, scale = 4, nullable = false)
    private BigDecimal balance = BigDecimal.ZERO;

    // Связь один-ко-многим с транзакциями
    @OneToMany(mappedBy = "wallet", fetch = FetchType.LAZY)
    private List<Transaction> transactions = new ArrayList<>();

    // Для транзакций типа TRANSFER (кошельки-получатели)
    @OneToMany(mappedBy = "targetWallet", fetch = FetchType.LAZY)
    private List<Transaction> incomingTransactions = new ArrayList<>();
}
