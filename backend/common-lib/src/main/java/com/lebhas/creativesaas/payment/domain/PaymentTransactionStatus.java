package com.lebhas.creativesaas.payment.domain;

public enum PaymentTransactionStatus {
    INITIATED,
    PENDING,
    SUCCESS,
    FAILED,
    CANCELLED,
    EXPIRED,
    REFUNDED
}
