package com.lebhas.creativesaas.billing;

import com.lebhas.creativesaas.pricing.cache.dto.PlanFeaturePolicyCacheEntry;
import com.lebhas.creativesaas.pricing.cache.dto.WorkspaceSubscriptionCacheEntry;
import com.lebhas.pricing.PlanFeaturePolicy;
import com.lebhas.pricing.PricingPlan;
import com.lebhas.pricing.WorkspaceSubscription;
import com.lebhas.pricing.WorkspaceSubscriptionStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class PricingFoundationIntegrationTest extends AbstractBillingPricingIntegrationTest {

    @Test
    void pricingPlanEntityPersistsCorrectly() {
        PricingPlan saved = createCustomPlan("persist-plan", true);

        PricingPlan reloaded = pricingPlanRepository.findByIdAndDeletedFalse(saved.getId()).orElseThrow();

        assertThat(reloaded.getCode()).startsWith("PERSIST_PLAN");
        assertThat(reloaded.getCurrency()).isEqualTo("USD");
        assertThat(reloaded.isActive()).isTrue();
        assertThat(reloaded.getCreatedAt()).isNotNull();
        assertThat(reloaded.getUpdatedAt()).isNotNull();
    }

    @Test
    void planFeaturePolicyPersistsCorrectly() {
        PricingPlan plan = createCustomPlan("policy-plan", true);
        PlanFeaturePolicy saved = createFeaturePolicy(plan.getId(), 14, new BigDecimal("15.5000"), true);

        PlanFeaturePolicy reloaded = planFeaturePolicyRepository.findByPricingPlanIdAndDeletedFalse(plan.getId()).orElseThrow();

        assertThat(reloaded.getId()).isEqualTo(saved.getId());
        assertThat(reloaded.getPricingPlanId()).isEqualTo(plan.getId());
        assertThat(reloaded.getMaxStorageGb()).isEqualByComparingTo("15.5000");
        assertThat(reloaded.isAllowPublicShareLinks()).isTrue();
    }

    @Test
    void workspaceSubscriptionPersistsCorrectly() {
        PricingPlan plan = createCustomPlan("subscription-plan", true);
        WorkspaceSubscription saved = createSubscription(workspaceOne.getId(), plan.getId(), WorkspaceSubscriptionStatus.ACTIVE);

        WorkspaceSubscription reloaded = workspaceSubscriptionRepository.findFirstByWorkspaceIdAndDeletedFalse(workspaceOne.getId()).orElseThrow();

        assertThat(reloaded.getId()).isEqualTo(saved.getId());
        assertThat(reloaded.getWorkspaceId()).isEqualTo(workspaceOne.getId());
        assertThat(reloaded.getPricingPlanId()).isEqualTo(plan.getId());
        assertThat(reloaded.getStatus()).isEqualTo(WorkspaceSubscriptionStatus.ACTIVE);
    }

    @Test
    void workspaceCanHaveActiveSubscription() {
        PricingPlan plan = createCustomPlan("active-subscription", true);
        createSubscription(workspaceOne.getId(), plan.getId(), WorkspaceSubscriptionStatus.ACTIVE);

        WorkspaceSubscription active = workspaceSubscriptionRepository.findByWorkspaceIdAndStatusAndDeletedFalse(
                workspaceOne.getId(),
                WorkspaceSubscriptionStatus.ACTIVE).orElseThrow();

        assertThat(active.getPricingPlanId()).isEqualTo(plan.getId());
    }

    @Test
    void pricingPlanCanBeActiveOrInactive() {
        PricingPlan activePlan = createCustomPlan("active-plan", true);
        PricingPlan inactivePlan = createCustomPlan("inactive-plan", true);
        inactivePlan.deactivate();
        pricingPlanRepository.save(inactivePlan);

        assertThat(pricingPlanRepository.findAllByActiveTrueAndDeletedFalseOrderBySortOrderAscNameAsc())
                .extracting(PricingPlan::getId)
                .contains(activePlan.getId())
                .doesNotContain(inactivePlan.getId());
    }

    @Test
    void defaultPlansAreSeedDataOnly() {
        PricingPlan freePlan = seedPlan("FREE");
        String originalName = freePlan.getName();
        String originalDescription = freePlan.getDescription();
        BigDecimal originalMonthly = freePlan.getMonthlyPrice();
        BigDecimal originalYearly = freePlan.getYearlyPrice();
        String originalCurrency = freePlan.getCurrency();
        boolean originalDefault = freePlan.isDefault();
        boolean originalActive = freePlan.isActive();
        int originalSortOrder = freePlan.getSortOrder();

        freePlan.update(
                "Free Editable Seed",
                freePlan.getCode(),
                "Updated seed plan description",
                originalMonthly,
                originalYearly,
                originalCurrency,
                originalDefault,
                originalActive,
                originalSortOrder);
        pricingPlanRepository.save(freePlan);

        PricingPlan updated = seedPlan("FREE");
        assertThat(updated.getName()).isEqualTo("Free Editable Seed");
        assertThat(updated.getDescription()).isEqualTo("Updated seed plan description");

        updated.update(
                originalName,
                updated.getCode(),
                originalDescription,
                originalMonthly,
                originalYearly,
                originalCurrency,
                originalDefault,
                originalActive,
                originalSortOrder);
        pricingPlanRepository.save(updated);
    }

    @Test
    void featurePolicyStoresGeneratedVersionLimit() {
        PricingPlan plan = createCustomPlan("generated-limit", true);
        createFeaturePolicy(plan.getId(), 21, new BigDecimal("22.0000"), false);

        PlanFeaturePolicy reloaded = planFeaturePolicyRepository.findByPricingPlanIdAndDeletedFalse(plan.getId()).orElseThrow();

        assertThat(reloaded.getMaxGeneratedVersionsPerRequest()).isEqualTo(21);
    }

    @Test
    void redisSubscriptionCacheStoresAndRetrievesWorkspaceSubscription() {
        PricingPlan plan = createCustomPlan("subscription-cache", true);
        WorkspaceSubscription subscription = createSubscription(workspaceOne.getId(), plan.getId(), WorkspaceSubscriptionStatus.TRIAL);
        WorkspaceSubscriptionCacheEntry cacheEntry = WorkspaceSubscriptionCacheEntry.from(subscription);

        workspaceSubscriptionCacheService.store(cacheEntry);

        WorkspaceSubscriptionCacheEntry cached = workspaceSubscriptionCacheService.get(workspaceOne.getId()).orElseThrow();
        assertThat(cached.workspaceId()).isEqualTo(workspaceOne.getId());
        assertThat(cached.pricingPlanId()).isEqualTo(plan.getId());
        assertThat(cached.status()).isEqualTo(WorkspaceSubscriptionStatus.TRIAL);
    }

    @Test
    void redisPlanFeatureCacheWorks() {
        PricingPlan plan = createCustomPlan("feature-cache", true);
        PlanFeaturePolicy policy = createFeaturePolicy(plan.getId(), 8, new BigDecimal("9.0000"), true);
        PlanFeaturePolicyCacheEntry cacheEntry = PlanFeaturePolicyCacheEntry.from(policy);

        planFeaturePolicyCacheService.store(cacheEntry);

        PlanFeaturePolicyCacheEntry cached = planFeaturePolicyCacheService.get(plan.getId()).orElseThrow();
        assertThat(cached.pricingPlanId()).isEqualTo(plan.getId());
        assertThat(cached.maxGeneratedVersionsPerRequest()).isEqualTo(8);
        assertThat(cached.allowPublicShareLinks()).isTrue();
    }

    @Test
    void postgresqlRemainsSourceOfTruth() {
        PricingPlan planA = createCustomPlan("source-a", true);
        PricingPlan planB = createCustomPlan("source-b", true);
        WorkspaceSubscription subscription = createSubscription(workspaceOne.getId(), planA.getId(), WorkspaceSubscriptionStatus.ACTIVE);
        workspaceSubscriptionCacheService.store(WorkspaceSubscriptionCacheEntry.from(subscription));

        subscription.changePlan(planB.getId());
        workspaceSubscriptionRepository.save(subscription);

        WorkspaceSubscription authoritative = workspaceSubscriptionRepository.findFirstByWorkspaceIdAndDeletedFalse(workspaceOne.getId()).orElseThrow();
        WorkspaceSubscriptionCacheEntry staleCache = workspaceSubscriptionCacheService.get(workspaceOne.getId()).orElseThrow();

        assertThat(authoritative.getPricingPlanId()).isEqualTo(planB.getId());
        assertThat(staleCache.pricingPlanId()).isEqualTo(planA.getId());
    }
}
