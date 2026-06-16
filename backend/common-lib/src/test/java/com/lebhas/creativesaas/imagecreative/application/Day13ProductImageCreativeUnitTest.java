package com.lebhas.creativesaas.imagecreative.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lebhas.ai.application.GeneratedVersionQualityService;
import com.lebhas.ai.application.MasterAiProviderToolRegistryService;
import com.lebhas.ai.application.dto.ResolvedProviderRouteView;
import com.lebhas.ai.domain.CreativeTool;
import com.lebhas.ai.domain.CreativeToolCategory;
import com.lebhas.ai.domain.ToolCreditCostPolicy;
import com.lebhas.ai.infrastructure.persistence.CreativeToolRepository;
import com.lebhas.ai.infrastructure.persistence.ToolCreditCostPolicyRepository;
import com.lebhas.creativesaas.asset.domain.AssetCategory;
import com.lebhas.creativesaas.asset.domain.AssetEntity;
import com.lebhas.creativesaas.asset.domain.AssetFileType;
import com.lebhas.creativesaas.asset.domain.AssetType;
import com.lebhas.creativesaas.asset.domain.StorageProvider;
import com.lebhas.creativesaas.asset.infrastructure.persistence.AssetRepository;
import com.lebhas.creativesaas.asset.storage.StorageService;
import com.lebhas.creativesaas.brand.domain.BrandEntity;
import com.lebhas.creativesaas.brand.domain.BrandLanguagePreference;
import com.lebhas.creativesaas.brand.infrastructure.persistence.BrandRepository;
import com.lebhas.creativesaas.campaign.domain.ProjectCampaignEntity;
import com.lebhas.creativesaas.campaign.infrastructure.persistence.ProjectCampaignRepository;
import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.creativerequest.domain.CreativeRequestEntity;
import com.lebhas.creativesaas.creativerequest.infrastructure.persistence.CreativeRequestRepository;
import com.lebhas.creativesaas.generatedversion.application.GeneratedVersionViewMapper;
import com.lebhas.creativesaas.generatedversion.domain.GeneratedVersionEntity;
import com.lebhas.creativesaas.generatedversion.infrastructure.persistence.GeneratedVersionRepository;
import com.lebhas.creativesaas.generation.application.CreativeCreditReservationService;
import com.lebhas.creativesaas.generation.application.dto.CreditFinalizeCommand;
import com.lebhas.creativesaas.generation.application.dto.CreditRefundCommand;
import com.lebhas.creativesaas.generation.application.dto.CreditReservationResult;
import com.lebhas.creativesaas.imagecreative.application.dto.ProductImageCreativeCommand;
import com.lebhas.creativesaas.imagecreative.application.dto.ProductImageCreativeRequest;
import com.lebhas.creativesaas.imagecreative.domain.ImageCreativeFormat;
import com.lebhas.creativesaas.imagecreative.domain.ImageCreativeGeneration;
import com.lebhas.creativesaas.imagecreative.domain.ImageCreativeGenerationStatus;
import com.lebhas.creativesaas.imagecreative.domain.ImageCreativeQualityMode;
import com.lebhas.creativesaas.imagecreative.infrastructure.persistence.ImageCreativeGenerationRepository;
import com.lebhas.creativesaas.pricing.application.WorkspacePlanContextService;
import com.lebhas.creativesaas.pricing.application.dto.PlanFeaturePolicyView;
import com.lebhas.creativesaas.pricing.application.dto.PricingPlanView;
import com.lebhas.creativesaas.pricing.application.dto.WorkspacePlanContextView;
import com.lebhas.creativesaas.product.domain.ProductServiceEntity;
import com.lebhas.creativesaas.product.infrastructure.persistence.ProductServiceRepository;
import com.lebhas.creativesaas.prompt.domain.PromptLanguage;
import com.lebhas.creativesaas.prompt.domain.PromptPlatform;
import com.lebhas.creativesaas.usage.domain.UsageBillingLog;
import com.lebhas.creativesaas.usage.infrastructure.persistence.UsageBillingLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
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

class Day13ProductImageCreativeUnitTest {

