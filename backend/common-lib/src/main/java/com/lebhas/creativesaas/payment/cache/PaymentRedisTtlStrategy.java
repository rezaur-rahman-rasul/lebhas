package com.lebhas.creativesaas.payment.cache;

import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class PaymentRedisTtlStrategy {

    public Duration activeProviderTtl() {
        return Duration.ofMinutes(5);
    }

    public Duration providerTtl() {
        return Duration.ofMinutes(30);
    }

    public Duration configurationTtl() {
        return Duration.ofMinutes(15);
    }

    public Duration sessionTtl() {
        return Duration.ofMinutes(30);
    }

    public Duration transactionTtl() {
        return Duration.ofHours(6);
    }

    public Duration webhookIdempotencyTtl() {
        return Duration.ofDays(3);
    }

    public Duration creditPackageTtl() {
        return Duration.ofMinutes(30);
    }

    public Duration activeCreditPackagesTtl() {
        return Duration.ofMinutes(10);
    }

    public Duration paymentLockTtl() {
        return Duration.ofMinutes(3);
    }

    public Duration webhookLockTtl() {
        return Duration.ofMinutes(5);
    }
}
