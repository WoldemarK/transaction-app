package org.example.transactionapp.exceptions;

public class TransactionIsNotPresent extends RuntimeException{
    public TransactionIsNotPresent(String message) {
        super(message);
    }
}
