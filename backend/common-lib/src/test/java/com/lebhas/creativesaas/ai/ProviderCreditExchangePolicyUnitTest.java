package com.lebhas.creativesaas.ai;

import com.lebhas.ai.credit.domain.ProviderCreditExchangePolicy;
import com.lebhas.ai.credit.domain.ProviderCreditPool;
import com.lebhas.creativesaas.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProviderCreditExchangePolicyUnitTest {

    private static final UUID PROVIDER_ID = UUID.fromString("00000000-0000-0000-0000-000000000101");

    @Test
    void freeSignupCreditUsesConfiguredPercentage() {
        ProviderCreditExchangePolicy policy = policy("2.0000", "2000.0000", "0.0000", "0.0000", true);

        BigDecimal freeCredit = policy.calculateFreeSignupCredits(new BigDecimal("100000.0000"));

        assertThat(freeCredit).isEqualByComparingTo("2000.0000");
    }

    @Test
    void freeSignupCreditRespectsMaxCap() {
        ProviderCreditExchangePolicy policy = policy("10.0000", "1500.0000", "0.0000", "0.0000", true);

        BigDecimal freeCredit = policy.calculateFreeSignupCredits(new BigDecimal("100000.0000"));

        assertThat(freeCredit).isEqualByComparingTo("1500.0000");
    }

    @Test
    void freeSignupCreditUsesFallbackWhenProviderBalanceTooLow() {
        ProviderCreditExchangePolicy policy = policy("2.0000", "2000.0000", "5000.0000", "100.0000", true);

        BigDecimal freeCredit = policy.calculateFreeSignupCredits(new BigDecimal("4999.0000"));

        assertThat(freeCredit).isEqualByComparingTo("100.0000");
    }

    @Test
    void providerPoolAllocationPreventsNegativeAvailableBalance() {
        ProviderCreditPool pool = ProviderCreditPool.create(
                PROVIDER_ID,
                "USD",
                new BigDecimal("10.0000"),
                new BigDecimal("1000.0000"),
                BigDecimal.ZERO);

        pool.allocateFreeCredit(new BigDecimal("900.0000"));

        assertThat(pool.availableInternalCredits()).isEqualByComparingTo("100.0000");
        assertThatThrownBy(() -> pool.allocateFreeCredit(new BigDecimal("101.0000")))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void providerPoolAdjustmentCannotDropBelowUsedCredits() {
        ProviderCreditPool pool = ProviderCreditPool.create(
                PROVIDER_ID,
                "USD",
                new BigDecimal("10.0000"),
                new BigDecimal("1000.0000"),
                BigDecimal.ZERO);
        pool.allocateFreeCredit(new BigDecimal("500.0000"));

        assertThatThrownBy(() -> pool.adjustInternalEquivalent(new BigDecimal("-501.0000")))
                .isInstanceOf(BusinessException.class);
    }

    private ProviderCreditExchangePolicy policy(
            String percentage,
            String max,
            String minRequired,
            String fallback,
            boolean enabled
    ) {
        return ProviderCreditExchangePolicy.create(
                PROVIDER_ID,
                BigDecimal.ONE,
                new BigDecimal(percentage),
                enabled,
                new BigDecimal(max),
                new BigDecimal(minRequired),
                new BigDecimal(fallback),
                true);
    }
}
