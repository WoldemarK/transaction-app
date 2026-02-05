package org.example.transactionapp.rest;

import lombok.RequiredArgsConstructor;
import org.example.transactionapp.dto.CreateWalletRequest;
import org.example.transactionapp.entity.Wallet;
import org.example.transactionapp.service.WalletService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class WalletRestController {

    private final WalletService walletService;

    @PostMapping
    public ResponseEntity<Wallet> createWallet(CreateWalletRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(walletService.createWallet(request));
    }

    @GetMapping("/{uuid}")
    public ResponseEntity<Wallet> getInformationByWalletId(@PathVariable UUID uuid) {
        return ResponseEntity.ok().body(walletService.getInformationByWalletId(uuid));
    }
}
