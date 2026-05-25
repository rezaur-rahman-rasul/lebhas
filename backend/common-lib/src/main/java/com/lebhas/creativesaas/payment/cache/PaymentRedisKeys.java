package com.lebhas.creativesaas.payment.cache;

import com.lebhas.creativesaas.payment.domain.PaymentEnvironmentType;

import java.util.UUID;

public final class PaymentRedisKeys {

    private static final String ACTIVE_PAYMENT_PROVIDER = "payment:provider:active";
    private static final String PAYMENT_PROVIDER = "payment:provider:%s";
    private static final String PAYMENT_CONFIGURATION = "payment:config:%s:%s";
    private static final String PAYMENT_SESSION = "payment:session:%s";
    private static final String PAYMENT_TRANSACTION = "payment:transaction:%s";
    private static final String PAYMENT_WEBHOOK = "payment:webhook:%s";
    private static final String PAYMENT_LOCK = "payment:lock:%s";
    private static final String PAYMENT_WEBHOOK_LOCK = "payment:webhook:lock:%s";
    private static final String CREDIT_PACKAGE = "credit:package:%s";
    private static final String ACTIVE_CREDIT_PACKAGES = "credit:packages:active";

    private PaymentRedisKeys() {
    }

    public static String activePaymentProvider() {
        return ACTIVE_PAYMENT_PROVIDER;
    }

    public static String paymentProvider(UUID providerId) {
        return PAYMENT_PROVIDER.formatted(require(providerId, "providerId"));
    }

    public static String paymentConfiguration(UUID providerId, PaymentEnvironmentType environmentType) {
        return PAYMENT_CONFIGURATION.formatted(
                require(providerId, "providerId"),
                require(environmentType, "environmentType").name());
    }

    public static String paymentSession(UUID paymentTransactionId) {
        return PAYMENT_SESSION.formatted(require(paymentTransactionId, "paymentTransactionId"));
    }

    public static String paymentTransaction(UUID paymentTransactionId) {
        return PAYMENT_TRANSACTION.formatted(require(paymentTransactionId, "paymentTransactionId"));
    }

    public static String paymentWebhook(String providerTransactionId) {
        return PAYMENT_WEBHOOK.formatted(normalize(providerTransactionId, "providerTransactionId"));
    }

    public static String paymentLock(UUID paymentTransactionId) {
        return PAYMENT_LOCK.formatted(require(paymentTransactionId, "paymentTransactionId"));
    }

    public static String paymentWebhookLock(String providerTransactionId) {
        return PAYMENT_WEBHOOK_LOCK.formatted(normalize(providerTransactionId, "providerTransactionId"));
    }

    public static String creditPackage(UUID creditPackageId) {
        return CREDIT_PACKAGE.formatted(require(creditPackageId, "creditPackageId"));
    }

    public static String activeCreditPackages() {
        return ACTIVE_CREDIT_PACKAGES;
    }

    private static <T> T require(T value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " must not be null");
        }
        return value;
    }

    private static String normalize(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }
}
