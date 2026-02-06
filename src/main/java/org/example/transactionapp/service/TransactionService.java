package org.example.transactionapp.service;


import org.example.transactionapp.dto.*;

import java.util.UUID;

public interface TransactionService {

    TransactionInitResponse init(String type, HasAmount request);

    TransactionConfirmResponse confirm(String type, TransactionConfirmRequest request);

    TransactionStatusResponse status(UUID transactionId);
}
