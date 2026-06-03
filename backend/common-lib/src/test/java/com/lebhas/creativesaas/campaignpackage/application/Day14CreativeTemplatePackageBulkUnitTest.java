package com.lebhas.creativesaas.campaignpackage.application;

import com.lebhas.ai.domain.CreativeTool;
import com.lebhas.ai.domain.CreativeToolCategory;
import com.lebhas.ai.domain.ToolCreditCostPolicy;
import com.lebhas.ai.infrastructure.persistence.CreativeToolRepository;
import com.lebhas.ai.infrastructure.persistence.ToolCreditCostPolicyRepository;
import com.lebhas.creativesaas.campaignpackage.application.dto.BulkGenerationCommand;
import com.lebhas.creativesaas.campaignpackage.application.dto.CampaignPackageCommand;
import com.lebhas.creativesaas.campaignpackage.application.dto.CreativeTemplateCommand;
import com.lebhas.creativesaas.campaignpackage.domain.BulkGenerationItem;
import com.lebhas.creativesaas.campaignpackage.domain.BulkGenerationJob;
import com.lebhas.creativesaas.campaignpackage.domain.BulkGenerationJobStatus;
import com.lebhas.creativesaas.campaignpackage.domain.BulkGenerationType;
import com.lebhas.creativesaas.campaignpackage.domain.CampaignPackage;
import com.lebhas.creativesaas.campaignpackage.domain.CampaignPackageItem;
import com.lebhas.creativesaas.campaignpackage.domain.CampaignPackageItemType;
import com.lebhas.creativesaas.campaignpackage.domain.CreativeTemplate;
import com.lebhas.creativesaas.campaignpackage.domain.CreativeTemplateCategory;
import com.lebhas.creativesaas.campaignpackage.domain.CreativeTemplateStatus;
import com.lebhas.creativesaas.campaignpackage.infrastructure.persistence.BulkGenerationItemRepository;
import com.lebhas.creativesaas.campaignpackage.infrastructure.persistence.BulkGenerationJobRepository;
import com.lebhas.creativesaas.campaignpackage.infrastructure.persistence.CampaignPackageItemRepository;
import com.lebhas.creativesaas.campaignpackage.infrastructure.persistence.CampaignPackageRepository;
import com.lebhas.creativesaas.campaignpackage.infrastructure.persistence.CreativeTemplateRepository;
import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.generatedversion.domain.GeneratedVersionEntity;
import com.lebhas.creativesaas.generatedversion.domain.GeneratedVersionStatus;
import com.lebhas.creativesaas.generatedversion.domain.GenerationStatus;
import com.lebhas.creativesaas.generatedversion.infrastructure.persistence.GeneratedVersionRepository;
import com.lebhas.creativesaas.pricing.application.WorkspacePlanContextService;
import com.lebhas.creativesaas.pricing.application.dto.PlanFeaturePolicyView;
import com.lebhas.creativesaas.pricing.application.dto.PricingPlanView;
import com.lebhas.creativesaas.pricing.application.dto.WorkspacePlanContextView;
import com.lebhas.creativesaas.project.domain.ProjectEntity;
import com.lebhas.creativesaas.project.infrastructure.persistence.ProjectRepository;
import com.lebhas.creativesaas.prompt.domain.CampaignObjective;
import com.lebhas.creativesaas.prompt.domain.PromptLanguage;
import com.lebhas.creativesaas.prompt.domain.PromptPlatform;
import com.lebhas.creativesaas.redis.RedisLockService;
import com.lebhas.creativesaas.texttool.infrastructure.persistence.CreativeTextToolOutputRepository;
import com.lebhas.creativesaas.usage.domain.UsageBillingLog;
import com.lebhas.creativesaas.usage.infrastructure.persistence.UsageBillingLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class Day14CreativeTemplatePackageBulkUnitTest {

    private static final UUID WORKSPACE_ID = UUID.fromString("14000000-0000-0000-0000-000000000001");
    private static final UUID PROJECT_ID = UUID.fromString("14000000-0000-0000-0000-000000000002");
    private static final UUID BRAND_ID = UUID.fromString("14000000-0000-0000-0000-000000000003");
    private static final UUID TEMPLATE_ID = UUID.fromString("14000000-0000-0000-0000-000000000004");
    private static final UUID MASTER_TEMPLATE_ID = UUID.fromString("14000000-0000-0000-0000-000000000005");
    private static final UUID PACKAGE_ID = UUID.fromString("14000000-0000-0000-0000-000000000006");
    private static final UUID ITEM_ID = UUID.fromString("14000000-0000-0000-0000-000000000007");
    private static final UUID VERSION_ID = UUID.fromString("14000000-0000-0000-0000-000000000008");
    private static final UUID JOB_ID = UUID.fromString("14000000-0000-0000-0000-000000000009");
    private static final UUID TOOL_ID = UUID.fromString("14000000-0000-0000-0000-000000000010");

    private final CreativeTemplateRepository templateRepository = mock(CreativeTemplateRepository.class);
    private final CampaignPackageRepository packageRepository = mock(CampaignPackageRepository.class);
    private final CampaignPackageItemRepository packageItemRepository = mock(CampaignPackageItemRepository.class);
    private final BulkGenerationJobRepository bulkJobRepository = mock(BulkGenerationJobRepository.class);
    private final BulkGenerationItemRepository bulkItemRepository = mock(BulkGenerationItemRepository.class);
    private final ProjectRepository projectRepository = mock(ProjectRepository.class);
    private final GeneratedVersionRepository generatedVersionRepository = mock(GeneratedVersionRepository.class);
    private final CreativeTextToolOutputRepository textOutputRepository = mock(CreativeTextToolOutputRepository.class);
    private final CreativeToolRepository toolRepository = mock(CreativeToolRepository.class);
    private final ToolCreditCostPolicyRepository costPolicyRepository = mock(ToolCreditCostPolicyRepository.class);
    private final WorkspacePlanContextService planContextService = mock(WorkspacePlanContextService.class);
    private final UsageBillingLogRepository usageRepository = mock(UsageBillingLogRepository.class);
    @SuppressWarnings("unchecked")
    private final ObjectProvider<RedisLockService> redisLockService = mock(ObjectProvider.class);

    private final CreativeTemplateLibraryService service = new CreativeTemplateLibraryService(
            templateRepository,
            packageRepository,
            packageItemRepository,
            bulkJobRepository,
            bulkItemRepository,
            projectRepository,
            generatedVersionRepository,
            textOutputRepository,
            toolRepository,
            costPolicyRepository,
            planContextService,
            usageRepository,
            new CreativeTemplateMapper(),
            redisLockService);

    @BeforeEach
    void setUp() {
        when(projectRepository.findByIdAndWorkspaceIdAndDeletedFalse(PROJECT_ID, WORKSPACE_ID)).thenReturn(Optional.of(project()));
        when(templateRepository.save(any(CreativeTemplate.class))).thenAnswer(invocation -> withId(invocation.getArgument(0), TEMPLATE_ID));
        when(packageRepository.save(any(CampaignPackage.class))).thenAnswer(invocation -> withId(invocation.getArgument(0), PACKAGE_ID));
        when(packageItemRepository.save(any(CampaignPackageItem.class))).thenAnswer(invocation -> withId(invocation.getArgument(0), ITEM_ID));
        when(bulkJobRepository.save(any(BulkGenerationJob.class))).thenAnswer(invocation -> withId(invocation.getArgument(0), JOB_ID));
        when(bulkItemRepository.save(any(BulkGenerationItem.class))).thenAnswer(invocation -> withId(invocation.getArgument(0), UUID.randomUUID()));
        when(usageRepository.save(any(UsageBillingLog.class))).thenAnswer(invocation -> withId(invocation.getArgument(0), UUID.randomUUID()));
        when(toolRepository.findByToolCodeAndDeletedFalse(CreativeTemplateLibraryService.BULK_GENERATION_TOOL_CODE)).thenReturn(Optional.of(tool()));
        when(costPolicyRepository.findFirstByToolIdAndEnabledTrueAndDeletedFalseOrderByUpdatedAtDesc(TOOL_ID)).thenReturn(Optional.of(cost(BigDecimal.valueOf(2))));
        when(planContextService.getWorkspacePlanContext(WORKSPACE_ID)).thenReturn(plan(Set.of(CreativeTemplateLibraryService.BULK_GENERATION_TOOL_CODE)));
        when(redisLockService.getIfAvailable()).thenReturn(null);
    }

    @Test
    void workspaceTemplateCrudWorks() {
        var created = service.createWorkspaceTemplate(templateCommand(WORKSPACE_ID, false));
        CreativeTemplate existing = withId(CreativeTemplate.create(WORKSPACE_ID, "Sale", CreativeTemplateCategory.FLASH_SALE, null,
                PromptPlatform.FACEBOOK, PromptLanguage.ENGLISH, CampaignObjective.SALES, false, Map.of("headline", "Old"), CreativeTemplateStatus.ACTIVE), TEMPLATE_ID);
        when(templateRepository.findByIdAndDeletedFalse(TEMPLATE_ID)).thenReturn(Optional.of(existing));

        var updated = service.updateTemplate(WORKSPACE_ID, TEMPLATE_ID, new CreativeTemplateCommand(WORKSPACE_ID, "Updated", CreativeTemplateCategory.EID_OFFER,
                "desc", PromptPlatform.FACEBOOK, PromptLanguage.ENGLISH, CampaignObjective.SALES, Map.of("headline", "New"), CreativeTemplateStatus.ACTIVE));
        var fetched = service.getTemplate(WORKSPACE_ID, TEMPLATE_ID);

        assertThat(created.id()).isEqualTo(TEMPLATE_ID);
        assertThat(updated.name()).isEqualTo("Updated");
        assertThat(fetched.id()).isEqualTo(TEMPLATE_ID);
    }

    @Test
    void masterGlobalTemplateIsVisibleToWorkspace() {
        CreativeTemplate workspaceTemplate = withId(CreativeTemplate.create(WORKSPACE_ID, "Workspace", CreativeTemplateCategory.CUSTOM, null, null, null, null, false, Map.of(), CreativeTemplateStatus.ACTIVE), TEMPLATE_ID);
        CreativeTemplate masterTemplate = withId(CreativeTemplate.create(new UUID(0L, 0L), "Master", CreativeTemplateCategory.EID_OFFER, null, null, null, null, true, Map.of(), CreativeTemplateStatus.ACTIVE), MASTER_TEMPLATE_ID);
        when(templateRepository.findAllByWorkspaceIdAndDeletedFalseOrderByCreatedAtDesc(WORKSPACE_ID)).thenReturn(List.of(workspaceTemplate));
        when(templateRepository.findAllByMasterTemplateTrueAndActiveTrueAndDeletedFalseOrderByCreatedAtDesc()).thenReturn(List.of(masterTemplate));

        var views = service.listWorkspaceTemplates(WORKSPACE_ID);

        assertThat(views).extracting("id").containsExactly(TEMPLATE_ID, MASTER_TEMPLATE_ID);
    }

    @Test
    void templateApplyValidatesProjectHierarchy() {
        when(projectRepository.findByIdAndWorkspaceIdAndDeletedFalse(PROJECT_ID, WORKSPACE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.applyTemplate(WORKSPACE_ID, PROJECT_ID, TEMPLATE_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Project not found");
    }

    @Test
    void campaignPackageCreatedFromGeneratedVersions() {
        when(generatedVersionRepository.findByIdAndWorkspaceIdAndDeletedFalse(VERSION_ID, WORKSPACE_ID)).thenReturn(Optional.of(generatedVersion(PROJECT_ID)));

        var view = service.createCampaignPackage(new CampaignPackageCommand(WORKSPACE_ID, PROJECT_ID, "Eid Pack", "desc",
                List.of(new CampaignPackageCommand.CampaignPackageItemCommand(CampaignPackageItemType.GENERATED_VERSION, VERSION_ID))));

        assertThat(view.id()).isEqualTo(PACKAGE_ID);
        assertThat(view.items()).hasSize(1);
        verify(packageItemRepository).save(any(CampaignPackageItem.class));
    }

    @Test
    void exportUrlUsesR2Foundation() {
        CampaignPackage pack = withId(CampaignPackage.create(WORKSPACE_ID, PROJECT_ID, "Pack", null), PACKAGE_ID);
        when(packageRepository.findByIdAndWorkspaceIdAndDeletedFalse(PACKAGE_ID, WORKSPACE_ID)).thenReturn(Optional.of(pack));

        var export = service.exportUrl(WORKSPACE_ID, PACKAGE_ID);

        assertThat(export.r2ObjectKey()).startsWith("campaign-packages/");
        assertThat(export.signedUrl()).startsWith("r2-signed://");
        assertThat(export.signedUrl()).contains("export.zip");
    }

    @Test
    void bulkPreviewEstimatesCreditsFromMasterPolicy() {
        var preview = service.previewBulk(bulkCommand(List.of(UUID.randomUUID(), UUID.randomUUID())));

        assertThat(preview.unitCreditCost()).isEqualByComparingTo("2");
        assertThat(preview.estimatedCredits()).isEqualByComparingTo("4");
    }

    @Test
    void bulkQueueBlockedIfPackageDisabled() {
        when(planContextService.getWorkspacePlanContext(WORKSPACE_ID)).thenReturn(plan(Set.of()));

        assertThatThrownBy(() -> service.queueBulk(bulkCommand(List.of(UUID.randomUUID()))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Package does not allow bulk generation");
    }

    @Test
    void bulkQueueCreatesJob() {
        var view = service.queueBulk(bulkCommand(List.of(UUID.randomUUID())));

        assertThat(view.id()).isEqualTo(JOB_ID);
        assertThat(view.status()).isEqualTo(BulkGenerationJobStatus.QUEUED);
        verify(bulkJobRepository).save(any(BulkGenerationJob.class));
        verify(bulkItemRepository).save(any(BulkGenerationItem.class));
    }

    @Test
    void noLocalFilesystemUsageOrHardcodedPackageNamesAreIntroduced() throws Exception {
        Path sourcePath = Path.of("src/main/java/com/lebhas/creativesaas/campaignpackage/application/CreativeTemplateLibraryService.java");
        if (!Files.exists(sourcePath)) {
            sourcePath = Path.of("backend/common-lib/src/main/java/com/lebhas/creativesaas/campaignpackage/application/CreativeTemplateLibraryService.java");
        }
        String source = Files.readString(sourcePath);

        assertThat(source).doesNotContain("StorageProvider.LOCAL", "java.nio.file", "Files.", "Path.");
        assertThat(source).doesNotContain("FREE", "BASIC_PLAN", "PREMIUM_PLAN", "ENTERPRISE");
    }

    private CreativeTemplateCommand templateCommand(UUID workspaceId, boolean master) {
        return new CreativeTemplateCommand(workspaceId, master ? "Master Eid" : "Eid", CreativeTemplateCategory.EID_OFFER, "desc",
                PromptPlatform.FACEBOOK, PromptLanguage.ENGLISH, CampaignObjective.SALES, Map.of("headline", "Eid offer"), CreativeTemplateStatus.ACTIVE);
    }

    private BulkGenerationCommand bulkCommand(List<UUID> sourceIds) {
        return new BulkGenerationCommand(WORKSPACE_ID, PROJECT_ID, BulkGenerationType.IMAGE_CREATIVE, PromptPlatform.FACEBOOK, PromptLanguage.ENGLISH, sourceIds, Map.of("qualityMode", "BASIC"));
    }

    private ProjectEntity project() {
        return withId(ProjectEntity.create(WORKSPACE_ID, BRAND_ID, "Eid", null, CampaignObjective.SALES, PromptPlatform.FACEBOOK), PROJECT_ID);
    }

    private GeneratedVersionEntity generatedVersion(UUID projectId) {
        GeneratedVersionEntity version = GeneratedVersionEntity.create(WORKSPACE_ID, UUID.randomUUID(), projectId, 1, "v1",
                UUID.randomUUID(), UUID.randomUUID(), GenerationStatus.READY, null, true, null, null, UUID.randomUUID(), GeneratedVersionStatus.ACTIVE);
        return withId(version, VERSION_ID);
    }

    private CreativeTool tool() {
        return withId(CreativeTool.create(CreativeTemplateLibraryService.BULK_GENERATION_TOOL_CODE, "Bulk", CreativeToolCategory.PRODUCT_IMAGE_CREATIVE, true, null, Map.of()), TOOL_ID);
    }

    private ToolCreditCostPolicy cost(BigDecimal creditCost) {
        return withId(ToolCreditCostPolicy.create(TOOL_ID, "configured", creditCost, true, null, null, Map.of()), UUID.randomUUID());
    }

    private WorkspacePlanContextView plan(Set<String> toolCodes) {
        return new WorkspacePlanContextView(
                WORKSPACE_ID,
                null,
                new PricingPlanView(UUID.randomUUID(), "Configured", "CONFIGURED", null, null, null, "USD", false, true, 1, null, null),
                new PlanFeaturePolicyView(UUID.randomUUID(), UUID.randomUUID(), null, null, null, null, null, null, null, null, null, null,
                        null, true, true, true, true, true, true, true, true, false, false, true, true, true, toolCodes, null, null),
                false);
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
        }
    }
}
