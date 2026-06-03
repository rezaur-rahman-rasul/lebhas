package com.lebhas.creativesaas.texttool.application;

import com.lebhas.ai.application.MasterAiProviderToolRegistryService;
import com.lebhas.ai.application.dto.ResolvedProviderRouteView;
import com.lebhas.ai.domain.CreativeTool;
import com.lebhas.ai.domain.CreativeToolCategory;
import com.lebhas.ai.domain.ToolCreditCostPolicy;
import com.lebhas.ai.infrastructure.persistence.CreativeToolRepository;
import com.lebhas.ai.infrastructure.persistence.ToolCreditCostPolicyRepository;
import com.lebhas.creativesaas.brand.domain.BrandEntity;
import com.lebhas.creativesaas.brand.domain.BrandLanguagePreference;
import com.lebhas.creativesaas.brand.infrastructure.persistence.BrandRepository;
import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.generation.application.CreativeCreditReservationService;
import com.lebhas.creativesaas.generation.application.dto.CreditFinalizeCommand;
import com.lebhas.creativesaas.generation.application.dto.CreditRefundCommand;
import com.lebhas.creativesaas.generation.application.dto.CreditReservationResult;
import com.lebhas.creativesaas.pricing.application.WorkspacePlanContextService;
import com.lebhas.creativesaas.pricing.application.dto.PlanFeaturePolicyView;
import com.lebhas.creativesaas.pricing.application.dto.PricingPlanView;
import com.lebhas.creativesaas.pricing.application.dto.WorkspacePlanContextView;
import com.lebhas.creativesaas.product.domain.ProductServiceEntity;
import com.lebhas.creativesaas.product.infrastructure.persistence.ProductServiceRepository;
import com.lebhas.creativesaas.project.domain.ProjectEntity;
import com.lebhas.creativesaas.project.infrastructure.persistence.ProjectRepository;
import com.lebhas.creativesaas.prompt.domain.CampaignObjective;
import com.lebhas.creativesaas.prompt.domain.PromptLanguage;
import com.lebhas.creativesaas.prompt.domain.PromptPlatform;
import com.lebhas.creativesaas.texttool.application.dto.CreativeTextToolCommand;
import com.lebhas.creativesaas.texttool.application.dto.CreativeTextToolRequest;
import com.lebhas.creativesaas.texttool.domain.CreativeTextQualityMode;
import com.lebhas.creativesaas.texttool.domain.CreativeTextToolHistory;
import com.lebhas.creativesaas.texttool.domain.CreativeTextToolOutput;
import com.lebhas.creativesaas.texttool.domain.CreativeTextToolStatus;
import com.lebhas.creativesaas.texttool.domain.CreativeTextToolType;
import com.lebhas.creativesaas.texttool.infrastructure.persistence.CreativeTextToolHistoryRepository;
import com.lebhas.creativesaas.texttool.infrastructure.persistence.CreativeTextToolOutputRepository;
import com.lebhas.creativesaas.usage.domain.UsageBillingLog;
import com.lebhas.creativesaas.usage.infrastructure.persistence.UsageBillingLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

class Day12CreativeTextToolUnitTest {

    private static final UUID WORKSPACE_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID PROJECT_ID = UUID.fromString("10000000-0000-0000-0000-000000000002");
    private static final UUID BRAND_ID = UUID.fromString("10000000-0000-0000-0000-000000000003");
    private static final UUID PRODUCT_ID = UUID.fromString("10000000-0000-0000-0000-000000000004");
    private static final UUID TOOL_ID = UUID.fromString("10000000-0000-0000-0000-000000000005");
    private static final UUID HISTORY_ID = UUID.fromString("10000000-0000-0000-0000-000000000006");
    private static final UUID OUTPUT_ID = UUID.fromString("10000000-0000-0000-0000-000000000007");
    private static final UUID RESERVATION_ID = UUID.fromString("10000000-0000-0000-0000-000000000008");
    private static final UUID PROVIDER_ID = UUID.fromString("10000000-0000-0000-0000-000000000009");

    private final ProjectRepository projectRepository = mock(ProjectRepository.class);
    private final BrandRepository brandRepository = mock(BrandRepository.class);
    private final ProductServiceRepository productRepository = mock(ProductServiceRepository.class);
    private final CreativeToolRepository toolRepository = mock(CreativeToolRepository.class);
    private final ToolCreditCostPolicyRepository costRepository = mock(ToolCreditCostPolicyRepository.class);
    private final WorkspacePlanContextService planContextService = mock(WorkspacePlanContextService.class);
    private final MasterAiProviderToolRegistryService registryService = mock(MasterAiProviderToolRegistryService.class);
    private final CreativeCreditReservationService creditService = mock(CreativeCreditReservationService.class);
    @SuppressWarnings("unchecked")
    private final ObjectProvider<CreativeTextToolProvider> provider = mock(ObjectProvider.class);
    private final CreativeTextToolOutputRepository outputRepository = mock(CreativeTextToolOutputRepository.class);
    private final CreativeTextToolHistoryRepository historyRepository = mock(CreativeTextToolHistoryRepository.class);
    private final UsageBillingLogRepository usageRepository = mock(UsageBillingLogRepository.class);
    private final CreativeTextToolService service = new CreativeTextToolService(
            projectRepository,
            brandRepository,
            productRepository,
            toolRepository,
            costRepository,
            planContextService,
            registryService,
            creditService,
            provider,
            outputRepository,
            historyRepository,
            usageRepository,
            new CreativeTextToolMapper());

