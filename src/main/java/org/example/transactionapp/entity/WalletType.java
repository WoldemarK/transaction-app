package org.example.transactionapp.entity;

import jakarta.persistence.*;
import lombok.*;


@Getter
@Setter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "wallet_types")
public class WalletType extends AuditableEntity {

    @Column(name = "name", length = 32, nullable = false)
    private String name;

    @Column(name = "currency_code", length = 3, nullable = false)
    private String currencyCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 18, nullable = false)
    private WalletTypeStatus status;

    @Column(name = "user_type", length = 15)
    private String userType;

    @Column(name = "creator", length = 255)
    private String creator;

    @Column(name = "modifier", length = 255)
    private String modifier;
}