    private static final UUID WORKSPACE_ID = UUID.fromString("13000000-0000-0000-0000-000000000001");
    private static final UUID PROJECT_ID = UUID.fromString("13000000-0000-0000-0000-000000000002");
    private static final UUID BRAND_ID = UUID.fromString("13000000-0000-0000-0000-000000000003");
    private static final UUID PRODUCT_ID = UUID.fromString("13000000-0000-0000-0000-000000000004");
    private static final UUID PRODUCT_ASSET_ID = UUID.fromString("13000000-0000-0000-0000-000000000005");
    private static final UUID TOOL_ID = UUID.fromString("13000000-0000-0000-0000-000000000006");
    private static final UUID GENERATION_ID = UUID.fromString("13000000-0000-0000-0000-000000000007");
    private static final UUID CREATIVE_REQUEST_ID = UUID.fromString("13000000-0000-0000-0000-000000000008");
    private static final UUID GENERATED_VERSION_ID = UUID.fromString("13000000-0000-0000-0000-000000000009");
    private static final UUID GENERATED_ASSET_ID = UUID.fromString("13000000-0000-0000-0000-000000000010");
    private static final UUID RESERVATION_ID = UUID.fromString("13000000-0000-0000-0000-000000000011");
    private static final UUID PROVIDER_ID = UUID.fromString("13000000-0000-0000-0000-000000000012");
    private static final UUID MODEL_ID = UUID.fromString("13000000-0000-0000-0000-000000000013");

    private final ProjectCampaignRepository projectRepository = mock(ProjectCampaignRepository.class);
    private final BrandRepository brandRepository = mock(BrandRepository.class);
    private final ProductServiceRepository productRepository = mock(ProductServiceRepository.class);
    private final AssetRepository assetRepository = mock(AssetRepository.class);
    private final CreativeToolRepository toolRepository = mock(CreativeToolRepository.class);
    private final ToolCreditCostPolicyRepository costRepository = mock(ToolCreditCostPolicyRepository.class);
    private final WorkspacePlanContextService planContextService = mock(WorkspacePlanContextService.class);
    private final MasterAiProviderToolRegistryService registryService = mock(MasterAiProviderToolRegistryService.class);
    private final CreativeCreditReservationService creditService = mock(CreativeCreditReservationService.class);
    @SuppressWarnings("unchecked")
    private final ObjectProvider<ProductImageCreativeProvider> provider = mock(ObjectProvider.class);
    private final ImageCreativeGenerationRepository generationRepository = mock(ImageCreativeGenerationRepository.class);
    private final CreativeRequestRepository creativeRequestRepository = mock(CreativeRequestRepository.class);
    private final GeneratedVersionRepository generatedVersionRepository = mock(GeneratedVersionRepository.class);
    private final UsageBillingLogRepository usageRepository = mock(UsageBillingLogRepository.class);
    private final GeneratedVersionQualityService qualityService = mock(GeneratedVersionQualityService.class);
    private final StorageService storageService = mock(StorageService.class);

    private final ProductImageCreativeService service = new ProductImageCreativeService(
            projectRepository,
            brandRepository,
            productRepository,
            assetRepository,
            toolRepository,
            costRepository,
            planContextService,
            registryService,
            creditService,
            provider,
            generationRepository,
            creativeRequestRepository,
            generatedVersionRepository,
            new GeneratedVersionViewMapper(),
            usageRepository,
            qualityService,
            storageService,
            new ImageCreativeMapper(),
            new ObjectMapper());

