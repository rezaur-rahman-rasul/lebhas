package com.lebhas.creativesaas.billing;

import com.lebhas.creativesaas.pricing.cache.dto.ActivePricingPlansCacheEntry;
import com.lebhas.creativesaas.pricing.cache.dto.PlanFeaturePolicyCacheEntry;
import com.lebhas.creativesaas.pricing.cache.dto.PricingPlanCacheEntry;
import com.lebhas.creativesaas.pricing.cache.dto.WorkspaceSubscriptionCacheEntry;
import com.lebhas.pricing.PlanFeaturePolicy;
import com.lebhas.pricing.PricingPlan;
import com.lebhas.pricing.WorkspaceSubscription;
import com.lebhas.pricing.WorkspaceSubscriptionStatus;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PricingApiMockMvcIntegrationTest extends AbstractBillingPricingIntegrationTest {

    @Test
    void masterCanCreatePricingPlan() throws Exception {
        mockMvc.perform(post("/api/v1/master/pricing-plans")
                        .header(HttpHeaders.AUTHORIZATION, bearer(masterToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Launch Plan",
                                  "code": "LAUNCH_PLAN",
                                  "description": "Plan created through API test",
                                  "monthlyPrice": 29.5,
                                  "yearlyPrice": 295.0,
                                  "currency": "USD",
                                  "defaultPlan": false,
                                  "active": true,
                                  "sortOrder": 60
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.pricingPlan.code").value("LAUNCH_PLAN"))
                .andExpect(jsonPath("$.data.pricingPlan.active").value(true));
    }

    @Test
    void pricingPlanCurrencyIsDynamicAndNormalized() throws Exception {
        mockMvc.perform(post("/api/v1/master/pricing-plans")
                        .header(HttpHeaders.AUTHORIZATION, bearer(masterToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Dynamic Currency Plan",
                                  "code": "DYNAMIC_CURRENCY_PLAN",
                                  "description": "Currency normalization test",
                                  "monthlyPrice": 17.0,
                                  "yearlyPrice": 170.0,
                                  "currency": "usd",
                                  "defaultPlan": false,
                                  "active": true,
                                  "sortOrder": 63
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.pricingPlan.code").value("DYNAMIC_CURRENCY_PLAN"))
                .andExpect(jsonPath("$.data.pricingPlan.currency").value("USD"));
    }

    @Test
    void masterCanUpdatePricingPlan() throws Exception {
        PricingPlan plan = createCustomPlan("update-plan", true);

        mockMvc.perform(put("/api/v1/master/pricing-plans/{pricingPlanId}", plan.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(masterToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Updated Plan",
                                  "code": "UPDATED_PLAN",
                                  "description": "Updated through API",
                                  "monthlyPrice": 39.0,
                                  "yearlyPrice": 390.0,
                                  "currency": "USD",
                                  "defaultPlan": false,
                                  "active": true,
                                  "sortOrder": 61
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.pricingPlan.name").value("Updated Plan"))
                .andExpect(jsonPath("$.data.pricingPlan.code").value("UPDATED_PLAN"));

        PricingPlan updated = pricingPlanRepository.findByIdAndDeletedFalse(plan.getId()).orElseThrow();
        assertThat(updated.getCode()).isEqualTo("UPDATED_PLAN");
    }

    @Test
    void masterCanDisablePricingPlan() throws Exception {
        PricingPlan plan = createCustomPlan("disable-plan", true);

        mockMvc.perform(delete("/api/v1/master/pricing-plans/{pricingPlanId}", plan.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(masterToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.pricingPlan.active").value(false));

        assertThat(pricingPlanRepository.findByIdAndDeletedFalse(plan.getId()).orElseThrow().isActive()).isFalse();
    }

    @Test
    void nonMasterCannotManagePricingPlan() throws Exception {
        mockMvc.perform(post("/api/v1/master/pricing-plans")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Forbidden Plan",
                                  "code": "FORBIDDEN_PLAN",
                                  "description": "Should not be created",
                                  "monthlyPrice": 10.0,
                                  "yearlyPrice": 100.0,
                                  "currency": "USD",
                                  "defaultPlan": false,
                                  "active": true,
                                  "sortOrder": 40
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errors[0].code").value("COMMON-403"));
    }

    @Test
    void masterCanUpdatePlanFeaturePolicy() throws Exception {
        PricingPlan plan = createCustomPlan("feature-policy", true);

        mockMvc.perform(put("/api/v1/master/pricing-plans/{pricingPlanId}/feature-policy", plan.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(masterToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "maxGeneratedVersionsPerRequest": 12,
                                  "maxBrands": 4,
                                  "maxProductServices": 9,
                                  "maxProjects": 7,
                                  "maxTeamMembers": 11,
                                  "maxStorageGb": 55.5,
                                  "monthlyCreditLimit": 500.0,
                                  "allowApprovalWorkflow": true,
                                  "allowPublicShareLinks": true,
                                  "allowVideoGeneration": false,
                                  "allowAdvancedPromptIntelligence": true,
                                  "allowTeamCollaboration": true,
                                  "allowExportWithoutWatermark": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.maxGeneratedVersionsPerRequest").value(12))
                .andExpect(jsonPath("$.data.allowPublicShareLinks").value(true));
    }

    @Test
    void masterCanAssignPlanToWorkspace() throws Exception {
        PricingPlan plan = createCustomPlan("assign-plan", true);

        mockMvc.perform(post("/api/v1/master/workspaces/{workspaceId}/subscription", workspaceOne.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(masterToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "pricingPlanId": "%s",
                                  "status": "ACTIVE",
                                  "startedAt": "2026-05-18T00:00:00Z",
                                  "expiresAt": "2026-06-18T00:00:00Z",
                                  "trialEndsAt": null,
                                  "autoRenew": true
                                }
                                """.formatted(plan.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.activePricingPlan.id").value(plan.getId().toString()))
                .andExpect(jsonPath("$.data.activeSubscription.workspaceId").value(workspaceOne.getId().toString()))
                .andExpect(jsonPath("$.data.activeSubscription.status").value("ACTIVE"));
    }

    @Test
    void publicPricingApiReturnsActivePlansOnly() throws Exception {
        PricingPlan activePlan = createCustomPlan("public-active", true);
        PricingPlan inactivePlan = createCustomPlan("public-inactive", false);

        MvcResult result = mockMvc.perform(get("/api/v1/pricing-plans/public"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        List<String> codes = json(result).at("/data").findValuesAsText("code");
        assertThat(codes).contains(activePlan.getCode());
        assertThat(codes).doesNotContain(inactivePlan.getCode());
    }

    @Test
    void activePricingApiReturnsActivePlansOnlyFromBaseRoute() throws Exception {
        PricingPlan activePlan = createCustomPlan("base-public-active", true);
        PricingPlan inactivePlan = createCustomPlan("base-public-inactive", false);

        MvcResult result = mockMvc.perform(get("/api/v1/pricing-plans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        List<String> codes = json(result).at("/data").findValuesAsText("code");
        assertThat(codes).contains(activePlan.getCode());
        assertThat(codes).doesNotContain(inactivePlan.getCode());
    }

    @Test
    void pricingCacheInvalidatesOnUpdate() throws Exception {
        PricingPlan plan = createCustomPlan("cache-update", true);
        pricingPlanCacheService.store(PricingPlanCacheEntry.from(plan));
        pricingPlanCacheService.storeActivePlans(new ActivePricingPlansCacheEntry(
                List.of(PricingPlanCacheEntry.from(plan)),
                Instant.now()));

        mockMvc.perform(put("/api/v1/master/pricing-plans/{pricingPlanId}", plan.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(masterToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Cache Updated Plan",
                                  "code": "CACHE_UPDATED_PLAN",
                                  "description": "Invalidate cache",
                                  "monthlyPrice": 44.0,
                                  "yearlyPrice": 440.0,
                                  "currency": "USD",
                                  "defaultPlan": false,
                                  "active": true,
                                  "sortOrder": 71
                                }
                                """))
                .andExpect(status().isOk());

        PricingPlanCacheEntry cachedPlan = pricingPlanCacheService.get(plan.getId()).orElseThrow();
        assertThat(cachedPlan.name()).isEqualTo("Cache Updated Plan");
        assertThat(cachedPlan.code()).isEqualTo("CACHE_UPDATED_PLAN");
        assertThat(pricingPlanCacheService.getActivePlans()).isEmpty();
    }

    @Test
    void subscriptionCacheInvalidatesOnWorkspaceSubscriptionChange() throws Exception {
        PricingPlan firstPlan = createCustomPlan("subscription-first", true);
        PricingPlan secondPlan = createCustomPlan("subscription-second", true);
        WorkspaceSubscription subscription = createSubscription(
                workspaceOne.getId(),
                firstPlan.getId(),
                WorkspaceSubscriptionStatus.ACTIVE);
        workspaceSubscriptionCacheService.store(WorkspaceSubscriptionCacheEntry.from(subscription));

        mockMvc.perform(post("/api/v1/master/workspaces/{workspaceId}/subscription", workspaceOne.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(masterToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "pricingPlanId": "%s",
                                  "status": "ACTIVE",
                                  "startedAt": "2026-05-18T00:00:00Z",
                                  "expiresAt": "2026-07-18T00:00:00Z",
                                  "trialEndsAt": null,
                                  "autoRenew": false
                                }
                                """.formatted(secondPlan.getId())))
                .andExpect(status().isOk());

        WorkspaceSubscriptionCacheEntry cached = workspaceSubscriptionCacheService.get(workspaceOne.getId()).orElseThrow();
        assertThat(cached.pricingPlanId()).isEqualTo(secondPlan.getId());
        assertThat(cached.autoRenew()).isFalse();
    }

    @Test
    void featurePolicyUpdateInvalidatesAffectedWorkspaceSubscriptionCache() throws Exception {
        PricingPlan plan = createCustomPlan("policy-invalidation", true);
        PlanFeaturePolicy policy = createFeaturePolicy(plan.getId(), 5, java.math.BigDecimal.TEN, true);
        WorkspaceSubscription subscription = createSubscription(
                workspaceOne.getId(),
                plan.getId(),
                WorkspaceSubscriptionStatus.ACTIVE);
        planFeaturePolicyCacheService.store(PlanFeaturePolicyCacheEntry.from(policy));
        workspaceSubscriptionCacheService.store(WorkspaceSubscriptionCacheEntry.from(subscription));

        mockMvc.perform(put("/api/v1/master/pricing-plans/{pricingPlanId}/feature-policy", plan.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(masterToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "maxGeneratedVersionsPerRequest": 15,
                                  "maxBrands": 5,
                                  "maxProductServices": 12,
                                  "maxProjects": 9,
                                  "maxTeamMembers": 8,
                                  "maxStorageGb": 75.0,
                                  "monthlyCreditLimit": 700.0,
                                  "allowApprovalWorkflow": false,
                                  "allowPublicShareLinks": true,
                                  "allowVideoGeneration": false,
                                  "allowAdvancedPromptIntelligence": true,
                                  "allowTeamCollaboration": true,
                                  "allowExportWithoutWatermark": false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.maxGeneratedVersionsPerRequest").value(15))
                .andExpect(jsonPath("$.data.allowApprovalWorkflow").value(false));

        assertThat(planFeaturePolicyCacheService.get(plan.getId())).isEmpty();
        assertThat(workspaceSubscriptionCacheService.get(workspaceOne.getId())).isEmpty();
    }

    @Test
    void standardApiResponseFormatWorks() throws Exception {
        mockMvc.perform(post("/api/v1/master/pricing-plans")
                        .header(HttpHeaders.AUTHORIZATION, bearer(masterToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Envelope Plan",
                                  "code": "ENVELOPE_PLAN",
                                  "description": "Envelope test",
                                  "monthlyPrice": 30.0,
                                  "yearlyPrice": 300.0,
                                  "currency": "USD",
                                  "defaultPlan": false,
                                  "active": true,
                                  "sortOrder": 62
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.data.pricingPlan.code").value("ENVELOPE_PLAN"))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors").isEmpty())
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }
}
