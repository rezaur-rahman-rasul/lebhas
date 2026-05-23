package com.lebhas.creativesaas.usage;

import com.lebhas.creativesaas.common.api.ApiResponse;
import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.common.security.Permission;
import com.lebhas.creativesaas.common.security.Role;
import com.lebhas.creativesaas.generation.event.CreativeGenerationKafkaTopicNames;
import com.lebhas.creativesaas.generation.event.CreditLifecycleEventDto;
import com.lebhas.creativesaas.generation.event.GenerationEventProducer;
import com.lebhas.creativesaas.identity.application.WorkspaceAuthorizationService;
import com.lebhas.creativesaas.messaging.kafka.KafkaTopicConstants;
import com.lebhas.creativesaas.redis.RedisCacheService;
import com.lebhas.creativesaas.redis.RedisKeyBuilder;
import com.lebhas.creativesaas.redis.RedisLockService;
import com.lebhas.creativesaas.redis.RedisWalletCache;
import com.lebhas.creativesaas.usage.application.CreditBalanceService;
import com.lebhas.creativesaas.usage.application.CreditLedgerService;
import com.lebhas.creativesaas.usage.application.CreditUsageMapper;
import com.lebhas.creativesaas.usage.application.PlanUsagePolicyResolver;
import com.lebhas.creativesaas.usage.application.QuotaValidationService;
import com.lebhas.creativesaas.usage.application.UsageBillingAccessService;
import com.lebhas.creativesaas.usage.application.WorkspaceLimitService;
import com.lebhas.creativesaas.usage.application.dto.CreditBalanceView;
import com.lebhas.creativesaas.usage.application.dto.WorkspaceUsageSummaryView;
import com.lebhas.creativesaas.usage.cache.CreditBalanceCacheService;
import com.lebhas.creativesaas.usage.cache.UsageBillingLockService;
import com.lebhas.creativesaas.usage.cache.UsageBillingRedisAccessSupport;
import com.lebhas.creativesaas.usage.cache.UsageBillingRedisKeys;
import com.lebhas.creativesaas.usage.cache.UsageBillingRedisTtlStrategy;
import com.lebhas.creativesaas.usage.cache.WorkspaceUsageSummaryCacheService;
import com.lebhas.creativesaas.usage.domain.CreditLedger;
import com.lebhas.creativesaas.usage.domain.CreditLedgerTransactionType;
import com.lebhas.creativesaas.usage.domain.DownloadUsageLog;
import com.lebhas.creativesaas.usage.domain.MonthlyUsageSnapshot;
import com.lebhas.creativesaas.usage.domain.ShareUsageLog;
import com.lebhas.creativesaas.usage.domain.UsageBillingLog;
import com.lebhas.creativesaas.usage.domain.WorkspaceUsageSummary;
import com.lebhas.creativesaas.usage.event.DownloadTrackedEventDto;
import com.lebhas.creativesaas.usage.event.ShareAccessedEventDto;
import com.lebhas.creativesaas.usage.event.UsageBillingEventProducer;
import com.lebhas.creativesaas.usage.event.UsageBillingKafkaTopicNames;
import com.lebhas.creativesaas.usage.event.UsageUpdatedEventDto;
import com.lebhas.creativesaas.usage.infrastructure.persistence.CreditLedgerRepository;
import com.lebhas.creativesaas.workspace.domain.WorkspaceEntity;
import com.lebhas.creativesaas.workspace.domain.WorkspaceLanguage;
import com.lebhas.pricing.PlanFeaturePolicy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class Day8UsageBillingBackendUnitTest {

    private static final UUID WORKSPACE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OTHER_WORKSPACE_ID = UUID.fromString("99999999-9999-9999-9999-999999999999");
    private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID CREATIVE_REQUEST_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID GENERATED_VERSION_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID GENERATION_JOB_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final UUID ASSET_ID = UUID.fromString("66666666-6666-6666-6666-666666666666");
    private static final UUID SHARE_LINK_ID = UUID.fromString("77777777-7777-7777-7777-777777777777");
    private static final UUID REFERENCE_ID = UUID.fromString("88888888-8888-8888-8888-888888888888");
    private static final LocalDate MAY_2026 = LocalDate.of(2026, 5, 1);

    @Test
    void creditLedgerPersistsCorrectly() {
        CreditLedgerService service = creditLedgerService();

        CreditLedger ledger = service.append(
                WORKSPACE_ID,
                CREATIVE_REQUEST_ID,
                GENERATED_VERSION_ID,
                GENERATION_JOB_ID,
                CreditLedgerTransactionType.RESERVE,
                new BigDecimal("4.12555"),
                new BigDecimal("20"),
                new BigDecimal("15.87445"),
                "GENERATION_JOB",
                REFERENCE_ID,
                "reserve generation credits",
                USER_ID);

        assertThat(ledger.getId()).isNotNull();
        assertThat(ledger.getWorkspaceId()).isEqualTo(WORKSPACE_ID);
        assertThat(ledger.getCreditsAmount()).isEqualByComparingTo("4.1256");
        assertThat(ledger.getBalanceAfterTransaction()).isEqualByComparingTo("15.8745");
    }

    @Test
    void creditLedgerIsAppendOnly() {
        assertThat(CreditLedger.class.getDeclaredMethods())
                .filteredOn(method -> Modifier.isPublic(method.getModifiers()))
                .extracting(method -> method.getName())
                .noneMatch(name -> name.startsWith("set") || name.startsWith("update"));
    }

    @Test
    void creditReserveCreatesLedgerEntry() {
        CreditLedger ledger = appendLedger(CreditLedgerTransactionType.RESERVE);

        assertThat(ledger.getTransactionType()).isEqualTo(CreditLedgerTransactionType.RESERVE);
        assertThat(ledger.getReferenceType()).isEqualTo("GENERATION_JOB");
    }

    @Test
    void creditFinalizeCreatesLedgerEntry() {
        CreditLedger ledger = appendLedger(CreditLedgerTransactionType.FINALIZE);

        assertThat(ledger.getTransactionType()).isEqualTo(CreditLedgerTransactionType.FINALIZE);
        assertThat(ledger.getCreditsAmount()).isEqualByComparingTo("3.0000");
    }

    @Test
    void creditRefundCreatesLedgerEntry() {
        CreditLedger ledger = appendLedger(CreditLedgerTransactionType.REFUND);

        assertThat(ledger.getTransactionType()).isEqualTo(CreditLedgerTransactionType.REFUND);
        assertThat(ledger.getCreatedBy()).isEqualTo(USER_ID);
    }

    @Test
    void workspaceUsageSummaryPersistsCorrectly() {
        WorkspaceUsageSummary summary = WorkspaceUsageSummary.create(WORKSPACE_ID, LocalDate.of(2026, 5, 23));
        summary.recordReservation(new BigDecimal("5"));
        summary.recordFinalization(new BigDecimal("2"));
        summary.recordLayerExecutionCost(new BigDecimal("0.125"));
        summary.recordDownload();
        summary.recordPublicShareAccess();

        assertThat(summary.getUsageMonth()).isEqualTo(MAY_2026);
        assertThat(summary.getReservedCredits()).isEqualByComparingTo("3.0000");
        assertThat(summary.getUsedCredits()).isEqualByComparingTo("2.0000");
        assertThat(summary.getTotalLayerExecutions()).isEqualTo(1);
        assertThat(summary.getTotalDownloads()).isEqualTo(1);
        assertThat(summary.getTotalPublicShares()).isEqualTo(1);
    }

    @Test
    void workspaceIdAndUsageMonthUniquenessWorks() {
        Table table = WorkspaceUsageSummary.class.getAnnotation(Table.class);

        assertThat(table.uniqueConstraints())
                .extracting(UniqueConstraint::name)
                .contains("uk_workspace_usage_summaries_workspace_month");
    }

    @Test
    void usageBillingLogCreatedForLayerExecution() {
        UsageBillingLog log = UsageBillingLog.create(
                WORKSPACE_ID,
                "AI_LAYER_EXECUTION",
                "LAYER_EXECUTION_LOG",
                REFERENCE_ID,
                new BigDecimal("2"),
                new BigDecimal("0.0425"),
                UUID.randomUUID(),
                UUID.randomUUID());

        assertThat(log.getUsageType()).isEqualTo("AI_LAYER_EXECUTION");
        assertThat(log.getReferenceType()).isEqualTo("LAYER_EXECUTION_LOG");
        assertThat(log.getEstimatedCostUsd()).isEqualByComparingTo("0.042500");
    }

    @Test
    void downloadUsageLogCreatedAfterDownload() {
        DownloadUsageLog log = DownloadUsageLog.create(
                WORKSPACE_ID,
                GENERATED_VERSION_ID,
                ASSET_ID,
                USER_ID,
                "GENERATED_VERSION",
                "127.0.0.1",
                "JUnit");

        assertThat(log.getWorkspaceId()).isEqualTo(WORKSPACE_ID);
        assertThat(log.getGeneratedVersionId()).isEqualTo(GENERATED_VERSION_ID);
        assertThat(log.getAssetId()).isEqualTo(ASSET_ID);
        assertThat(log.getDownloadType()).isEqualTo("GENERATED_VERSION");
    }

    @Test
    void shareUsageLogCreatedAfterPublicShareAccess() {
        ShareUsageLog log = ShareUsageLog.create(
                WORKSPACE_ID,
                SHARE_LINK_ID,
                GENERATED_VERSION_ID,
                USER_ID,
                "127.0.0.1",
                "JUnit",
                "https://example.test");

        assertThat(log.getWorkspaceId()).isEqualTo(WORKSPACE_ID);
        assertThat(log.getShareLinkId()).isEqualTo(SHARE_LINK_ID);
        assertThat(log.getGeneratedVersionId()).isEqualTo(GENERATED_VERSION_ID);
        assertThat(log.getReferrer()).isEqualTo("https://example.test");
    }

    @Test
    void monthlyUsageSnapshotCreatedCorrectly() {
        UUID planId = UUID.randomUUID();
        UUID subscriptionId = UUID.randomUUID();

        MonthlyUsageSnapshot snapshot = MonthlyUsageSnapshot.create(
                WORKSPACE_ID,
                LocalDate.of(2026, 5, 23),
                planId,
                subscriptionId,
                new BigDecimal("25"),
                7,
                3,
                new BigDecimal("0.80"),
                1024,
                4,
                2);

        assertThat(snapshot.getUsageMonth()).isEqualTo(MAY_2026);
        assertThat(snapshot.getPricingPlanId()).isEqualTo(planId);
        assertThat(snapshot.getSubscriptionId()).isEqualTo(subscriptionId);
        assertThat(snapshot.getUsedCredits()).isEqualByComparingTo("25.0000");
    }

    @Test
    void monthlyCreditLimitEnforcementWorks() {
        QuotaValidationService service = quotaValidationService(featurePolicy(new BigDecimal("10"), new BigDecimal("5"), true), new BigDecimal("8"), BigDecimal.ZERO);

        assertThatThrownBy(() -> service.validateMonthlyCreditLimit(WORKSPACE_ID, new BigDecimal("3")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PLAN_QUOTA_EXCEEDED);
    }

    @Test
    void maxStorageGbEnforcementWorks() {
        QuotaValidationService service = quotaValidationService(featurePolicy(new BigDecimal("100"), BigDecimal.ONE, true), BigDecimal.ZERO, new BigDecimal("0.9000"));

        assertThatThrownBy(() -> service.validateStorageLimit(WORKSPACE_ID, 214_748_365L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PLAN_QUOTA_EXCEEDED);
    }

    @Test
    void allowPublicShareLinksEnforcementWorks() {
        QuotaValidationService service = quotaValidationService(featurePolicy(new BigDecimal("100"), BigDecimal.TEN, false), BigDecimal.ZERO, BigDecimal.ZERO);

        assertThatThrownBy(() -> service.validatePublicShareLinksAllowed(WORKSPACE_ID))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PLAN_FEATURE_DISABLED);
    }

    @Test
    void creditLockPreventsConcurrentReserveRace() {
        RedisLockService redisLockService = mock(RedisLockService.class);
        when(redisLockService.acquire(eq("lock:wallet:" + WORKSPACE_ID), eq(Duration.ofSeconds(10)))).thenReturn(Optional.empty());
        CreditBalanceService service = new CreditBalanceService(
                mock(com.lebhas.creativesaas.credit.infrastructure.persistence.CreditWalletRepository.class),
                mock(RedisWalletCache.class),
                redisLockService,
                new RedisKeyBuilder(),
                new CreditUsageMapper());

        assertThatThrownBy(() -> service.withCreditLock(WORKSPACE_ID, () -> "reserved"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CREDIT_RESERVE_INVALID);
    }

    @Test
    void usageSummaryLockPreventsRaceCondition() {
        RedisLockService redisLockService = mock(RedisLockService.class);
        when(redisLockService.acquire(eq("lock:usage-summary:" + WORKSPACE_ID + ":2026-05-01"), eq(Duration.ofSeconds(45))))
                .thenReturn(Optional.empty());
        UsageBillingLockService service = new UsageBillingLockService(
                new UsageBillingRedisKeys(),
                new UsageBillingRedisAccessSupport(mock(RedisCacheService.class), redisLockService),
                new UsageBillingRedisTtlStrategy());

        assertThat(service.acquireUsageSummaryLock(WORKSPACE_ID, MAY_2026)).isEmpty();
    }

    @Test
    void redisUsageSummaryCacheWorks() {
        RedisCacheService redis = mock(RedisCacheService.class);
        WorkspaceUsageSummaryView view = usageSummaryView();
        when(redis.get("usage:summary:" + WORKSPACE_ID + ":2026-05-01", WorkspaceUsageSummaryView.class))
                .thenReturn(Optional.of(view));
        WorkspaceUsageSummaryCacheService cache = new WorkspaceUsageSummaryCacheService(
                new UsageBillingRedisKeys(),
                new UsageBillingRedisAccessSupport(redis, mock(RedisLockService.class)),
                new UsageBillingRedisTtlStrategy());

        assertThat(cache.put(view)).isTrue();
        assertThat(cache.get(WORKSPACE_ID, MAY_2026)).contains(view);
        verify(redis).set("usage:summary:" + WORKSPACE_ID + ":2026-05-01", view, Duration.ofMinutes(30));
    }

    @Test
    void redisCreditBalanceCacheWorks() {
        RedisCacheService redis = mock(RedisCacheService.class);
        CreditBalanceView view = new CreditBalanceView(WORKSPACE_ID, new BigDecimal("50"), new BigDecimal("10"), new BigDecimal("40"));
        when(redis.get("credits:balance:" + WORKSPACE_ID, CreditBalanceView.class)).thenReturn(Optional.of(view));
        CreditBalanceCacheService cache = new CreditBalanceCacheService(
                new UsageBillingRedisKeys(),
                new UsageBillingRedisAccessSupport(redis, mock(RedisLockService.class)),
                new UsageBillingRedisTtlStrategy());

        assertThat(cache.put(view)).isTrue();
        assertThat(cache.get(WORKSPACE_ID)).contains(view);
        verify(redis).set("credits:balance:" + WORKSPACE_ID, view, Duration.ofMinutes(10));
    }

    @Test
    void kafkaCreditsReservedEventPublished() {
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, Object> kafkaTemplate = mock(KafkaTemplate.class);
        GenerationEventProducer producer = new GenerationEventProducer(kafkaTemplate, new CreativeGenerationKafkaTopicNames(""));
        CreditLifecycleEventDto event = new CreditLifecycleEventDto(WORKSPACE_ID, CREATIVE_REQUEST_ID, null, REFERENCE_ID, BigDecimal.TEN, "RESERVED", null, Instant.now());

        producer.publishCreditsReserved(event);

        verify(kafkaTemplate).send(KafkaTopicConstants.CREDITS_RESERVED, REFERENCE_ID.toString(), event);
    }

    @Test
    void kafkaUsageUpdatedEventPublished() {
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, Object> kafkaTemplate = mock(KafkaTemplate.class);
        UsageBillingEventProducer producer = new UsageBillingEventProducer(kafkaTemplate, new UsageBillingKafkaTopicNames(""));
        UsageUpdatedEventDto event = new UsageUpdatedEventDto(WORKSPACE_ID, MAY_2026, REFERENCE_ID, "GENERATION_JOB", "FINALIZED", Instant.now());

        producer.publishUsageUpdated(event);

        verify(kafkaTemplate).send(KafkaTopicConstants.USAGE_UPDATED, REFERENCE_ID.toString(), event);
    }

    @Test
    void kafkaDownloadTrackedEventPublished() {
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, Object> kafkaTemplate = mock(KafkaTemplate.class);
        UsageBillingEventProducer producer = new UsageBillingEventProducer(kafkaTemplate, new UsageBillingKafkaTopicNames(""));
        DownloadTrackedEventDto event = new DownloadTrackedEventDto(WORKSPACE_ID, REFERENCE_ID, GENERATED_VERSION_ID, ASSET_ID, USER_ID, "GENERATED_VERSION", MAY_2026, true, Instant.now());

        producer.publishDownloadTracked(event);

        verify(kafkaTemplate).send(KafkaTopicConstants.DOWNLOAD_TRACKED, REFERENCE_ID.toString(), event);
    }

    @Test
    void kafkaShareAccessedEventPublished() {
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, Object> kafkaTemplate = mock(KafkaTemplate.class);
        UsageBillingEventProducer producer = new UsageBillingEventProducer(kafkaTemplate, new UsageBillingKafkaTopicNames(""));
        ShareAccessedEventDto event = new ShareAccessedEventDto(WORKSPACE_ID, REFERENCE_ID, SHARE_LINK_ID, GENERATED_VERSION_ID, USER_ID, 4, MAY_2026, true, Instant.now());

        producer.publishShareAccessed(event);

        verify(kafkaTemplate).send(KafkaTopicConstants.SHARE_ACCESSED, REFERENCE_ID.toString(), event);
    }

    @Test
    void workspaceIsolationEnforced() {
        WorkspaceAuthorizationService authorizationService = mock(WorkspaceAuthorizationService.class);
        when(authorizationService.requireWorkspaceContext(OTHER_WORKSPACE_ID))
                .thenThrow(new BusinessException(ErrorCode.WORKSPACE_ACCESS_DENIED));
        UsageBillingAccessService service = new UsageBillingAccessService(authorizationService);

        assertThatThrownBy(() -> service.requireUsageBillingView(OTHER_WORKSPACE_ID))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.WORKSPACE_ACCESS_DENIED);
    }

    @Test
    void noHardcodedFreeBasicProEnterpriseLogic() throws Exception {
        Pattern forbiddenPlanNames = Pattern.compile("(?i)\\b(free|basic|pro|enterprise)\\b");
        try (var files = Files.walk(Path.of("src/main/java/com/lebhas/creativesaas/usage"))) {
            assertThat(files
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> containsForbiddenPlanName(path, forbiddenPlanNames))
                    .map(Path::toString)
                    .toList()).isEmpty();
        }
    }

    @Test
    void standardApiResponseFormatWorks() {
        ApiResponse<WorkspaceUsageSummaryView> response = ApiResponse.success("Usage loaded", usageSummaryView());

        assertThat(response.success()).isTrue();
        assertThat(response.message()).isEqualTo("Usage loaded");
        assertThat(response.data().workspaceId()).isEqualTo(WORKSPACE_ID);
        assertThat(response.errors()).isEmpty();
        assertThat(response.timestamp()).isNotNull();
    }

    @Test
    void adminCanViewOwnWorkspaceUsageWhenPermissionAllows() {
        WorkspaceAuthorizationService authorizationService = mock(WorkspaceAuthorizationService.class);
        WorkspaceEntity workspace = WorkspaceEntity.create("Workspace", "workspace", null, null, null, "Asia/Dhaka", WorkspaceLanguage.ENGLISH, "USD", "US", USER_ID);
        ReflectionTestUtils.setField(workspace, "id", WORKSPACE_ID);
        when(authorizationService.requireWorkspaceContext(WORKSPACE_ID))
                .thenReturn(new WorkspaceAuthorizationService.WorkspaceAccess(workspace, null, null, Role.ADMIN, Set.of(Permission.WORKSPACE_SETTINGS_VIEW)));
        UsageBillingAccessService service = new UsageBillingAccessService(authorizationService);

        assertThat(service.requireUsageBillingView(WORKSPACE_ID)).isEqualTo(WORKSPACE_ID);
    }

    private CreditLedger appendLedger(CreditLedgerTransactionType transactionType) {
        return creditLedgerService().append(
                WORKSPACE_ID,
                CREATIVE_REQUEST_ID,
                GENERATED_VERSION_ID,
                GENERATION_JOB_ID,
                transactionType,
                new BigDecimal("3"),
                new BigDecimal("20"),
                new BigDecimal("17"),
                "GENERATION_JOB",
                REFERENCE_ID,
                transactionType.name().toLowerCase(),
                USER_ID);
    }

    private CreditLedgerService creditLedgerService() {
        CreditLedgerRepository repository = mock(CreditLedgerRepository.class);
        when(repository.save(any(CreditLedger.class))).thenAnswer(invocation -> {
            CreditLedger ledger = invocation.getArgument(0);
            ReflectionTestUtils.setField(ledger, "id", UUID.randomUUID());
            ReflectionTestUtils.setField(ledger, "createdAt", Instant.now());
            return ledger;
        });
        return new CreditLedgerService(repository, new CreditUsageMapper());
    }

    @SuppressWarnings("unchecked")
    private QuotaValidationService quotaValidationService(PlanFeaturePolicy policy, BigDecimal usedCredits, BigDecimal storageGb) {
        PlanUsagePolicyResolver resolver = mock(PlanUsagePolicyResolver.class);
        when(resolver.resolve(WORKSPACE_ID)).thenReturn(new PlanUsagePolicyResolver.PlanUsagePolicy(WORKSPACE_ID, null, policy));
        WorkspaceLimitService limits = mock(WorkspaceLimitService.class);
        when(limits.usedCreditsThisMonth(WORKSPACE_ID)).thenReturn(usedCredits);
        when(limits.reservedCreditsThisMonth(WORKSPACE_ID)).thenReturn(BigDecimal.ZERO);
        when(limits.storageUsedGb(WORKSPACE_ID)).thenReturn(storageGb);
        ObjectProvider<RedisLockService> lockProvider = mock(ObjectProvider.class);
        when(lockProvider.getIfAvailable()).thenReturn(null);
        return new QuotaValidationService(resolver, limits, lockProvider, new RedisKeyBuilder());
    }

    private PlanFeaturePolicy featurePolicy(BigDecimal monthlyCreditLimit, BigDecimal maxStorageGb, boolean allowPublicShareLinks) {
        return PlanFeaturePolicy.create(
                UUID.randomUUID(),
                5,
                null,
                null,
                null,
                8,
                maxStorageGb,
                monthlyCreditLimit,
                true,
                allowPublicShareLinks,
                false,
                false,
                true,
                false);
    }

    private WorkspaceUsageSummaryView usageSummaryView() {
        return new WorkspaceUsageSummaryView(
                UUID.randomUUID(),
                WORKSPACE_ID,
                MAY_2026,
                new BigDecimal("20.0000"),
                new BigDecimal("2.0000"),
                BigDecimal.ZERO.setScale(4),
                3,
                4,
                5,
                new BigDecimal("0.250000"),
                1,
                2048,
                2,
                1,
                0,
                0,
                11,
                Instant.now());
    }

    private boolean containsForbiddenPlanName(Path path, Pattern forbiddenPlanNames) {
        try {
            String source = Files.readString(path, StandardCharsets.UTF_8);
            return forbiddenPlanNames.matcher(source).find();
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to inspect " + path, exception);
        }
    }
}