    @BeforeEach
    void setUp() {
        when(projectRepository.findByIdAndWorkspaceIdAndDeletedFalse(PROJECT_ID, WORKSPACE_ID)).thenReturn(Optional.of(project()));
        when(brandRepository.findByIdAndWorkspaceIdAndDeletedFalse(BRAND_ID, WORKSPACE_ID)).thenReturn(Optional.of(brand(BrandLanguagePreference.ENGLISH)));
        when(productRepository.findByIdAndWorkspaceIdAndDeletedFalse(PRODUCT_ID, WORKSPACE_ID)).thenReturn(Optional.of(product()));
        when(productRepository.findAllByWorkspaceIdAndBrandIdAndDeletedFalseOrderByCreatedAtDesc(WORKSPACE_ID, BRAND_ID)).thenReturn(List.of(product()));
        when(assetRepository.findByIdAndWorkspaceIdAndDeletedFalse(PRODUCT_ASSET_ID, WORKSPACE_ID)).thenReturn(Optional.of(productAsset(PROJECT_ID)));
        when(toolRepository.findByToolCodeAndDeletedFalse(ProductImageCreativeService.TOOL_CODE)).thenReturn(Optional.of(tool(true)));
        when(costRepository.findFirstByToolIdAndEnabledTrueAndDeletedFalseOrderByUpdatedAtDesc(TOOL_ID)).thenReturn(Optional.of(cost(BigDecimal.valueOf(4))));
        when(planContextService.getWorkspacePlanContext(WORKSPACE_ID)).thenReturn(plan(Set.of(ProductImageCreativeService.TOOL_CODE), true, 3));
        when(registryService.resolveProvider(TOOL_ID, "BASIC")).thenReturn(new ResolvedProviderRouteView(null, TOOL_ID, "BASIC", PROVIDER_ID, MODEL_ID, false, "configured-route"));
        when(provider.getIfAvailable(any())).thenReturn(new DeterministicProductImageCreativeProvider());
        when(creditService.reserveCredits(eq(WORKSPACE_ID), eq(BigDecimal.valueOf(4)), eq("product_image_creative_generation"), any()))
                .thenReturn(new CreditReservationResult(RESERVATION_ID, WORKSPACE_ID, BigDecimal.valueOf(4), BigDecimal.TEN, BigDecimal.valueOf(4), BigDecimal.valueOf(6), "product_image_creative_generation", GENERATION_ID));
        when(storageService.provider()).thenReturn(StorageProvider.R2);
        when(storageService.storeGenerated(any(StorageService.GeneratedStorageUploadRequest.class)))
                .thenAnswer(invocation -> {
                    StorageService.GeneratedStorageUploadRequest request = invocation.getArgument(0);
                    String storageKey = "image-creatives/%s/%s/%s.png".formatted(
                            request.workspaceId(),
                            GENERATION_ID,
                            request.outputId());
                    return new StorageService.StoredObject(
                            request.outputId() + ".png",
                            null,
                            storageKey,
                            null,
                            null,
                            null);
                });
        when(generationRepository.save(any(ImageCreativeGeneration.class))).thenAnswer(invocation -> withId(invocation.getArgument(0), GENERATION_ID));
        when(creativeRequestRepository.save(any(CreativeRequestEntity.class))).thenAnswer(invocation -> withId(invocation.getArgument(0), CREATIVE_REQUEST_ID));
        when(assetRepository.saveAndFlush(any(AssetEntity.class))).thenAnswer(invocation -> withId(invocation.getArgument(0), GENERATED_ASSET_ID));
        when(assetRepository.save(any(AssetEntity.class))).thenAnswer(invocation -> withId(invocation.getArgument(0), GENERATED_ASSET_ID));
        when(generatedVersionRepository.save(any(GeneratedVersionEntity.class))).thenAnswer(invocation -> withId(invocation.getArgument(0), GENERATED_VERSION_ID));
        when(usageRepository.save(any(UsageBillingLog.class))).thenAnswer(invocation -> withId(invocation.getArgument(0), UUID.randomUUID()));
    }

    @Test
    void generateImageCreativeSucceedsWithValidHierarchyAndStoresGeneratedRecords() {
        var result = service.generate(command(request(PRODUCT_ASSET_ID, 1)));

        assertThat(result.generation().id()).isEqualTo(GENERATION_ID);
        assertThat(result.generation().status()).isEqualTo(ImageCreativeGenerationStatus.COMPLETED);
        assertThat(result.generatedVersions()).hasSize(1);
        assertThat(result.generatedVersions().getFirst().id()).isEqualTo(GENERATED_VERSION_ID);
        verify(generatedVersionRepository).save(any(GeneratedVersionEntity.class));
        verify(assetRepository).save(any(AssetEntity.class));
        verify(usageRepository).save(any(UsageBillingLog.class));
    }

