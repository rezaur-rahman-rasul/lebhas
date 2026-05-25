package com.lebhas.creativesaas.billing.interfaces;

import jakarta.validation.constraints.PositiveOrZero;

public record UpdatePaymentProviderPriorityRequest(
        @PositiveOrZero
        int priority
) {
}
