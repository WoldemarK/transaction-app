package org.example.transactionapp.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transactions extends AuditableEntity {

    @Column(name = "user_uid", nullable = false)
    private UUID userUid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_uid", nullable = false)
    private Wallet wallet;

    @Column(name = "amount", precision = 19, scale = 4, nullable = false)
    private BigDecimal amount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private PaymentType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 32, nullable = false)
    private TransactionStatus status;

    @Column(name = "comment", length = 256)
    private String comment;

    @Column(name = "fee", precision = 19, scale = 4)
    private BigDecimal fee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_wallet_uid")
    private Wallet targetWallet;

    @Column(name = "payment_method_id")
    private Long paymentMethodId;

    @Column(name = "failure_reason", length = 256)
    private String failureReason;
}