    @Test
    void missingProductAssetIsBlockedWhenMasterPolicyRequiresIt() {
        when(toolRepository.findByToolCodeAndDeletedFalse(ProductImageCreativeService.TOOL_CODE)).thenReturn(Optional.of(tool(true)));

        assertThatThrownBy(() -> service.generate(command(request(null, 1))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Selected product image is not ready for generation");
    }

    @Test
    void crossProjectProductAssetIsBlocked() {
        when(assetRepository.findByIdAndWorkspaceIdAndDeletedFalse(PRODUCT_ASSET_ID, WORKSPACE_ID)).thenReturn(Optional.of(productAsset(UUID.randomUUID())));

        assertThatThrownBy(() -> service.generate(command(request(PRODUCT_ASSET_ID, 1))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Selected product image is not ready for generation");
    }

    @Test
    void workspaceLevelProductAssetCanGenerateWithoutProjectAssignment() {
        when(assetRepository.findByIdAndWorkspaceIdAndDeletedFalse(PRODUCT_ASSET_ID, WORKSPACE_ID)).thenReturn(Optional.of(productAsset(null)));

        var result = service.generate(command(request(PRODUCT_ASSET_ID, 1)));

        assertThat(result.generation().status()).isEqualTo(ImageCreativeGenerationStatus.COMPLETED);
        verify(generatedVersionRepository).save(any(GeneratedVersionEntity.class));
    }

    @Test
    void packageDisabledToolIsBlocked() {
        when(planContextService.getWorkspacePlanContext(WORKSPACE_ID)).thenReturn(plan(Set.of(), true, 3));

        assertThatThrownBy(() -> service.generate(command(request(PRODUCT_ASSET_ID, 1))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Package does not allow selected image creative tool");
    }

    @Test
    void versionLimitIsEnforcedFromPlanPolicy() {
        when(planContextService.getWorkspacePlanContext(WORKSPACE_ID)).thenReturn(plan(Set.of(ProductImageCreativeService.TOOL_CODE), true, 1));

        assertThatThrownBy(() -> service.generate(command(request(PRODUCT_ASSET_ID, 2))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("requestedVersionCount exceeds plan policy");
    }

    @Test
    void creditsAreReservedAndFinalizedUsingMasterCostPolicy() {
        service.generate(command(request(PRODUCT_ASSET_ID, 1)));

        verify(creditService).reserveCredits(eq(WORKSPACE_ID), eq(BigDecimal.valueOf(4)), eq("product_image_creative_generation"), any());
        verify(creditService).finalizeCredits(any(CreditFinalizeCommand.class));
    }

    @Test
    void failureRefundsReservedCredits() {
        ProductImageCreativeProvider failingProvider = mock(ProductImageCreativeProvider.class);
        when(failingProvider.generate(any(), eq(1), any())).thenThrow(new IllegalStateException("provider failed"));
        when(provider.getIfAvailable(any())).thenReturn(failingProvider);

        assertThatThrownBy(() -> service.generate(command(request(PRODUCT_ASSET_ID, 1))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("provider failed");

        verify(creditService).refundCredits(any(CreditRefundCommand.class));
        verify(generatedVersionRepository, never()).save(any(GeneratedVersionEntity.class));
    }

    @Test
    void generatedAssetMetadataUsesR2AndDoesNotUseLocalFilesystem() {
        service.generate(command(request(PRODUCT_ASSET_ID, 1)));

        ArgumentCaptor<AssetEntity> assetCaptor = ArgumentCaptor.forClass(AssetEntity.class);
        verify(assetRepository).save(assetCaptor.capture());
        AssetEntity generatedAsset = assetCaptor.getValue();
        assertThat(generatedAsset.getStorageProvider()).isEqualTo(StorageProvider.R2);
        assertThat(generatedAsset.getStorageKey()).contains("image-creatives", GENERATION_ID.toString());
        assertThat(generatedAsset.getStorageBucket()).isNull();
        assertThat(generatedAsset.getAssetType()).isEqualTo(AssetType.GENERATED_CREATIVE);
        assertThat(generatedAsset.getFileType()).isEqualTo(AssetFileType.IMAGE);
        assertThat(generatedAsset.getMetadataJson()).contains("r2ObjectKey");
    }

    @Test
    void historyIsPaginated() {
        ImageCreativeGeneration history = withId(ImageCreativeGeneration.requested(
                WORKSPACE_ID,
                PROJECT_ID,
                BRAND_ID,
                PRODUCT_ID,
                PRODUCT_ASSET_ID,
                ProductImageCreativeService.TOOL_CODE,
                ImageCreativeFormat.FACEBOOK_SQUARE,
                PromptPlatform.FACEBOOK,
                PromptLanguage.ENGLISH,
                ImageCreativeQualityMode.BASIC,
                1,
                BigDecimal.ONE,
                Map.of("sourcePrompt", "Eid launch")), GENERATION_ID);
        history.complete(List.of(GENERATED_VERSION_ID));
        when(generationRepository.findAllByWorkspaceIdAndProjectIdAndDeletedFalseOrderByCreatedAtDesc(WORKSPACE_ID, PROJECT_ID, PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(List.of(history), PageRequest.of(0, 10), 1));

        var page = service.history(WORKSPACE_ID, PROJECT_ID, PageRequest.of(0, 10));

        assertThat(page.totalItems()).isEqualTo(1);
        assertThat(page.items().getFirst().status()).isEqualTo(ImageCreativeGenerationStatus.COMPLETED);
    }

    @Test
    void deterministicProviderOutputIsStable() {
        ProductImageCreativeProvider provider = new DeterministicProductImageCreativeProvider();
        ProductImageCreativeContext context = new ProductImageCreativeContext(
                GENERATION_ID,
                ProductImageCreativeService.TOOL_CODE,
                project(),
                brand(BrandLanguagePreference.ENGLISH),
                product(),
                productAsset(PROJECT_ID),
                request(PRODUCT_ASSET_ID, 1));

        var first = provider.generate(context, 1, null).getFirst();
        var second = provider.generate(context, 1, null).getFirst();

        assertThat(first.objectKey()).isEqualTo(second.objectKey());
        assertThat(first.width()).isEqualTo(ImageCreativeFormat.FACEBOOK_SQUARE.width());
        assertThat(first.height()).isEqualTo(ImageCreativeFormat.FACEBOOK_SQUARE.height());
    }

    @Test
    void noHardcodedPackageProviderOrLocalFilesystemNamesAreIntroduced() throws Exception {
        Path sourcePath = Path.of("src/main/java/com/lebhas/creativesaas/imagecreative/application/ProductImageCreativeService.java");
        if (!Files.exists(sourcePath)) {
            sourcePath = Path.of("backend/common-lib/src/main/java/com/lebhas/creativesaas/imagecreative/application/ProductImageCreativeService.java");
        }
        String source = Files.readString(sourcePath);

        assertThat(source).doesNotContain("FREE", "BASIC_PLAN", "PREMIUM_PLAN", "ENTERPRISE");
        assertThat(source).doesNotContain("OPENAI", "STABILITY", "GEMINI", "MOCK");
        assertThat(source).doesNotContain("StorageProvider.LOCAL", "java.nio.file", "Files.", "Path.");
    }

    private ProductImageCreativeCommand command(ProductImageCreativeRequest request) {
        return new ProductImageCreativeCommand(WORKSPACE_ID, PROJECT_ID, request);
    }

    private ProductImageCreativeRequest request(UUID productAssetId, int versionCount) {
        return new ProductImageCreativeRequest(
                null,
                "Create an Eid catalog product image creative",
                productAssetId,
                null,
                ImageCreativeFormat.FACEBOOK_SQUARE,
                PromptPlatform.FACEBOOK,
                PromptLanguage.ENGLISH,
                ImageCreativeQualityMode.BASIC,
                versionCount,
                "Premium",
                "Studio",
                null,
                null,
                null,
                "Shop now",
                true,
                true);
    }

    private WorkspacePlanContextView plan(Set<String> toolCodes, boolean premiumAllowed, Integer maxVersions) {
        return new WorkspacePlanContextView(
                WORKSPACE_ID,
                null,
                new PricingPlanView(UUID.randomUUID(), "Configured", "CONFIGURED", null, null, null, "USD", false, true, 1, null, null),
                new PlanFeaturePolicyView(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        maxVersions,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        true,
                        true,
                        true,
                        true,
                        true,
                        true,
                        true,
                        premiumAllowed,
                        false,
                        false,
                        true,
                        true,
                        true,
                        toolCodes,
                        null,
                        null),
                false);
    }

    private ProjectCampaignEntity project() {
        ProjectCampaignEntity project = ProjectCampaignEntity.create(
                WORKSPACE_ID,
                BRAND_ID,
                PRODUCT_ID,
                UUID.randomUUID(),
                "Eid Campaign",
                null,
                "SALES",
                PromptPlatform.FACEBOOK.name(),
                "SEASONAL");
        return withId(project, PROJECT_ID);
    }

    private BrandEntity brand(BrandLanguagePreference languagePreference) {
        BrandEntity brand = BrandEntity.create(
                WORKSPACE_ID,
                UUID.randomUUID(),
                "Lebhas",
                "Fashion",
                "Apparel",
                "Men",
                "Premium",
                "Shop now",
                "#000000",
                "#ffffff",
                null,
                null,
                null,
                null,
                null,
                languagePreference);
        return withId(brand, BRAND_ID);
    }

    private ProductServiceEntity product() {
        ProductServiceEntity product = ProductServiceEntity.create(WORKSPACE_ID, BRAND_ID, "Panjabi", "Cotton panjabi", "Attire", "Men", "Comfort");
        return withId(product, PRODUCT_ID);
    }

    private AssetEntity productAsset(UUID projectId) {
        AssetEntity asset = AssetEntity.createUploading(
                WORKSPACE_ID,
                BRAND_ID,
                PRODUCT_ID,
                projectId,
                UUID.randomUUID(),
                null,
                AssetType.PRODUCT_IMAGE,
                AssetCategory.PRODUCT_IMAGE,
                "panjabi.png",
                "Panjabi",
                "Product image",
                Set.of("product"),
                null,
                null,
                StorageProvider.R2);
        asset.completeUpload("panjabi.png", AssetFileType.IMAGE, "image/png", "png", 1024L, StorageProvider.R2, null, "assets/product/panjabi.png", null, null, null, 1080, 1080, null);
        return withId(asset, PRODUCT_ASSET_ID);
    }

    private CreativeTool tool(boolean productAssetRequired) {
        CreativeTool tool = CreativeTool.create(
                ProductImageCreativeService.TOOL_CODE,
                ProductImageCreativeService.TOOL_CODE,
                CreativeToolCategory.PRODUCT_IMAGE_CREATIVE,
                true,
                null,
                Map.of("productAssetRequired", productAssetRequired));
        return withId(tool, TOOL_ID);
    }

    private ToolCreditCostPolicy cost(BigDecimal creditCost) {
        ToolCreditCostPolicy policy = ToolCreditCostPolicy.create(TOOL_ID, "configured", creditCost, true, null, null, Map.of());
        return withId(policy, UUID.randomUUID());
    }

    private <T> T withId(T entity, UUID id) {
        ReflectionTestUtils.setField(entity, "id", id);
        setFieldIfPresent(entity, "createdAt", Instant.parse("2026-06-01T00:00:00Z"));
        setFieldIfPresent(entity, "updatedAt", Instant.parse("2026-06-01T00:00:00Z"));
        return entity;
    }

    private void setFieldIfPresent(Object target, String fieldName, Object value) {
        try {
            ReflectionTestUtils.setField(target, fieldName, value);
        } catch (IllegalArgumentException ignored) {
            // Some legacy entities do not inherit BaseEntity audit columns.
        }
    }
}
