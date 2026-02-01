package org.example.transactionapp.repository;

import org.example.transactionapp.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, UUID> {

    @Query("SELECT w FROM Wallet w WHERE w.userUid = :userUid AND w.status = :status")
    List<Wallet> findByUserUidAndStatus
            (
                    @Param("userUid") UUID userUid,
                    @Param("status") String status
            );

}
