package com.lebhas.creativesaas.creative;

import com.lebhas.creativesaas.common.security.Role;
import com.lebhas.creativesaas.pricing.cache.PricingRedisKeys;
import com.lebhas.creativesaas.redis.RedisKeyBuilder;
import com.lebhas.creativesaas.redis.RedisLockService;
import com.lebhas.creativesaas.storage.cache.StorageUsageRedisCacheService;
import com.lebhas.creativesaas.storage.cache.StorageUsageRedisKeys;
import com.lebhas.creativesaas.storage.cache.dto.StorageUsageCacheEntry;
import com.lebhas.creativesaas.storage.domain.StorageUsageEntity;
import com.lebhas.creativesaas.storage.event.StoragePlanKafkaTopicNames;
import com.lebhas.pricing.PlanFeaturePolicy;
import com.lebhas.pricing.PlanFeaturePolicyRepository;
import com.lebhas.pricing.PricingPlan;
import com.lebhas.pricing.PricingPlanRepository;
import com.lebhas.pricing.WorkspaceSubscription;
import com.lebhas.pricing.WorkspaceSubscriptionRepository;
import com.lebhas.pricing.WorkspaceSubscriptionStatus;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class Day123DynamicPricingStorageIntegrationTest extends AbstractDay4BackendIntegrationTest {

    private static final BigDecimal SMALL_STORAGE_LIMIT_GB = new BigDecimal("0.0001");

    @Autowired
    private PricingPlanRepository pricingPlanRepository;

    @Autowired
    private PlanFeaturePolicyRepository planFeaturePolicyRepository;

    @Autowired
    private WorkspaceSubscriptionRepository workspaceSubscriptionRepository;

    @Autowired
    private StorageUsageRedisCacheService storageUsageRedisCacheService;

    @Autowired
    private RedisLockService redisLockService;

    @Autowired
    private RedisKeyBuilder redisKeyBuilder;

    @Autowired
    private StoragePlanKafkaTopicNames storagePlanKafkaTopicNames;

    @Test
    void assetUploadAllowedWhenStorageLimitAvailable() throws Exception {
        configureWorkspacePlan(SMALL_STORAGE_LIMIT_GB, true);

        MvcResult upload = uploadAsset(
                tokenFor(adminUser, workspaceOne.getId(), Role.ADMIN),
                "quota-allowed.png",
                pngBytes(),
                "image/png",
                "PRODUCT_IMAGE");

        assertThat(upload.getResponse().getStatus()).isEqualTo(200);
        assertThat(textAt(upload, "/data/status")).isEqualTo("READY");
    }

    @Test
    void assetUploadBlockedWhenStorageQuotaExceeded() throws Exception {
        configureWorkspacePlan(SMALL_STORAGE_LIMIT_GB, true);
        long currentUsageBytes = storageLimitBytes() - 10L;
        seedStorageUsage(workspaceOne.getId(), currentUsageBytes, currentUsageBytes, 0L, 0L, 0L);

        MvcResult upload = uploadAsset(
                tokenFor(adminUser, workspaceOne.getId(), Role.ADMIN),
                "quota-blocked.png",
                pngBytes(),
                "image/png",
                "PRODUCT_IMAGE");

        assertThat(upload.getResponse().getStatus()).isEqualTo(409);
        assertThat(textAt(upload, "/message")).isEqualTo("Storage limit exceeded for current pricing plan");
    }

    @Test
    void assetUploadBlockedWhenPlanStorageLimitIsZero() throws Exception {
        configureWorkspacePlan(BigDecimal.ZERO, true);

        MvcResult upload = uploadAsset(
                tokenFor(adminUser, workspaceOne.getId(), Role.ADMIN),
                "zero-quota.png",
                pngBytes(),
                "image/png",
                "PRODUCT_IMAGE");

        assertThat(upload.getResponse().getStatus()).isEqualTo(409);
        assertThat(textAt(upload, "/message")).isEqualTo("Storage limit exceeded for current pricing plan");
    }

    @Test
    void storageUsageUpdatesAfterUpload() throws Exception {
        configureWorkspacePlan(SMALL_STORAGE_LIMIT_GB, true);

        MvcResult upload = uploadAsset(
                tokenFor(adminUser, workspaceOne.getId(), Role.ADMIN),
                "usage-updated.png",
                pngBytes(),
                "image/png",
                "PRODUCT_IMAGE");

        assertThat(upload.getResponse().getStatus()).isEqualTo(200);
        StorageUsageEntity usage = storageUsageRepository.findFirstByWorkspaceIdAndDeletedFalse(workspaceOne.getId()).orElseThrow();
        assertThat(usage.getTotalUsedBytes()).isEqualTo(pngBytes().length);
        assertThat(usage.getRawAssetBytes()).isEqualTo(pngBytes().length);
        assertThat(usage.getGeneratedAssetBytes()).isZero();
    }

    @Test
    void redisStorageUsageCacheWorks() throws Exception {
        configureWorkspacePlan(SMALL_STORAGE_LIMIT_GB, true);

        MvcResult upload = uploadAsset(
                tokenFor(adminUser, workspaceOne.getId(), Role.ADMIN),
                "usage-cache.png",
                pngBytes(),
                "image/png",
                "PRODUCT_IMAGE");

        assertThat(upload.getResponse().getStatus()).isEqualTo(200);
        StorageUsageCacheEntry cached = storageUsageRedisCacheService.get(workspaceOne.getId()).orElseThrow();
        assertThat(cached.workspaceId()).isEqualTo(workspaceOne.getId());
        assertThat(cached.totalUsedBytes()).isEqualTo(pngBytes().length);
        assertThat(redisTemplate.hasKey(StorageUsageRedisKeys.storageUsage(workspaceOne.getId()))).isEqualTo(Boolean.TRUE);
    }

    @Test
    void uploadQuotaLockPreventsRaceCondition() throws Exception {
        configureWorkspacePlan(SMALL_STORAGE_LIMIT_GB, true);
        RedisLockService.RedisLockToken lockToken = redisLockService.acquire(
                redisKeyBuilder.uploadQuotaLock(workspaceOne.getId()),
                Duration.ofMinutes(5)).orElseThrow();
        try {
            MvcResult upload = uploadAsset(
                    tokenFor(adminUser, workspaceOne.getId(), Role.ADMIN),
                    "quota-locked.png",
                    pngBytes(),
                    "image/png",
                    "PRODUCT_IMAGE");

            assertThat(upload.getResponse().getStatus()).isEqualTo(409);
            assertThat(textAt(upload, "/message")).isEqualTo("Asset upload quota validation is already in progress");
        } finally {
            redisLockService.releaseQuietly(lockToken);
        }
    }

    @Test
    void publicShareActionChecksPlanFeatureFoundation() throws Exception {
        configureWorkspacePlan(new BigDecimal("5.0000"), false);
        String accessToken = tokenFor(adminUser, workspaceOne.getId(), Role.ADMIN);
        var generatedVersion = createShareableGeneratedVersion();

        mockMvc.perform(post("/api/v1/share-links")
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(java.util.Map.of(
                                "generatedVersionId", generatedVersion.getId(),
                                "expiresAt", Instant.now().plusSeconds(3600).toString()))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Public share is not available for current pricing plan"));
    }

    @Test
    void storageLimitExceededReturnsStandardApiResponse() throws Exception {
        configureWorkspacePlan(SMALL_STORAGE_LIMIT_GB, true);
        long currentUsageBytes = storageLimitBytes() - 10L;
        seedStorageUsage(workspaceOne.getId(), currentUsageBytes, currentUsageBytes, 0L, 0L, 0L);

        MvcResult upload = uploadAsset(
                tokenFor(adminUser, workspaceOne.getId(), Role.ADMIN),
                "quota-envelope.png",
                pngBytes(),
                "image/png",
                "PRODUCT_IMAGE");

        assertThat(upload.getResponse().getStatus()).isEqualTo(409);
        assertThat(json(upload).at("/success").asBoolean()).isFalse();
        assertThat(json(upload).at("/message").asText()).isEqualTo("Storage limit exceeded for current pricing plan");
        assertThat(json(upload).at("/data").isNull()).isTrue();
        assertThat(json(upload).at("/errors").isArray()).isTrue();
        assertThat(json(upload).at("/errors").size()).isZero();
        assertThat(json(upload).at("/timestamp").asText()).isNotBlank();
    }

    @Test
    void kafkaStorageUsageUpdatedEventPublished() throws Exception {
        configureWorkspacePlan(SMALL_STORAGE_LIMIT_GB, true);
        String accessToken = tokenFor(adminUser, workspaceOne.getId(), Role.ADMIN);

        try (Consumer<String, String> consumer = createConsumer(storagePlanKafkaTopicNames.storageUsageUpdated())) {
            MvcResult upload = uploadAsset(accessToken, "usage-event.png", pngBytes(), "image/png", "PRODUCT_IMAGE");
            UUID assetId = uuidAt(upload, "/data/id");

            List<ConsumerRecord<String, String>> records = pollRecords(consumer, 1, Duration.ofSeconds(10));

            assertThat(records).isNotEmpty();
            assertThat(records.getFirst().topic()).isEqualTo(storagePlanKafkaTopicNames.storageUsageUpdated());
            assertThat(records.getFirst().value()).contains(
                    workspaceOne.getId().toString(),
                    assetId.toString(),
                    "ASSET_UPLOAD_COMPLETED");
        }
    }

    @Test
    void kafkaAssetUploadBlockedByPlanEventPublished() throws Exception {
        configureWorkspacePlan(SMALL_STORAGE_LIMIT_GB, true);
        long currentUsageBytes = storageLimitBytes() - 10L;
        seedStorageUsage(workspaceOne.getId(), currentUsageBytes, currentUsageBytes, 0L, 0L, 0L);
        String accessToken = tokenFor(adminUser, workspaceOne.getId(), Role.ADMIN);

        try (Consumer<String, String> consumer = createConsumer(storagePlanKafkaTopicNames.assetUploadBlockedByPlan())) {
            MvcResult upload = uploadAsset(accessToken, "blocked-event.png", pngBytes(), "image/png", "PRODUCT_IMAGE");

            assertThat(upload.getResponse().getStatus()).isEqualTo(409);
            List<ConsumerRecord<String, String>> records = pollRecords(consumer, 1, Duration.ofSeconds(10));

            assertThat(records).isNotEmpty();
            assertThat(records.getFirst().topic()).isEqualTo(storagePlanKafkaTopicNames.assetUploadBlockedByPlan());
            assertThat(records.getFirst().value()).contains(
                    workspaceOne.getId().toString(),
                    "ASSET_UPLOAD_BLOCKED_BY_PLAN");
        }
    }

    @Test
    void crossWorkspaceStorageUsageIsolationWorks() throws Exception {
        configureWorkspacePlan(SMALL_STORAGE_LIMIT_GB, true);
        seedStorageUsage(workspaceTwo.getId(), 500L, 500L, 0L, 0L, 0L);

        MvcResult upload = uploadAsset(
                tokenFor(adminUser, workspaceOne.getId(), Role.ADMIN),
                "workspace-isolation.png",
                pngBytes(),
                "image/png",
                "PRODUCT_IMAGE");

        assertThat(upload.getResponse().getStatus()).isEqualTo(200);
        StorageUsageEntity workspaceOneUsage = storageUsageRepository.findFirstByWorkspaceIdAndDeletedFalse(workspaceOne.getId()).orElseThrow();
        StorageUsageEntity workspaceTwoUsage = storageUsageRepository.findFirstByWorkspaceIdAndDeletedFalse(workspaceTwo.getId()).orElseThrow();
        assertThat(workspaceOneUsage.getTotalUsedBytes()).isEqualTo(pngBytes().length);
        assertThat(workspaceTwoUsage.getTotalUsedBytes()).isEqualTo(500L);
    }

    private void configureWorkspacePlan(BigDecimal maxStorageGb, boolean allowPublicShareLinks) {
        PricingPlan plan = PricingPlan.create(
                "Storage Test Plan",
                ("STORAGE_" + UUID.randomUUID()).replace('-', '_'),
                "Plan for pricing/storage integration tests",
                new BigDecimal("25.0000"),
                new BigDecimal("250.0000"),
                "USD",
                false,
                true,
                95);
        plan = pricingPlanRepository.save(plan);
        UUID planId = plan.getId();
        planFeaturePolicyRepository.save(PlanFeaturePolicy.create(
                planId,
                12,
                5,
                10,
                10,
                8,
                maxStorageGb,
                new BigDecimal("500.0000"),
                true,
                allowPublicShareLinks,
                false,
                false,
                true,
                true));
        WorkspaceSubscription subscription = workspaceSubscriptionRepository.findFirstByWorkspaceIdAndDeletedFalse(workspaceOne.getId())
                .orElse(null);
        if (subscription != null) {
            subscription.update(
                    planId,
                    WorkspaceSubscriptionStatus.ACTIVE,
                    Instant.now(),
                    Instant.now().plusSeconds(86400),
                    null,
                    true);
        } else {
            subscription = WorkspaceSubscription.create(
                    workspaceOne.getId(),
                    planId,
                    WorkspaceSubscriptionStatus.ACTIVE,
                    Instant.now(),
                    Instant.now().plusSeconds(86400),
                    null,
                    true);
        }
        workspaceSubscriptionRepository.save(subscription);
        redisTemplate.delete(PricingRedisKeys.workspaceSubscription(workspaceOne.getId()));
        redisTemplate.delete(PricingRedisKeys.planFeatures(planId));
        redisTemplate.delete(PricingRedisKeys.pricingPlan(planId));
    }

    private void seedStorageUsage(
            UUID workspaceId,
            long totalUsedBytes,
            long rawAssetBytes,
            long generatedAssetBytes,
            long variantBytes,
            long deletedBytes
    ) {
        storageUsageRepository.findFirstByWorkspaceIdAndDeletedFalse(workspaceId)
                .ifPresent(storageUsageRepository::delete);
        storageUsageRepository.save(StorageUsageEntity.create(
                workspaceId,
                totalUsedBytes,
                rawAssetBytes,
                generatedAssetBytes,
                variantBytes,
                deletedBytes,
                Instant.now()));
        storageUsageRedisCacheService.invalidate(workspaceId);
    }

    private long storageLimitBytes() {
        return SMALL_STORAGE_LIMIT_GB.multiply(BigDecimal.valueOf(1024L * 1024L * 1024L))
                .setScale(0, java.math.RoundingMode.DOWN)
                .longValue();
    }
}