    @BeforeEach
    void setUp() {
        when(projectRepository.findByIdAndWorkspaceIdAndDeletedFalse(PROJECT_ID, WORKSPACE_ID)).thenReturn(Optional.of(project(BRAND_ID)));
        when(brandRepository.findByIdAndWorkspaceIdAndDeletedFalse(BRAND_ID, WORKSPACE_ID)).thenReturn(Optional.of(brand(BrandLanguagePreference.ENGLISH)));
        when(productRepository.findByIdAndWorkspaceIdAndDeletedFalse(PRODUCT_ID, WORKSPACE_ID)).thenReturn(Optional.of(product(BRAND_ID)));
        when(toolRepository.findByToolCodeAndDeletedFalse(CreativeTextToolType.POST.toolCode())).thenReturn(Optional.of(tool(CreativeTextToolType.POST.toolCode())));
        when(costRepository.findFirstByToolIdAndEnabledTrueAndDeletedFalseOrderByUpdatedAtDesc(TOOL_ID)).thenReturn(Optional.of(cost(BigDecimal.valueOf(3))));
        when(planContextService.getWorkspacePlanContext(WORKSPACE_ID)).thenReturn(plan(Set.of(CreativeTextToolType.POST.toolCode()), true));
        when(registryService.resolveProvider(TOOL_ID, "BASIC")).thenReturn(new ResolvedProviderRouteView(null, TOOL_ID, "BASIC", PROVIDER_ID, null, false, "mock"));
        when(provider.getIfAvailable(any())).thenReturn(new DeterministicCreativeTextToolProvider());
        when(creditService.reserveCredits(eq(WORKSPACE_ID), eq(BigDecimal.valueOf(3)), eq("creative_text_tool_output"), any()))
                .thenReturn(new CreditReservationResult(RESERVATION_ID, WORKSPACE_ID, BigDecimal.valueOf(3), BigDecimal.TEN, BigDecimal.valueOf(3), BigDecimal.valueOf(7), "creative_text_tool_output", HISTORY_ID));
        when(historyRepository.save(any(CreativeTextToolHistory.class))).thenAnswer(invocation -> withId(invocation.getArgument(0), HISTORY_ID));
        when(outputRepository.save(any(CreativeTextToolOutput.class))).thenAnswer(invocation -> withId(invocation.getArgument(0), OUTPUT_ID));
        when(usageRepository.save(any(UsageBillingLog.class))).thenAnswer(invocation -> withId(invocation.getArgument(0), UUID.randomUUID()));
    }

    @Test
    void postGenerationValidatesHierarchyAndStoresDeterministicOutput() {
        var view = service.generate(command(CreativeTextToolType.POST, request(PRODUCT_ID, PromptLanguage.ENGLISH, CreativeTextQualityMode.BASIC)));

        assertThat(view.id()).isEqualTo(OUTPUT_ID);
        assertThat(view.creditCost()).isEqualByComparingTo("3.0000");
        assertThat(view.output()).containsKeys("postText", "shortHeadline", "cta");
        assertThat(view.output().get("postText").toString()).contains("Lebhas", "Panjabi");
        verify(outputRepository).save(any(CreativeTextToolOutput.class));
        verify(usageRepository).save(any(UsageBillingLog.class));
    }

