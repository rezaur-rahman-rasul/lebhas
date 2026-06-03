package com.lebhas.creativesaas.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lebhas.ai.application.AiAnalyticsMonitoringQueryService;
import com.lebhas.ai.application.AiCostIntelligenceService;
import com.lebhas.ai.application.AiProviderHealthService;
import com.lebhas.ai.application.AiProviderMetricsService;
import com.lebhas.ai.application.CostEfficiencyCalculator;
import com.lebhas.ai.application.DynamicRoutingOptimizationService;
import com.lebhas.ai.application.GeneratedVersionQualityService;
import com.lebhas.ai.application.BanglaTypographyQualityService;
import com.lebhas.ai.application.BrandingQualityService;
import com.lebhas.ai.application.CompositionQualityService;
import com.lebhas.ai.application.ProductPreservationQualityService;
import com.lebhas.ai.application.ProviderCostComparisonService;
import com.lebhas.ai.application.ProviderReliabilityScorer;
import com.lebhas.ai.application.QualityScoreCalculator;
import com.lebhas.ai.application.WorkspaceAiUsageAggregator;
import com.lebhas.ai.application.WorkspaceAiUsageService;
import com.lebhas.ai.application.AiUsageAnalyticsMapper;
import com.lebhas.ai.application.dto.CostEstimateInput;
import com.lebhas.ai.application.dto.CostObservation;
import com.lebhas.ai.application.dto.ProviderHealthSnapshot;
import com.lebhas.ai.application.dto.ProviderMetricsSnapshot;
import com.lebhas.ai.application.dto.QualityScoreInput;
import com.lebhas.ai.application.dto.QualityScoreResult;
import com.lebhas.ai.application.dto.WorkspaceAiUsageView;
import com.lebhas.ai.cache.AiFailureCacheService;
import com.lebhas.ai.cache.AiLayerAnalyticsCacheService;
import com.lebhas.ai.cache.AiProviderHealthCacheService;
import com.lebhas.ai.cache.AiRedisAccessSupport;
import com.lebhas.ai.cache.AiRedisCacheProperties;
import com.lebhas.ai.cache.AiRedisTtlStrategy;
import com.lebhas.ai.cache.WorkspaceAiUsageCacheService;
import com.lebhas.ai.consumer.AiFailureLogConsumer;
import com.lebhas.ai.consumer.AiLayerAnalyticsConsumer;
import com.lebhas.ai.consumer.AiWorkspaceUsageConsumer;
import com.lebhas.ai.domain.AiFailureLog;
import com.lebhas.ai.domain.AiFailureType;
import com.lebhas.ai.domain.AiLayerAnalytics;
import com.lebhas.ai.domain.AiProviderMetrics;
import com.lebhas.ai.domain.AiQualityScore;
import com.lebhas.ai.domain.AiToolProvider;
import com.lebhas.ai.domain.CreativeLayerType;
import com.lebhas.ai.domain.LayerToolMapping;
import com.lebhas.ai.domain.ProviderStatus;
import com.lebhas.ai.domain.ProviderType;
import com.lebhas.ai.event.AiFailureLoggedEvent;
import com.lebhas.ai.event.AiLayerLifecycleEvent;
import com.lebhas.ai.event.AiMonitoringKafkaTopicNames;
import com.lebhas.ai.event.AiProviderMetricsUpdatedEvent;
import com.lebhas.ai.event.AiWorkspaceUsageUpdatedEvent;
import com.lebhas.ai.infrastructure.persistence.AiFailureLogRepository;
import com.lebhas.ai.infrastructure.persistence.AiLayerAnalyticsRepository;
import com.lebhas.ai.infrastructure.persistence.AiModelRepository;
import com.lebhas.ai.infrastructure.persistence.AiProviderMetricsRepository;
import com.lebhas.ai.infrastructure.persistence.AiQualityScoreRepository;
import com.lebhas.ai.infrastructure.persistence.AiToolProviderRepository;
import com.lebhas.ai.infrastructure.persistence.LayerCostPolicyRepository;
import com.lebhas.ai.infrastructure.persistence.LayerQualityPolicyRepository;
import com.lebhas.ai.infrastructure.persistence.LayerToolMappingRepository;
import com.lebhas.ai.producer.AiMonitoringEventProducer;
import com.lebhas.creativesaas.brand.domain.BrandLanguagePreference;
import com.lebhas.creativesaas.common.exception.TenantIsolationException;
import com.lebhas.creativesaas.common.security.Permission;
import com.lebhas.creativesaas.common.security.Role;
import com.lebhas.creativesaas.common.security.context.CurrentUser;
import com.lebhas.creativesaas.common.security.context.CurrentUserContext;
import com.lebhas.creativesaas.creativerequest.domain.CreativeRequestEntity;
import com.lebhas.creativesaas.generatedversion.domain.ApprovalStatus;
import com.lebhas.creativesaas.generatedversion.domain.GeneratedVersionEntity;
import com.lebhas.creativesaas.generatedversion.domain.GeneratedVersionStatus;
import com.lebhas.creativesaas.generatedversion.domain.GenerationStatus;
import com.lebhas.creativesaas.generatedversion.infrastructure.persistence.GeneratedVersionRepository;
import com.lebhas.creativesaas.identity.application.WorkspaceAuthorizationService;
import com.lebhas.creativesaas.messaging.kafka.KafkaTopicConstants;
import com.lebhas.creativesaas.pricing.application.WorkspacePlanContextService;
import com.lebhas.creativesaas.prompt.domain.CampaignObjective;
import com.lebhas.creativesaas.prompt.domain.PromptPlatform;
import com.lebhas.creativesaas.redis.RedisCacheService;
import com.lebhas.creativesaas.redis.RedisLockService;
import com.lebhas.creativesaas.redis.RedisRateLimitService;
import com.lebhas.creativesaas.workspace.domain.WorkspaceEntity;
import com.lebhas.creativesaas.workspace.infrastructure.persistence.WorkspaceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class Day7AiAnalyticsMonitoringUnitTest {

    private static final Instant NOW = Instant.parse("2026-05-23T00:00:00Z");
    private static final UUID WORKSPACE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OTHER_WORKSPACE_ID = UUID.fromString("99999999-9999-9999-9999-999999999999");
    private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID PROVIDER_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID LAYER_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID CREATIVE_REQUEST_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final UUID GENERATED_VERSION_ID = UUID.fromString("66666666-6666-6666-6666-666666666666");

    @Test
    void aiProviderMetricsPersistsCorrectly() {
        AiProviderMetricsService service = providerMetricsService();

        AiProviderMetrics metrics = service.recordRequest(successObservation());

        assertThat(metrics.getProviderId()).isEqualTo(PROVIDER_ID);
        assertThat(metrics.getModelName()).isEqualTo("dynamic-model");
        assertThat(metrics.getTotalRequests()).isEqualTo(1);
        assertThat(metrics.getSuccessfulRequests()).isEqualTo(1);
    }

    @Test
    void aiLayerAnalyticsPersistsCorrectly() {
        AiCostIntelligenceService service = costIntelligenceService(mock(AiLayerAnalyticsRepository.class));

        AiLayerAnalytics analytics = service.trackLayerCost(successObservation());

        assertThat(analytics.getLayerId()).isEqualTo(LAYER_ID);
        assertThat(analytics.getProviderId()).isEqualTo(PROVIDER_ID);
        assertThat(analytics.getTotalExecutions()).isEqualTo(1);
        assertThat(analytics.getAvgExecutionCostUsd()).isEqualByComparingTo("0.250000");
    }

    @Test
    void workspaceAiUsagePersistsCorrectly() {
        WorkspaceAiUsageService service = workspaceAiUsageService();

        WorkspaceAiUsageView view = service.recordGenerationCompleted(WORKSPACE_ID, 2, BigDecimal.TEN, new BigDecimal("0.50"), new BigDecimal("1200"));

        assertThat(view.workspaceId()).isEqualTo(WORKSPACE_ID);
        assertThat(view.totalGeneratedVersions()).isEqualTo(2);
        assertThat(view.totalCreditsConsumed()).isEqualByComparingTo("10");
        assertThat(view.totalEstimatedCostUsd()).isEqualByComparingTo("0.50");
    }

    @Test
    void aiQualityScorePersistsCorrectly() {
        AiQualityScore score = AiQualityScore.create(
                GENERATED_VERSION_ID,
                WORKSPACE_ID,
                new BigDecimal("0.80"),
                new BigDecimal("0.70"),
                new BigDecimal("0.90"),
                new BigDecimal("0.80"),
                new BigDecimal("0.60"),
                new BigDecimal("0.85"),
                "measured");

        assertThat(score.getGeneratedVersionId()).isEqualTo(GENERATED_VERSION_ID);
        assertThat(score.getOverallScore()).isEqualByComparingTo("0.80");
        assertThat(score.getQualityNotes()).isEqualTo("measured");
    }

    @Test
    void aiFailureLogPersistsCorrectly() {
        AiFailureLogRepository repository = mock(AiFailureLogRepository.class);
        AiFailureLogConsumer consumer = failureConsumer(repository);
        when(repository.save(any(AiFailureLog.class))).thenAnswer(invocation -> {
            AiFailureLog log = invocation.getArgument(0);
            ReflectionTestUtils.setField(log, "id", UUID.randomUUID());
            return log;
        });

        consumer.consumeLayerFailed(layerEvent(false));

        var captor = forClass(AiFailureLog.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getFailureType()).isEqualTo(AiFailureType.TIMEOUT);
        assertThat(captor.getValue().getProviderId()).isEqualTo(PROVIDER_ID);
    }

    @Test
    void providerSuccessUpdatesMetrics() {
        AiProviderMetricsService service = providerMetricsService();

        AiProviderMetrics metrics = service.recordRequest(successObservation());

        assertThat(metrics.getSuccessfulRequests()).isEqualTo(1);
        assertThat(metrics.getFailedRequests()).isZero();
        assertThat(metrics.getLastSuccessAt()).isEqualTo(NOW);
    }

    @Test
    void providerFailureUpdatesMetrics() {
        AiProviderMetricsService service = providerMetricsService();

        AiProviderMetrics metrics = service.recordRequest(failureObservation());

        assertThat(metrics.getSuccessfulRequests()).isZero();
        assertThat(metrics.getFailedRequests()).isEqualTo(1);
        assertThat(metrics.getLastFailureAt()).isEqualTo(NOW);
    }

    @Test
    void layerAnalyticsUpdatesAfterExecutionEvent() {
        AiCostIntelligenceService costService = mock(AiCostIntelligenceService.class);
        AiLayerAnalytics analytics = layerAnalytics();
        when(costService.trackLayerCost(any(CostObservation.class))).thenReturn(analytics);
        AiLayerAnalyticsConsumer consumer = new AiLayerAnalyticsConsumer(
                objectMapper(),
                costService,
                mock(AiLayerAnalyticsCacheService.class),
                mock(AiMonitoringEventProducer.class));

        consumer.consumeLayerCompleted(layerEvent(true));

        verify(costService).trackLayerCost(any(CostObservation.class));
    }

    @Test
    void workspaceUsageUpdatesAfterGenerationEvent() {
        WorkspaceAiUsageService usageService = mock(WorkspaceAiUsageService.class);
        WorkspaceAiUsageView view = usageView();
        when(usageService.recordGenerationRequested(WORKSPACE_ID)).thenReturn(view);
        WorkspaceAiUsageCacheService cache = mock(WorkspaceAiUsageCacheService.class);
        AiMonitoringEventProducer producer = mock(AiMonitoringEventProducer.class);
        AiWorkspaceUsageConsumer consumer = new AiWorkspaceUsageConsumer(objectMapper(), usageService, cache, producer);

        consumer.consumeGenerationRequested(Map.of("workspaceId", WORKSPACE_ID, "creativeRequestId", CREATIVE_REQUEST_ID));

        verify(cache).store(view);
        verify(producer).publishWorkspaceUsageUpdated(any(AiWorkspaceUsageUpdatedEvent.class));
    }

    @Test
    void qualityScoreIsStoredForGeneratedVersion() {
        GeneratedVersionQualityService service = qualityService();

        QualityScoreResult result = service.scoreGeneratedVersion(qualityInput());

        assertThat(result.generatedVersionId()).isEqualTo(GENERATED_VERSION_ID);
        assertThat(result.overallScore()).isEqualByComparingTo("0.80");
    }

    @Test
    void costEstimationWorks() {
        CostEfficiencyCalculator calculator = new CostEfficiencyCalculator();

        BigDecimal estimate = calculator.estimateCostUsd(Map.of("unitCostUsd", "0.25"), new BigDecimal("4"));

        assertThat(estimate).isEqualByComparingTo("1.000000");
    }

    @Test
    void qualityToCostRatioCalculationWorks() {
        CostEfficiencyCalculator calculator = new CostEfficiencyCalculator();

        BigDecimal ratio = calculator.qualityToCostRatio(new BigDecimal("0.80"), new BigDecimal("0.25"));

        assertThat(ratio).isEqualByComparingTo("3.200000");
    }

    @Test
    void providerHealthCacheWorks() {
        RedisCacheService redis = mock(RedisCacheService.class);
        AiProviderHealthCacheService cache = new AiProviderHealthCacheService(redisAccess(redis), new AiRedisTtlStrategy(new AiRedisCacheProperties()));
        ProviderHealthSnapshot snapshot = providerHealth();
        when(redis.get("ai:provider:health:" + PROVIDER_ID, ProviderHealthSnapshot.class)).thenReturn(Optional.of(snapshot));

        assertThat(cache.store(snapshot)).isTrue();
        assertThat(cache.get(PROVIDER_ID)).contains(snapshot);
        verify(redis).set(eq("ai:provider:health:" + PROVIDER_ID), eq(snapshot), eq(Duration.ofMinutes(2)));
    }

    @Test
    void workspaceAiUsageCacheWorks() {
        RedisCacheService redis = mock(RedisCacheService.class);
        WorkspaceAiUsageCacheService cache = new WorkspaceAiUsageCacheService(redisAccess(redis), new AiRedisTtlStrategy(new AiRedisCacheProperties()));
        WorkspaceAiUsageView view = usageView();
        when(redis.get("ai:workspace:usage:" + WORKSPACE_ID, WorkspaceAiUsageView.class)).thenReturn(Optional.of(view));

        assertThat(cache.store(view)).isTrue();
        assertThat(cache.get(WORKSPACE_ID)).contains(view);
        verify(redis).set(eq("ai:workspace:usage:" + WORKSPACE_ID), eq(view), eq(Duration.ofMinutes(5)));
    }

    @Test
    void kafkaProviderMetricsUpdatedEventPublished() {
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, Object> kafkaTemplate = mock(KafkaTemplate.class);
        AiMonitoringEventProducer producer = new AiMonitoringEventProducer(kafkaTemplate, new AiMonitoringKafkaTopicNames(""));
        AiProviderMetricsUpdatedEvent event = new AiProviderMetricsUpdatedEvent(null, NOW, PROVIDER_ID, "dynamic-model", 1, 1, 0, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, Map.of());

        producer.publishProviderMetricsUpdated(event);

        verify(kafkaTemplate).send(KafkaTopicConstants.AI_PROVIDER_METRICS_UPDATED, PROVIDER_ID.toString(), event);
    }

    @Test
    void kafkaFailureLoggedEventPublished() {
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, Object> kafkaTemplate = mock(KafkaTemplate.class);
        AiMonitoringEventProducer producer = new AiMonitoringEventProducer(kafkaTemplate, new AiMonitoringKafkaTopicNames(""));
        AiFailureLoggedEvent event = new AiFailureLoggedEvent(null, NOW, UUID.randomUUID(), CREATIVE_REQUEST_ID, LAYER_ID, PROVIDER_ID, "dynamic-model", AiFailureType.TIMEOUT, "timeout", 1, true, Map.of());

        producer.publishFailureLogged(event);

        verify(kafkaTemplate).send(KafkaTopicConstants.AI_FAILURE_LOGGED, PROVIDER_ID.toString(), event);
    }

    @Test
    void masterCanViewProviderAnalytics() {
        AiAnalyticsMonitoringQueryService service = analyticsQueryService(currentUser(Role.MASTER, WORKSPACE_ID, Set.of()));
        when(providerHealthService(service).listProviderHealth()).thenReturn(List.of(providerHealth()));

        assertThat(service.listProviderMetricsForMaster()).hasSize(1);
    }

    @Test
    void adminCanViewOwnWorkspaceUsage() {
        AiAnalyticsMonitoringQueryService service = analyticsQueryService(currentUser(Role.ADMIN, WORKSPACE_ID, Set.of(Permission.WORKSPACE_VIEW)));
        when(workspaceUsageQueryService(service).getWorkspaceUsage(WORKSPACE_ID)).thenReturn(usageView());

        assertThat(service.getWorkspaceUsage(WORKSPACE_ID).workspaceId()).isEqualTo(WORKSPACE_ID);
    }

    @Test
    void crossWorkspaceUsageAccessBlocked() {
        AiAnalyticsMonitoringQueryService service = analyticsQueryService(currentUser(Role.ADMIN, WORKSPACE_ID, Set.of()));
        WorkspaceAuthorizationService authorizationService = workspaceAuthorizationService(service);
        when(authorizationService.requirePermission(OTHER_WORKSPACE_ID, Permission.WORKSPACE_VIEW))
                .thenThrow(new TenantIsolationException("denied"));

        assertThatThrownBy(() -> service.getWorkspaceUsage(OTHER_WORKSPACE_ID))
                .isInstanceOf(TenantIsolationException.class);
    }

    @Test
    void hardcodedProviderRoutingIsNotUsed() {
        LayerToolMappingRepository mappingRepository = mock(LayerToolMappingRepository.class);
        AiToolProviderRepository providerRepository = mock(AiToolProviderRepository.class);
        AiModelRepository modelRepository = mock(AiModelRepository.class);
        LayerToolMapping mapping = LayerToolMapping.create(LAYER_ID, PROVIDER_ID, null, null, "dynamic_mapping", 1, 1, true, true, Map.of());
        AiToolProvider provider = provider("ANY_VENDOR_2026");
        when(mappingRepository.findAllByPipelineLayerIdAndDeletedFalseOrderByPriorityOrderAsc(LAYER_ID)).thenReturn(List.of(mapping));
        when(providerRepository.findById(PROVIDER_ID)).thenReturn(Optional.of(provider));
        when(modelRepository.findAllByProviderIdAndDeletedFalseOrderByModelNameAsc(PROVIDER_ID)).thenReturn(List.of());

        ProviderCostComparisonService service = new ProviderCostComparisonService(mappingRepository, providerRepository, modelRepository, new CostEfficiencyCalculator());

        assertThat(service.compareProviderCostEfficiency(LAYER_ID, new CostEstimateInput(WORKSPACE_ID, CREATIVE_REQUEST_ID, BigDecimal.ONE, Map.of())))
                .extracting(option -> option.providerCode())
                .containsExactly("ANY_VENDOR_2026");
    }

    private AiProviderMetricsService providerMetricsService() {
        AiProviderMetricsRepository metricsRepository = mock(AiProviderMetricsRepository.class);
        AiToolProviderRepository providerRepository = mock(AiToolProviderRepository.class);
        when(providerRepository.findById(PROVIDER_ID)).thenReturn(Optional.of(provider("DYNAMIC_PROVIDER")));
        when(metricsRepository.findByProviderIdAndModelNameAndDeletedFalse(PROVIDER_ID, "dynamic-model"))
                .thenReturn(Optional.empty());
        when(metricsRepository.save(any(AiProviderMetrics.class))).thenAnswer(invocation -> invocation.getArgument(0));
        return new AiProviderMetricsService(metricsRepository, providerRepository, new ProviderReliabilityScorer());
    }

    private AiCostIntelligenceService costIntelligenceService(AiLayerAnalyticsRepository layerAnalyticsRepository) {
        when(layerAnalyticsRepository.findByLayerIdAndProviderIdAndModelNameAndDeletedFalse(LAYER_ID, PROVIDER_ID, "dynamic-model"))
                .thenReturn(Optional.empty());
        when(layerAnalyticsRepository.save(any(AiLayerAnalytics.class))).thenAnswer(invocation -> invocation.getArgument(0));
        return new AiCostIntelligenceService(
                mock(AiToolProviderRepository.class),
                mock(AiModelRepository.class),
                layerAnalyticsRepository,
                mock(AiProviderMetricsService.class),
                new CostEfficiencyCalculator(),
                mock(ProviderCostComparisonService.class),
                mock(com.lebhas.ai.application.LayerCostOptimizationService.class),
                mock(com.lebhas.ai.application.GenerationCostEstimator.class));
    }

    private WorkspaceAiUsageService workspaceAiUsageService() {
        WorkspaceRepository workspaceRepository = mock(WorkspaceRepository.class);
        WorkspaceEntity workspace = mock(WorkspaceEntity.class);
        when(workspace.getId()).thenReturn(WORKSPACE_ID);
        when(workspaceRepository.findByIdAndDeletedFalse(WORKSPACE_ID)).thenReturn(Optional.of(workspace));
        var repository = mock(com.lebhas.ai.infrastructure.persistence.WorkspaceAiUsageRepository.class);
        when(repository.findByWorkspaceIdAndDeletedFalse(WORKSPACE_ID)).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        return new WorkspaceAiUsageService(workspaceRepository, repository, new WorkspaceAiUsageAggregator(), new AiUsageAnalyticsMapper());
    }

    private GeneratedVersionQualityService qualityService() {
        GeneratedVersionRepository versionRepository = mock(GeneratedVersionRepository.class);
        AiQualityScoreRepository scoreRepository = mock(AiQualityScoreRepository.class);
        when(versionRepository.findByIdAndWorkspaceIdAndDeletedFalse(GENERATED_VERSION_ID, WORKSPACE_ID))
                .thenReturn(Optional.of(generatedVersion()));
        when(scoreRepository.findByGeneratedVersionIdAndDeletedFalse(GENERATED_VERSION_ID)).thenReturn(Optional.empty());
        when(scoreRepository.save(any(AiQualityScore.class))).thenAnswer(invocation -> {
            AiQualityScore score = invocation.getArgument(0);
            ReflectionTestUtils.setField(score, "id", UUID.randomUUID());
            return score;
        });
        return new GeneratedVersionQualityService(
                versionRepository,
                scoreRepository,
                new QualityScoreCalculator(
                        new ProductPreservationQualityService(),
                        new BrandingQualityService(),
                        new BanglaTypographyQualityService(),
                        new CompositionQualityService()),
                mock(AiCostIntelligenceService.class));
    }

    private AiFailureLogConsumer failureConsumer(AiFailureLogRepository repository) {
        return new AiFailureLogConsumer(objectMapper(), repository, mock(AiFailureCacheService.class), mock(AiMonitoringEventProducer.class));
    }

    private AiRedisAccessSupport redisAccess(RedisCacheService redis) {
        return new AiRedisAccessSupport(redis, mock(RedisLockService.class), mock(RedisRateLimitService.class));
    }

    private ObjectMapper objectMapper() {
        return new ObjectMapper().findAndRegisterModules();
    }

    private AiAnalyticsMonitoringQueryService analyticsQueryService(CurrentUser currentUser) {
        CurrentUserContext currentUserContext = mock(CurrentUserContext.class);
        when(currentUserContext.requireCurrentUser()).thenReturn(currentUser);
        return new AiAnalyticsMonitoringQueryService(
                mock(AiProviderHealthService.class),
                mock(AiLayerAnalyticsRepository.class),
                mock(com.lebhas.ai.infrastructure.persistence.WorkspaceAiUsageRepository.class),
                mock(com.lebhas.ai.application.AiUsageAnalyticsMapper.class),
                mock(com.lebhas.ai.application.WorkspaceAiUsageQueryService.class),
                mock(GeneratedVersionQualityService.class),
                mock(AiFailureLogRepository.class),
                mock(DynamicRoutingOptimizationService.class),
                currentUserContext,
                mock(WorkspaceAuthorizationService.class));
    }

    private AiProviderHealthService providerHealthService(AiAnalyticsMonitoringQueryService service) {
        return (AiProviderHealthService) ReflectionTestUtils.getField(service, "providerHealthService");
    }

    private com.lebhas.ai.application.WorkspaceAiUsageQueryService workspaceUsageQueryService(AiAnalyticsMonitoringQueryService service) {
        return (com.lebhas.ai.application.WorkspaceAiUsageQueryService) ReflectionTestUtils.getField(service, "workspaceAiUsageQueryService");
    }

    private WorkspaceAuthorizationService workspaceAuthorizationService(AiAnalyticsMonitoringQueryService service) {
        return (WorkspaceAuthorizationService) ReflectionTestUtils.getField(service, "workspaceAuthorizationService");
    }

    private CostObservation successObservation() {
        return new CostObservation(PROVIDER_ID, LAYER_ID, "dynamic-model", new BigDecimal("0.25"), new BigDecimal("0.80"), new BigDecimal("500"), true, NOW);
    }

    private CostObservation failureObservation() {
        return new CostObservation(PROVIDER_ID, LAYER_ID, "dynamic-model", new BigDecimal("0.25"), new BigDecimal("0.20"), new BigDecimal("900"), false, NOW);
    }

    private AiLayerLifecycleEvent layerEvent(boolean success) {
        return new AiLayerLifecycleEvent(null, NOW, WORKSPACE_ID, CREATIVE_REQUEST_ID, GENERATED_VERSION_ID, UUID.randomUUID(), UUID.randomUUID(), LAYER_ID,
                CreativeLayerType.MODEL_GENERATION, PROVIDER_ID, null, 1, success ? "COMPLETED" : "FAILED", success ? null : "timeout",
                Map.of("modelName", "dynamic-model", "costUsd", "0.25", "qualityScore", "0.80", "latencyMs", "500", "failureType", "TIMEOUT", "failureReason", "timeout"));
    }

    private AiToolProvider provider(String code) {
        AiToolProvider provider = AiToolProvider.create(code, "Provider " + code, ProviderType.IMAGE_GENERATION, ProviderStatus.ACTIVE, true,
                List.of(CreativeLayerType.MODEL_GENERATION.name()), "dynamic.credentials", true, true, true,
                Map.of("estimatedCostUsd", "0.25"), Map.of("qualityScore", "0.80"), Map.of());
        ReflectionTestUtils.setField(provider, "id", PROVIDER_ID);
        return provider;
    }

    private AiLayerAnalytics layerAnalytics() {
        AiLayerAnalytics analytics = AiLayerAnalytics.create(LAYER_ID, PROVIDER_ID, "dynamic-model");
        ReflectionTestUtils.setField(analytics, "id", UUID.randomUUID());
        analytics.updateTotals(1, 1, 0, new BigDecimal("500"), new BigDecimal("0.25"), new BigDecimal("0.80"));
        return analytics;
    }

    private WorkspaceAiUsageView usageView() {
        return new WorkspaceAiUsageView(UUID.randomUUID(), WORKSPACE_ID, 1, 1, BigDecimal.ONE, new BigDecimal("0.25"), 0, new BigDecimal("500"), NOW, NOW);
    }

    private ProviderHealthSnapshot providerHealth() {
        ProviderMetricsSnapshot metrics = new ProviderMetricsSnapshot(UUID.randomUUID(), PROVIDER_ID, "DYNAMIC_PROVIDER", "Dynamic Provider", "dynamic-model", 1, 1, 0,
                new BigDecimal("500"), new BigDecimal("0.25"), new BigDecimal("0.80"), BigDecimal.ONE, null, NOW);
        return new ProviderHealthSnapshot(PROVIDER_ID, "DYNAMIC_PROVIDER", "Dynamic Provider", "HEALTHY", BigDecimal.ONE, BigDecimal.ONE, 1, 1, 0, null, NOW, List.of(metrics));
    }

    private QualityScoreInput qualityInput() {
        return new QualityScoreInput(WORKSPACE_ID, GENERATED_VERSION_ID, null, null, null,
                new BigDecimal("0.70"), new BigDecimal("0.90"), new BigDecimal("0.80"), new BigDecimal("0.60"), new BigDecimal("0.85"),
                new BigDecimal("0.80"), "measured", null, null, true);
    }

    private GeneratedVersionEntity generatedVersion() {
        GeneratedVersionEntity version = GeneratedVersionEntity.create(
                WORKSPACE_ID,
                CREATIVE_REQUEST_ID,
                UUID.randomUUID(),
                1,
                "Version",
                null,
                null,
                GenerationStatus.READY,
                ApprovalStatus.NOT_SUBMITTED,
                true,
                null,
                null,
                USER_ID,
                GeneratedVersionStatus.ACTIVE);
        ReflectionTestUtils.setField(version, "id", GENERATED_VERSION_ID);
        return version;
    }

    private CurrentUser currentUser(Role role, UUID workspaceId, Set<Permission> permissions) {
        return new CurrentUser(USER_ID, workspaceId, "device", "user@example.com", Set.of(role), permissions, "token", NOW.plus(Duration.ofHours(1)));
    }
}
