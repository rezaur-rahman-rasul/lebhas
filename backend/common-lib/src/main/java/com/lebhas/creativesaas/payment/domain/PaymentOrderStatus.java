package com.lebhas.creativesaas.payment.domain;

public enum PaymentOrderStatus {
    CREATED,
    PAYMENT_PENDING,
    PAID,
    FAILED,
    CANCELLED,
    EXPIRED
}