    @Test
    void generationBlocksWhenProjectBrandHierarchyIsInvalid() {
        assertThatThrownBy(() -> service.generate(command(CreativeTextToolType.POST, new CreativeTextToolRequest(
                UUID.randomUUID(),
                PRODUCT_ID,
                PromptPlatform.FACEBOOK,
                PromptLanguage.ENGLISH,
                "Premium",
                CampaignObjective.SALES,
                "Eid launch",
                CreativeTextQualityMode.BASIC,
                List.of()))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Brand does not belong to the project");
    }

    @Test
    void captionIsBlockedIfPackageDoesNotEnableTool() {
        when(toolRepository.findByToolCodeAndDeletedFalse(CreativeTextToolType.CAPTION.toolCode())).thenReturn(Optional.of(tool(CreativeTextToolType.CAPTION.toolCode())));
        when(planContextService.getWorkspacePlanContext(WORKSPACE_ID)).thenReturn(plan(Set.of(CreativeTextToolType.POST.toolCode()), true));

        assertThatThrownBy(() -> service.generate(command(CreativeTextToolType.CAPTION, request(PRODUCT_ID, PromptLanguage.ENGLISH, CreativeTextQualityMode.BASIC))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Package does not allow selected text tool");
    }

    @Test
    void unsupportedBrandLanguageIsBlocked() {
        when(brandRepository.findByIdAndWorkspaceIdAndDeletedFalse(BRAND_ID, WORKSPACE_ID)).thenReturn(Optional.of(brand(BrandLanguagePreference.BANGLA)));

        assertThatThrownBy(() -> service.generate(command(CreativeTextToolType.POST, request(PRODUCT_ID, PromptLanguage.ENGLISH, CreativeTextQualityMode.BASIC))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Language does not match brand language preference");
    }

    @Test
    void creditsAreReservedAndFinalizedFromMasterPolicyCost() {
        service.generate(command(CreativeTextToolType.POST, request(PRODUCT_ID, PromptLanguage.ENGLISH, CreativeTextQualityMode.BASIC)));

        verify(creditService).reserveCredits(eq(WORKSPACE_ID), eq(BigDecimal.valueOf(3)), eq("creative_text_tool_output"), any());
        verify(creditService).finalizeCredits(any(CreditFinalizeCommand.class));
    }

    @Test
    void failureRefundsReservedCredits() {
        CreativeTextToolProvider failingProvider = mock(CreativeTextToolProvider.class);
        when(failingProvider.generate(any())).thenThrow(new IllegalStateException("provider failed"));
        when(provider.getIfAvailable(any())).thenReturn(failingProvider);

        assertThatThrownBy(() -> service.generate(command(CreativeTextToolType.POST, request(PRODUCT_ID, PromptLanguage.ENGLISH, CreativeTextQualityMode.BASIC))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("provider failed");

        verify(creditService).refundCredits(any(CreditRefundCommand.class));
        verify(outputRepository, never()).save(any(CreativeTextToolOutput.class));
    }

    @Test
    void historyIsPaginated() {
        CreativeTextToolHistory history = withId(CreativeTextToolHistory.requested(
                WORKSPACE_ID,
                PROJECT_ID,
                CreativeTextToolType.POST,
                CreativeTextToolType.POST.toolCode(),
                BigDecimal.ONE,
                Map.of("toolCode", CreativeTextToolType.POST.toolCode())), HISTORY_ID);
        history.complete(OUTPUT_ID, Map.of("postText", "done"));
        when(historyRepository.findAllByWorkspaceIdAndProjectIdAndDeletedFalseOrderByCreatedAtDesc(WORKSPACE_ID, PROJECT_ID, PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(List.of(history), PageRequest.of(0, 10), 1));

        var page = service.history(WORKSPACE_ID, PROJECT_ID, PageRequest.of(0, 10));

        assertThat(page.totalItems()).isEqualTo(1);
        assertThat(page.items().getFirst().status()).isEqualTo(CreativeTextToolStatus.COMPLETED);
    }

    @Test
    void noHardcodedPackageNamesAreIntroduced() throws Exception {
        Path sourcePath = Path.of("src/main/java/com/lebhas/creativesaas/texttool/application/CreativeTextToolService.java");
        if (!Files.exists(sourcePath)) {
            sourcePath = Path.of("backend/common-lib/src/main/java/com/lebhas/creativesaas/texttool/application/CreativeTextToolService.java");
        }
        String source = Files.readString(sourcePath);

        assertThat(source).doesNotContain("FREE", "BASIC_PLAN", "PREMIUM_PLAN", "ENTERPRISE");
    }

    private CreativeTextToolCommand command(CreativeTextToolType type, CreativeTextToolRequest request) {
        return new CreativeTextToolCommand(WORKSPACE_ID, PROJECT_ID, type, request);
    }

    private CreativeTextToolRequest request(UUID productId, PromptLanguage language, CreativeTextQualityMode qualityMode) {
        return new CreativeTextToolRequest(
                BRAND_ID,
                productId,
                PromptPlatform.FACEBOOK,
                language,
                "Premium",
                CampaignObjective.SALES,
                "Eid launch",
                qualityMode,
                List.of());
    }

    private WorkspacePlanContextView plan(Set<String> toolCodes, boolean premiumAllowed) {
        return new WorkspacePlanContextView(
                WORKSPACE_ID,
                null,
                new PricingPlanView(UUID.randomUUID(), "Configured", "CONFIGURED", null, null, null, "USD", false, true, 1, null, null),
                new PlanFeaturePolicyView(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
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

    private ProjectEntity project(UUID brandId) {
        ProjectEntity project = ProjectEntity.create(WORKSPACE_ID, brandId, "Eid Campaign", null, CampaignObjective.SALES, PromptPlatform.FACEBOOK);
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

    private ProductServiceEntity product(UUID brandId) {
        ProductServiceEntity product = ProductServiceEntity.create(WORKSPACE_ID, brandId, "Panjabi", "Cotton panjabi", "Attire", "Men", "Comfort");
        return withId(product, PRODUCT_ID);
    }

    private CreativeTool tool(String toolCode) {
        CreativeTool tool = CreativeTool.create(toolCode, toolCode, CreativeToolCategory.SOCIAL_POST, true, null, Map.of());
        return withId(tool, TOOL_ID);
    }

    private ToolCreditCostPolicy cost(BigDecimal creditCost) {
        ToolCreditCostPolicy policy = ToolCreditCostPolicy.create(TOOL_ID, "standard", creditCost, true, null, null, Map.of());
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
