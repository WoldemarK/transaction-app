package org.example.transactionapp.entity;

public enum TransactionStatus {
    PENDING,      // ожидает подтверждения
    PROCESSING,   // в процессе выполнения
    COMPLETED,    // успешно завершена
    FAILED,       // неудачно завершена
    CANCELLED     // отменена
}
