package com.lebhas.creativesaas.foundation;

import com.lebhas.ai.cache.ActivePipelineCacheEntry;
import com.lebhas.ai.cache.AiPipelineCacheService;
import com.lebhas.ai.cache.AiRoutingDecisionCacheService;
import com.lebhas.ai.domain.AiToolProvider;
import com.lebhas.ai.domain.CreativeLayerType;
import com.lebhas.ai.domain.CreativePipeline;
import com.lebhas.ai.domain.CreativePipelineLayer;
import com.lebhas.ai.domain.CreativePipelineStatus;
import com.lebhas.ai.domain.LayerRoutingPolicy;
import com.lebhas.ai.domain.LayerRoutingStrategy;
import com.lebhas.ai.domain.LayerToolMapping;
import com.lebhas.ai.domain.ProviderStatus;
import com.lebhas.ai.domain.ProviderType;
import com.lebhas.ai.event.AiCreativePipelineKafkaTopicNames;
import com.lebhas.ai.event.AiGenerationLifecycleEvent;
import com.lebhas.ai.event.AiLayerLifecycleEvent;
import com.lebhas.ai.infrastructure.persistence.LayerRoutingPolicyRepository;
import com.lebhas.ai.producer.AiCreativePipelineEventProducer;
import com.lebhas.creativesaas.approval.domain.ApprovalWorkflow;
import com.lebhas.creativesaas.approval.validation.ApprovalPlanValidationService;
import com.lebhas.creativesaas.approval.validation.ApprovalWorkflowValidationService;
import com.lebhas.creativesaas.brand.domain.BrandEntity;
import com.lebhas.creativesaas.brand.domain.BrandLanguagePreference;
import com.lebhas.creativesaas.campaign.domain.ProjectCampaignEntity;
import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.TenantIsolationException;
import com.lebhas.creativesaas.common.security.Permission;
import com.lebhas.creativesaas.common.security.Role;
import com.lebhas.creativesaas.common.security.context.CurrentUser;
import com.lebhas.creativesaas.creativerequest.application.CostAwareRoutingService;
import com.lebhas.creativesaas.creativerequest.application.CreativeHierarchyValidationService;
import com.lebhas.creativesaas.creativerequest.application.CreativePlanValidationService;
import com.lebhas.creativesaas.creativerequest.application.CreativeRequestPromptFlowService;
import com.lebhas.creativesaas.creativerequest.application.FallbackResolver;
import com.lebhas.creativesaas.creativerequest.application.LayerRoutingDecision;
import com.lebhas.creativesaas.creativerequest.application.LayerRoutingResolver;
import com.lebhas.creativesaas.creativerequest.application.LayerToolCandidate;
import com.lebhas.creativesaas.creativerequest.application.LayerToolResolver;
import com.lebhas.creativesaas.creativerequest.application.PipelineResolutionContext;
import com.lebhas.creativesaas.creativerequest.application.QualityAwareRoutingService;
import com.lebhas.creativesaas.creativerequest.application.dto.CreateCreativeRequestCommand;
import com.lebhas.creativesaas.creativerequest.domain.CreativeRequestEntity;
import com.lebhas.creativesaas.creativerequest.infrastructure.persistence.CreativeRequestRepository;
import com.lebhas.creativesaas.generatedversion.application.GeneratedVersionService;
import com.lebhas.creativesaas.generatedversion.domain.GeneratedVersionEntity;
import com.lebhas.creativesaas.generatedversion.domain.GeneratedVersionStatus;
import com.lebhas.creativesaas.generatedversion.domain.GenerationStatus;
import com.lebhas.creativesaas.generatedversion.infrastructure.persistence.GeneratedVersionRepository;
import com.lebhas.creativesaas.generation.application.CreditReservationService;
import com.lebhas.creativesaas.generation.application.dto.CreditReservationResult;
import com.lebhas.creativesaas.generation.cache.GenerationLockService;
import com.lebhas.creativesaas.generation.cache.GeneratedVersionCountCacheService;
import com.lebhas.creativesaas.generation.event.GenerationEventProducer;
import com.lebhas.creativesaas.identity.application.WorkspaceAuthorizationService;
import com.lebhas.creativesaas.messaging.kafka.DomainEventPublisher;
import com.lebhas.creativesaas.messaging.kafka.KafkaTopicConstants;
import com.lebhas.creativesaas.pricing.application.WorkspacePlanContextService;
import com.lebhas.creativesaas.pricing.application.dto.PlanFeaturePolicyView;
import com.lebhas.creativesaas.pricing.application.dto.PricingPlanView;
import com.lebhas.creativesaas.pricing.application.dto.WorkspacePlanContextView;
import com.lebhas.creativesaas.pricing.application.dto.WorkspaceSubscriptionView;
import com.lebhas.creativesaas.product.domain.ProductServiceEntity;
import com.lebhas.creativesaas.prompt.cache.PromptEnhancementCacheService;
import com.lebhas.creativesaas.prompt.cache.dto.PromptEnhancementCacheEntry;
import com.lebhas.creativesaas.prompt.application.PromptJsonCodec;
import com.lebhas.creativesaas.prompt.domain.CampaignObjective;
import com.lebhas.creativesaas.prompt.domain.PromptLanguage;
import com.lebhas.creativesaas.prompt.domain.PromptPlatform;
import com.lebhas.creativesaas.redis.RedisLockService;
import com.lebhas.creativesaas.sharing.validation.ShareLinkValidationService;
import com.lebhas.creativesaas.workspace.domain.WorkspaceEntity;
import com.lebhas.creativesaas.workspace.domain.WorkspaceLanguage;
import com.lebhas.pricing.WorkspaceSubscriptionStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;

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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FinalRevisedDay1To6BackendUnitTest {

    private static final Instant NOW = Instant.parse("2026-05-22T00:00:00Z");
    private static final UUID WORKSPACE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID CREATIVE_REQUEST_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID GENERATED_VERSION_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

    @Test
    void dynamicPlansPersistAsDataShape() {
        PricingPlanView plan = pricingPlan("CUSTOM_AGENCY_PLAN", false, true);

        assertThat(plan.code()).isEqualTo("CUSTOM_AGENCY_PLAN");
        assertThat(plan.name()).isEqualTo("Custom plan");
        assertThat(plan.active()).isTrue();
    }

    @Test
    void defaultPlansAreSeedDataOnly() {
        PricingPlanView defaultPlan = pricingPlan("FREE", true, true);

        assertThat(defaultPlan.defaultPlan()).isTrue();
        assertThat(defaultPlan.code()).isEqualTo("FREE");
    }

    @Test
    void workspaceSubscriptionLoads() {
        WorkspacePlanContextView context = planContext(2, true, true, true);

        assertThat(context.subscription()).isNotNull();
        assertThat(context.subscription().workspaceId()).isEqualTo(WORKSPACE_ID);
        assertThat(context.pricingPlan().active()).isTrue();
    }

    @Test
    void planFeaturePolicyControlsLimits() {
        PlanFeaturePolicyView policy = featurePolicy(3, true, true);

        assertThat(policy.maxGeneratedVersionsPerRequest()).isEqualTo(3);
        assertThat(policy.allowApprovalWorkflow()).isTrue();
        assertThat(policy.allowPublicShareLinks()).isTrue();
    }

    @Test
    void noHardcodedPlanLogicBehavior() {
        CreativePlanValidationService service = new CreativePlanValidationService(planContextService(planContext(1, true, true, true)), domainEventProvider());

        assertThatThrownBy(() -> service.validateForCreativeRequest(WORKSPACE_ID, 2, null, BigDecimal.ONE))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Requested versions exceed");
    }

    @Test
    void providerCanBeEnabledAndDisabled() {
        AiToolProvider provider = provider("ANY_DYNAMIC_PROVIDER", true, CreativeLayerType.MODEL_GENERATION);

        provider.disable();
        assertThat(provider.isEnabled()).isFalse();
        assertThat(provider.getStatus()).isEqualTo(ProviderStatus.DISABLED);
        provider.enable();
        assertThat(provider.isEnabled()).isTrue();
        assertThat(provider.getStatus()).isEqualTo(ProviderStatus.ACTIVE);
    }

    @Test
    void providerSupportsLayers() {
        AiToolProvider provider = provider("CUSTOM_PROVIDER", true, CreativeLayerType.PROMPT_ENGINEERING);

        assertThat(provider.getSupportedLayers()).contains(CreativeLayerType.PROMPT_ENGINEERING.name());
    }

    @Test
    void noHardcodedProviderRouting() {
        AiToolProvider provider = provider("ARBITRARY_VENDOR", true, CreativeLayerType.MODEL_GENERATION);

        assertThat(provider.getProviderCode()).isEqualTo("ARBITRARY_VENDOR");
        assertThat(provider.getCredentialConfigKey()).isEqualTo("ai.providers.dynamic.credentials");
    }

    @Test
    void pipelineLayersAreOrdered() {
        CreativePipelineLayer first = layer(CreativeLayerType.INPUT_UNDERSTANDING, 1);
        CreativePipelineLayer second = layer(CreativeLayerType.MODEL_GENERATION, 2);

        assertThat(List.of(second, first).stream().sorted(java.util.Comparator.comparing(CreativePipelineLayer::getSortOrder)).toList())
                .containsExactly(first, second);
    }

    @Test
    void layerToolMappingWorks() {
        UUID layerId = UUID.randomUUID();
        UUID providerId = UUID.randomUUID();
        LayerToolMapping mapping = mapping(layerId, providerId, 1, true);

        assertThat(mapping.getPipelineLayerId()).isEqualTo(layerId);
        assertThat(mapping.getProviderId()).isEqualTo(providerId);
        assertThat(mapping.isEnabled()).isTrue();
    }

    @Test
    void routingPolicyResolvesDynamically() {
        CreativePipelineLayer layer = persistedLayer(CreativeLayerType.MODEL_GENERATION, 1);
        LayerRoutingPolicy policy = LayerRoutingPolicy.create(layer.getId(), "QUALITY", LayerRoutingStrategy.QUALITY_OPTIMIZED, 1, true, Map.of(), Map.of());
        LayerRoutingPolicyRepository routingRepository = mock(LayerRoutingPolicyRepository.class);
        LayerToolResolver toolResolver = mock(LayerToolResolver.class);
        QualityAwareRoutingService qualityService = mock(QualityAwareRoutingService.class);
        CostAwareRoutingService costService = mock(CostAwareRoutingService.class);
        AiRoutingDecisionCacheService routingCache = mock(AiRoutingDecisionCacheService.class);
        LayerToolCandidate low = candidate(1, "LOW", new BigDecimal("1.00"), new BigDecimal("0.40"));
        LayerToolCandidate high = candidate(2, "HIGH", new BigDecimal("3.00"), new BigDecimal("0.90"));
        when(routingRepository.findAllByPipelineLayerIdAndDeletedFalseOrderByPriorityOrderAsc(layer.getId())).thenReturn(List.of(policy));
        when(toolResolver.resolveCandidates(layer)).thenReturn(List.of(low, high));
        when(costService.activePolicy(layer.getId())).thenReturn(Optional.empty());
        when(qualityService.activePolicy(layer.getId())).thenReturn(Optional.empty());
        when(qualityService.chooseHighestQuality(any(), any())).thenReturn(Optional.of(high));
        when(costService.estimateLayerCost(high, Optional.empty())).thenReturn(high.estimatedCost());
        when(qualityService.estimateQualityScore(high)).thenReturn(high.qualityScore());

        LayerRoutingDecision decision = new LayerRoutingResolver(routingRepository, toolResolver, costService, qualityService, routingCache)
                .resolve(resolutionContext(), layer, creativeRequest());

        assertThat(decision.candidate()).isEqualTo(high);
        assertThat(decision.metadata()).containsEntry("strategy", "QUALITY_OPTIMIZED");
        verify(routingCache).store(any());
    }

    @Test
    void fallbackPolicyWorksAsFoundation() {
        LayerToolCandidate first = candidate(1, "FIRST", BigDecimal.ONE, BigDecimal.ONE);
        LayerToolCandidate fallback = candidate(2, "FALLBACK", BigDecimal.TEN, BigDecimal.TEN);

        assertThat(new FallbackResolver().resolveFallback(List.of(first, fallback), Set.of(first.provider().getId())))
                .contains(fallback);
    }

    @Test
    void hierarchyValidationWorks() {
        CreativeHierarchyValidationService service = mock(CreativeHierarchyValidationService.class);
        CreateCreativeRequestCommand command = createCommand(1, BrandLanguagePreference.ENGLISH);
        when(service.validate(eq(WORKSPACE_ID), eq(command))).thenReturn(mock(CreativeHierarchyValidationService.CreativeHierarchyContext.class));

        assertThat(service.validate(WORKSPACE_ID, command)).isNotNull();
    }

    @Test
    void subscriptionInactiveBlocksRequest() {
        CreativePlanValidationService service = new CreativePlanValidationService(planContextService(planContext(5, true, true, false)), domainEventProvider());

        assertThatThrownBy(() -> service.validateForCreativeRequest(WORKSPACE_ID, 1, null, BigDecimal.ONE))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("active workspace subscription");
    }

    @Test
    void requestedVersionsExceedingPlanLimitBlocked() {
        CreativePlanValidationService service = new CreativePlanValidationService(planContextService(planContext(1, true, true, true)), domainEventProvider());

        assertThatThrownBy(() -> service.validateForCreativeRequest(WORKSPACE_ID, 2, null, BigDecimal.ONE))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void brandLanguagePreferenceApplied() {
        BrandEntity brand = brand(BrandLanguagePreference.BANGLA);

        assertThat(brand.getLanguagePreference()).isEqualTo(BrandLanguagePreference.BANGLA);
    }

    @Test
    void promptCacheWorks() {
        PromptEnhancementCacheService cache = mock(PromptEnhancementCacheService.class);
        CreativeRequestPromptFlowService service = new CreativeRequestPromptFlowService(
                cache,
                null,
                new PromptJsonCodec(new ObjectMapper()),
                null
        );
        when(cache.sha256(any())).thenReturn("hash");
        when(cache.get("hash")).thenReturn(Optional.of(new PromptEnhancementCacheEntry("hash", "cached prompt", "cached", List.of(), null, null, null, NOW)));

        String prompt = service.resolveEnhancedPrompt(
                WORKSPACE_ID,
                brand(BrandLanguagePreference.ENGLISH),
                productService(),
                campaign(),
                "source",
                PromptLanguage.ENGLISH,
                PromptPlatform.FACEBOOK,
                CampaignObjective.AWARENESS,
                List.of(),
                null);

        assertThat(prompt).isEqualTo("cached prompt");
    }

    @Test
    void generationLockPreventsDuplicateProcessing() {
        GenerationLockService lockService = mock(GenerationLockService.class);
        when(lockService.acquire(WORKSPACE_ID, CREATIVE_REQUEST_ID)).thenReturn(Optional.empty());

        assertThat(lockService.acquire(WORKSPACE_ID, CREATIVE_REQUEST_ID)).isEmpty();
    }

    @Test
    void creditsReservedBeforeGeneration() {
        CreditReservationService service = mock(CreditReservationService.class);
        CreditReservationResult result = reservation();
        when(service.reserve(eq(WORKSPACE_ID), eq(CREATIVE_REQUEST_ID), eq(GENERATED_VERSION_ID), any(), any(), any())).thenReturn(result);

        assertThat(service.reserve(WORKSPACE_ID, CREATIVE_REQUEST_ID, GENERATED_VERSION_ID, BigDecimal.ONE, "GENERATION", CREATIVE_REQUEST_ID))
                .isEqualTo(result);
    }

    @Test
    void creditsFinalizedOnSuccess() {
        CreditReservationService service = mock(CreditReservationService.class);

        service.finalize(WORKSPACE_ID, CREATIVE_REQUEST_ID, GENERATED_VERSION_ID, UUID.randomUUID(), "GENERATION", CREATIVE_REQUEST_ID, BigDecimal.ONE, "success");

        verify(service).finalize(eq(WORKSPACE_ID), eq(CREATIVE_REQUEST_ID), eq(GENERATED_VERSION_ID), any(), eq("GENERATION"), eq(CREATIVE_REQUEST_ID), eq(BigDecimal.ONE), eq("success"));
    }

    @Test
    void creditsRefundedOnFailure() {
        CreditReservationService service = mock(CreditReservationService.class);

        service.refund(WORKSPACE_ID, CREATIVE_REQUEST_ID, GENERATED_VERSION_ID, UUID.randomUUID(), "GENERATION", CREATIVE_REQUEST_ID, BigDecimal.ONE, "failed");

        verify(service).refund(eq(WORKSPACE_ID), eq(CREATIVE_REQUEST_ID), eq(GENERATED_VERSION_ID), any(), eq("GENERATION"), eq(CREATIVE_REQUEST_ID), eq(BigDecimal.ONE), eq("failed"));
    }

    @Test
    void generatedVersionCountRespectsPlanLimit() {
        GeneratedVersionRepository repository = mock(GeneratedVersionRepository.class);
        when(repository.countByWorkspaceIdAndCreativeRequestIdAndDeletedFalse(WORKSPACE_ID, CREATIVE_REQUEST_ID)).thenReturn(1L);
        GeneratedVersionService service = new GeneratedVersionService(
                repository,
                planContextService(planContext(1, true, true, true)),
                mock(GeneratedVersionCountCacheService.class),
                mock(GenerationEventProducer.class));

        assertThatThrownBy(() -> service.validateVersionCapacity(WORKSPACE_ID, CREATIVE_REQUEST_ID, 1))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void approvalBlockedWhenPlanDisallows() {
        ApprovalPlanValidationService service = new ApprovalPlanValidationService(planContextService(planContext(5, false, true, true)));

        assertThatThrownBy(() -> service.requireApprovalWorkflowEnabled(WORKSPACE_ID))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void shareLinkBlockedWhenPlanDisallows() {
        ShareLinkValidationService service = new ShareLinkValidationService(mock(), mock(), planContextService(planContext(5, true, false, true)), mock());

        assertThatThrownBy(() -> service.requirePublicShareLinksEnabled(WORKSPACE_ID))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void approvalShareWorkspaceIsolationWorks() {
        ApprovalWorkflowValidationService service = new ApprovalWorkflowValidationService(mock(), mock(CreativeRequestRepository.class), mock(), mock());

        assertThatThrownBy(() -> service.requireGeneratedVersionMatchesCreativeRequest(generatedVersion(), UUID.randomUUID()))
                .isInstanceOf(TenantIsolationException.class);
    }

    @Test
    void pipelineCacheWorks() {
        AiPipelineCacheService cacheService = mock(AiPipelineCacheService.class);
        ActivePipelineCacheEntry entry = new ActivePipelineCacheEntry(UUID.randomUUID(), "DYNAMIC", 1, NOW);
        when(cacheService.storeActivePipeline(entry)).thenReturn(true);
        when(cacheService.getActivePipeline()).thenReturn(Optional.of(entry));

        assertThat(cacheService.storeActivePipeline(entry)).isTrue();
        assertThat(cacheService.getActivePipeline()).contains(entry);
    }

    @Test
    void routingCacheInvalidates() {
        AiRoutingDecisionCacheService cacheService = mock(AiRoutingDecisionCacheService.class);
        when(cacheService.invalidate(WORKSPACE_ID, CreativeLayerType.MODEL_GENERATION)).thenReturn(true);

        assertThat(cacheService.invalidate(WORKSPACE_ID, CreativeLayerType.MODEL_GENERATION)).isTrue();
    }

    @Test
    void kafkaGenerationEventsPublished() {
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, Object> kafkaTemplate = mock(KafkaTemplate.class);
        AiCreativePipelineEventProducer producer = new AiCreativePipelineEventProducer(kafkaTemplate, new AiCreativePipelineKafkaTopicNames(""));
        AiGenerationLifecycleEvent event = generationEvent("STARTED");

        producer.publishGenerationStarted(event);
        producer.publishGenerationCompleted(event);

        verify(kafkaTemplate).send(KafkaTopicConstants.AI_GENERATION_STARTED, CREATIVE_REQUEST_ID.toString(), event);
        verify(kafkaTemplate).send(KafkaTopicConstants.AI_GENERATION_COMPLETED, CREATIVE_REQUEST_ID.toString(), event);
    }

    @Test
    void kafkaLayerEventsPublished() {
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, Object> kafkaTemplate = mock(KafkaTemplate.class);
        AiCreativePipelineEventProducer producer = new AiCreativePipelineEventProducer(kafkaTemplate, new AiCreativePipelineKafkaTopicNames(""));
        AiLayerLifecycleEvent event = new AiLayerLifecycleEvent(
                null,
                null,
                WORKSPACE_ID,
                CREATIVE_REQUEST_ID,
                GENERATED_VERSION_ID,
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                CreativeLayerType.MODEL_GENERATION,
                UUID.randomUUID(),
                null,
                1,
                "COMPLETED",
                null,
                Map.of());

        producer.publishLayerStarted(event);
        producer.publishLayerCompleted(event);

        verify(kafkaTemplate).send(KafkaTopicConstants.AI_LAYER_STARTED, CREATIVE_REQUEST_ID.toString(), event);
        verify(kafkaTemplate).send(KafkaTopicConstants.AI_LAYER_COMPLETED, CREATIVE_REQUEST_ID.toString(), event);
    }

    @Test
    void masterCanManagePipeline() {
        CurrentUser master = currentUser(Role.MASTER, Set.of(Permission.SUPPORT_WORKSPACE_ACCESS));

        assertThat(master.isMaster()).isTrue();
    }

    @Test
    void adminCannotManageGlobalPipeline() {
        CurrentUser admin = currentUser(Role.ADMIN, Set.of(Permission.WORKSPACE_UPDATE));

        assertThat(admin.isMaster()).isFalse();
    }

    @Test
    void workspaceIsolationEnforced() {
        WorkspaceAuthorizationService authorizationService = mock(WorkspaceAuthorizationService.class);
        when(authorizationService.requireWorkspaceContext(WORKSPACE_ID)).thenThrow(new TenantIsolationException("workspace denied"));

        assertThatThrownBy(() -> authorizationService.requireWorkspaceContext(WORKSPACE_ID))
                .isInstanceOf(TenantIsolationException.class);
    }

    private WorkspacePlanContextService planContextService(WorkspacePlanContextView context) {
        WorkspacePlanContextService service = mock(WorkspacePlanContextService.class);
        when(service.getWorkspacePlanContext(WORKSPACE_ID)).thenReturn(context);
        return service;
    }

    @SuppressWarnings("unchecked")
    private ObjectProvider<DomainEventPublisher> domainEventProvider() {
        ObjectProvider<DomainEventPublisher> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(null);
        return provider;
    }

    private WorkspacePlanContextView planContext(int maxVersions, boolean approval, boolean share, boolean activeSubscription) {
        UUID pricingPlanId = UUID.randomUUID();
        return new WorkspacePlanContextView(
                WORKSPACE_ID,
                activeSubscription ? new WorkspaceSubscriptionView(UUID.randomUUID(), WORKSPACE_ID, pricingPlanId, WorkspaceSubscriptionStatus.ACTIVE, NOW.minus(Duration.ofDays(1)), NOW.plus(Duration.ofDays(30)), null, true, NOW, NOW) : null,
                pricingPlan("DYNAMIC_CUSTOM", false, true),
                featurePolicy(maxVersions, approval, share),
                false);
    }

    private PricingPlanView pricingPlan(String code, boolean defaultPlan, boolean active) {
        return new PricingPlanView(UUID.randomUUID(), "Custom plan", code, "Dynamic plan", BigDecimal.ONE, BigDecimal.TEN, "USD", defaultPlan, active, 1, NOW, NOW);
    }

    private PlanFeaturePolicyView featurePolicy(int maxVersions, boolean approval, boolean share) {
        return new PlanFeaturePolicyView(UUID.randomUUID(), UUID.randomUUID(), maxVersions, 5, 5, 5, 5, BigDecimal.TEN, BigDecimal.TEN, approval, share, true, true, true, true, NOW, NOW);
    }

    private AiToolProvider provider(String code, boolean enabled, CreativeLayerType layerType) {
        return AiToolProvider.create(code, "Provider " + code, ProviderType.IMAGE_GENERATION, enabled ? ProviderStatus.ACTIVE : ProviderStatus.DISABLED, enabled, List.of(layerType.name()), "ai.providers.dynamic.credentials", true, true, true, Map.of("estimatedCost", "1.25"), Map.of("qualityScore", "0.80"), Map.of("rpm", 60));
    }

    private CreativePipelineLayer layer(CreativeLayerType layerType, int sortOrder) {
        return CreativePipelineLayer.create(UUID.randomUUID(), layerType, layerType.name(), layerType.name(), sortOrder, true, true, true, Map.of());
    }

    private CreativePipelineLayer persistedLayer(CreativeLayerType layerType, int sortOrder) {
        CreativePipelineLayer layer = layer(layerType, sortOrder);
        ReflectionTestUtils.setField(layer, "id", UUID.randomUUID());
        return layer;
    }

    private LayerToolMapping mapping(UUID layerId, UUID providerId, int priorityOrder, boolean fallbackEligible) {
        return LayerToolMapping.create(layerId, providerId, null, null, "MAP_" + priorityOrder, priorityOrder, priorityOrder, true, fallbackEligible, Map.of());
    }

    private LayerToolCandidate candidate(int priority, String providerCode, BigDecimal cost, BigDecimal quality) {
        CreativePipelineLayer layer = persistedLayer(CreativeLayerType.MODEL_GENERATION, 1);
        AiToolProvider provider = provider(providerCode, true, CreativeLayerType.MODEL_GENERATION);
        ReflectionTestUtils.setField(provider, "id", UUID.randomUUID());
        return new LayerToolCandidate(mapping(layer.getId(), provider.getId(), priority, true), provider, cost, quality);
    }

    private PipelineResolutionContext resolutionContext() {
        CreativePipeline pipeline = CreativePipeline.create("DYNAMIC", "Dynamic", null, CreativePipelineStatus.ACTIVE, true, 1, Map.of());
        ReflectionTestUtils.setField(pipeline, "id", UUID.randomUUID());
        return new PipelineResolutionContext(planContext(5, true, true, true), pipeline, List.of());
    }

    private CreativeRequestEntity creativeRequest() {
        CreativeRequestEntity request = CreativeRequestEntity.create(WORKSPACE_ID, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), USER_ID, "Request", "Prompt", "Enhanced", BrandLanguagePreference.ENGLISH, PromptPlatform.FACEBOOK, null, CampaignObjective.AWARENESS, null, null, null, 1);
        ReflectionTestUtils.setField(request, "id", CREATIVE_REQUEST_ID);
        return request;
    }

    private CreateCreativeRequestCommand createCommand(int requestedVersions, BrandLanguagePreference languagePreference) {
        return new CreateCreativeRequestCommand(WORKSPACE_ID, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "Request", "Prompt", null, languagePreference, "AWARENESS", "FACEBOOK", "PNG", requestedVersions, List.of());
    }

    private BrandEntity brand(BrandLanguagePreference languagePreference) {
        BrandEntity brand = BrandEntity.create(WORKSPACE_ID, USER_ID, "Brand", "Industry", "Category", "Audience", "Voice", "CTA", "#000000", "#ffffff", null, null, null, null, null, languagePreference);
        ReflectionTestUtils.setField(brand, "id", UUID.randomUUID());
        return brand;
    }

    private ProductServiceEntity productService() {
        ProductServiceEntity productService = ProductServiceEntity.create(WORKSPACE_ID, UUID.randomUUID(), "Product", "Description", "SERVICE", "Audience", "USP");
        ReflectionTestUtils.setField(productService, "id", UUID.randomUUID());
        return productService;
    }

    private ProjectCampaignEntity campaign() {
        ProjectCampaignEntity campaign = ProjectCampaignEntity.create(WORKSPACE_ID, UUID.randomUUID(), UUID.randomUUID(), USER_ID, "Campaign", "Description", "AWARENESS", "FACEBOOK", "CAMPAIGN");
        ReflectionTestUtils.setField(campaign, "id", UUID.randomUUID());
        return campaign;
    }

    private CreditReservationResult reservation() {
        return new CreditReservationResult(UUID.randomUUID(), WORKSPACE_ID, BigDecimal.ONE, BigDecimal.TEN, BigDecimal.ONE, BigDecimal.TEN, "GENERATION", CREATIVE_REQUEST_ID);
    }

    private GeneratedVersionEntity generatedVersion() {
        GeneratedVersionEntity version = GeneratedVersionEntity.create(WORKSPACE_ID, CREATIVE_REQUEST_ID, UUID.randomUUID(), 1, "Version 1", null, null, GenerationStatus.READY, com.lebhas.creativesaas.generatedversion.domain.ApprovalStatus.NOT_SUBMITTED, true, null, null, USER_ID, GeneratedVersionStatus.ACTIVE);
        ReflectionTestUtils.setField(version, "id", GENERATED_VERSION_ID);
        return version;
    }

    private AiGenerationLifecycleEvent generationEvent(String status) {
        return new AiGenerationLifecycleEvent(null, null, WORKSPACE_ID, CREATIVE_REQUEST_ID, GENERATED_VERSION_ID, UUID.randomUUID(), UUID.randomUUID(), status, null, Map.of());
    }

    private CurrentUser currentUser(Role role, Set<Permission> permissions) {
        return new CurrentUser(USER_ID, WORKSPACE_ID, "device", "user@example.com", Set.of(role), permissions, "token", NOW.plus(Duration.ofHours(1)));
    }
}
