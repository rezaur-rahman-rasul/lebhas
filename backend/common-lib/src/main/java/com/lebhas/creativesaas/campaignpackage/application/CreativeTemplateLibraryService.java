package com.lebhas.creativesaas.campaignpackage.application;

import com.lebhas.ai.domain.CreativeTool;
import com.lebhas.ai.domain.ToolCreditCostPolicy;
import com.lebhas.ai.infrastructure.persistence.CreativeToolRepository;
import com.lebhas.ai.infrastructure.persistence.ToolCreditCostPolicyRepository;
import com.lebhas.creativesaas.campaignpackage.application.dto.AppliedCreativeTemplateView;
import com.lebhas.creativesaas.campaignpackage.application.dto.BulkGenerationCommand;
import com.lebhas.creativesaas.campaignpackage.application.dto.BulkGenerationJobView;
import com.lebhas.creativesaas.campaignpackage.application.dto.BulkGenerationPreviewView;
import com.lebhas.creativesaas.campaignpackage.application.dto.CampaignPackageCommand;
import com.lebhas.creativesaas.campaignpackage.application.dto.CampaignPackageExportUrlView;
import com.lebhas.creativesaas.campaignpackage.application.dto.CampaignPackageView;
import com.lebhas.creativesaas.campaignpackage.application.dto.CreativeTemplateCommand;
import com.lebhas.creativesaas.campaignpackage.application.dto.CreativeTemplateView;
import com.lebhas.creativesaas.campaignpackage.domain.BulkGenerationItem;
import com.lebhas.creativesaas.campaignpackage.domain.BulkGenerationJob;
import com.lebhas.creativesaas.campaignpackage.domain.BulkGenerationType;
import com.lebhas.creativesaas.campaignpackage.domain.CampaignPackage;
import com.lebhas.creativesaas.campaignpackage.domain.CampaignPackageItem;
import com.lebhas.creativesaas.campaignpackage.domain.CampaignPackageItemType;
import com.lebhas.creativesaas.campaignpackage.domain.CreativeTemplate;
import com.lebhas.creativesaas.campaignpackage.domain.CreativeTemplateStatus;
import com.lebhas.creativesaas.campaignpackage.infrastructure.persistence.BulkGenerationItemRepository;
import com.lebhas.creativesaas.campaignpackage.infrastructure.persistence.BulkGenerationJobRepository;
import com.lebhas.creativesaas.campaignpackage.infrastructure.persistence.CampaignPackageItemRepository;
import com.lebhas.creativesaas.campaignpackage.infrastructure.persistence.CampaignPackageRepository;
import com.lebhas.creativesaas.campaignpackage.infrastructure.persistence.CreativeTemplateRepository;
import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.generatedversion.domain.GeneratedVersionEntity;
import com.lebhas.creativesaas.generatedversion.infrastructure.persistence.GeneratedVersionRepository;
import com.lebhas.creativesaas.imagecreative.application.ProductImageCreativeService;
import com.lebhas.creativesaas.messaging.kafka.BaseDomainEvent;
import com.lebhas.creativesaas.messaging.kafka.DomainEventPublisher;
import com.lebhas.creativesaas.messaging.kafka.KafkaTopicConstants;
import com.lebhas.creativesaas.pricing.application.WorkspacePlanContextService;
import com.lebhas.creativesaas.pricing.application.dto.PlanFeaturePolicyView;
import com.lebhas.creativesaas.project.domain.ProjectEntity;
import com.lebhas.creativesaas.project.infrastructure.persistence.ProjectRepository;
import com.lebhas.creativesaas.redis.RedisLockService;
import com.lebhas.creativesaas.texttool.domain.CreativeTextToolOutput;
import com.lebhas.creativesaas.texttool.domain.CreativeTextToolType;
import com.lebhas.creativesaas.texttool.infrastructure.persistence.CreativeTextToolOutputRepository;
import com.lebhas.creativesaas.usage.domain.UsageBillingLog;
import com.lebhas.creativesaas.usage.infrastructure.persistence.UsageBillingLogRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class CreativeTemplateLibraryService {

    public static final String BULK_GENERATION_TOOL_CODE = "BULK_GENERATION";
    private static final String REFERENCE_TYPE_BULK = "bulk_generation_job";
    private static final String REFERENCE_TYPE_EXPORT = "campaign_package_export";

    private final CreativeTemplateRepository templateRepository;
    private final CampaignPackageRepository packageRepository;
    private final CampaignPackageItemRepository packageItemRepository;
    private final BulkGenerationJobRepository bulkJobRepository;
    private final BulkGenerationItemRepository bulkItemRepository;
    private final ProjectRepository projectRepository;
    private final GeneratedVersionRepository generatedVersionRepository;
    private final CreativeTextToolOutputRepository textOutputRepository;
    private final CreativeToolRepository toolRepository;
    private final ToolCreditCostPolicyRepository costPolicyRepository;
    private final WorkspacePlanContextService planContextService;
    private final UsageBillingLogRepository usageRepository;
    private final CreativeTemplateMapper mapper;
    private final ObjectProvider<RedisLockService> redisLockService;
    private DomainEventPublisher domainEventPublisher;

    public CreativeTemplateLibraryService(
            CreativeTemplateRepository templateRepository,
            CampaignPackageRepository packageRepository,
            CampaignPackageItemRepository packageItemRepository,
            BulkGenerationJobRepository bulkJobRepository,
            BulkGenerationItemRepository bulkItemRepository,
            ProjectRepository projectRepository,
            GeneratedVersionRepository generatedVersionRepository,
            CreativeTextToolOutputRepository textOutputRepository,
            CreativeToolRepository toolRepository,
            ToolCreditCostPolicyRepository costPolicyRepository,
            WorkspacePlanContextService planContextService,
            UsageBillingLogRepository usageRepository,
            CreativeTemplateMapper mapper,
            ObjectProvider<RedisLockService> redisLockService
    ) {
        this.templateRepository = templateRepository;
        this.packageRepository = packageRepository;
        this.packageItemRepository = packageItemRepository;
        this.bulkJobRepository = bulkJobRepository;
        this.bulkItemRepository = bulkItemRepository;
        this.projectRepository = projectRepository;
        this.generatedVersionRepository = generatedVersionRepository;
        this.textOutputRepository = textOutputRepository;
        this.toolRepository = toolRepository;
        this.costPolicyRepository = costPolicyRepository;
        this.planContextService = planContextService;
        this.usageRepository = usageRepository;
        this.mapper = mapper;
        this.redisLockService = redisLockService;
    }

    @Autowired(required = false)
    void setDomainEventPublisher(DomainEventPublisher domainEventPublisher) {
        this.domainEventPublisher = domainEventPublisher;
    }

    @Transactional
    public CreativeTemplateView createWorkspaceTemplate(CreativeTemplateCommand command) {
        CreativeTemplate template = templateRepository.save(CreativeTemplate.create(command.workspaceId(), command.name(), command.category(),
                command.description(), command.platform(), command.language(), command.campaignObjective(), false,
                command.templatePayload(), command.status()));
        publish(KafkaTopicConstants.CREATIVE_TEMPLATE_CREATED, command.workspaceId(), template.getId(), Map.of("masterTemplate", false));
        return mapper.toView(template);
    }

    @Transactional
    public CreativeTemplateView createMasterTemplate(CreativeTemplateCommand command) {
        CreativeTemplate template = templateRepository.save(CreativeTemplate.create(command.workspaceId(), command.name(), command.category(),
                command.description(), command.platform(), command.language(), command.campaignObjective(), true,
                command.templatePayload(), command.status()));
        publish(KafkaTopicConstants.CREATIVE_TEMPLATE_CREATED, command.workspaceId(), template.getId(), Map.of("masterTemplate", true));
        return mapper.toView(template);
    }

    @Transactional(readOnly = true)
    public List<CreativeTemplateView> listWorkspaceTemplates(UUID workspaceId) {
        List<CreativeTemplate> templates = new ArrayList<>(templateRepository.findAllByWorkspaceIdAndDeletedFalseOrderByCreatedAtDesc(workspaceId));
        templates.addAll(templateRepository.findAllByMasterTemplateTrueAndActiveTrueAndDeletedFalseOrderByCreatedAtDesc());
        return templates.stream().map(mapper::toView).toList();
    }

    @Transactional(readOnly = true)
    public List<CreativeTemplateView> listMasterTemplates() {
        return templateRepository.findAllByMasterTemplateTrueAndActiveTrueAndDeletedFalseOrderByCreatedAtDesc()
                .stream().map(mapper::toView).toList();
    }

    @Transactional(readOnly = true)
    public CreativeTemplateView getTemplate(UUID workspaceId, UUID templateId) {
        return mapper.toView(requireTemplate(workspaceId, templateId));
    }

    @Transactional
    public CreativeTemplateView updateTemplate(UUID workspaceId, UUID templateId, CreativeTemplateCommand command) {
        CreativeTemplate template = requireTemplate(workspaceId, templateId);
        if (template.isMasterTemplate()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Master template must be updated through master API");
        }
        template.update(command.name(), command.category(), command.description(), command.platform(), command.language(),
                command.campaignObjective(), command.templatePayload(), command.status());
        return mapper.toView(templateRepository.save(template));
    }

    @Transactional
    public CreativeTemplateView updateMasterTemplate(UUID templateId, CreativeTemplateCommand command) {
        CreativeTemplate template = templateRepository.findByIdAndDeletedFalse(templateId)
                .filter(CreativeTemplate::isMasterTemplate)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Creative template not found"));
        template.update(command.name(), command.category(), command.description(), command.platform(), command.language(),
                command.campaignObjective(), command.templatePayload(), command.status());
        return mapper.toView(templateRepository.save(template));
    }

    @Transactional
    public AppliedCreativeTemplateView applyTemplate(UUID workspaceId, UUID projectId, UUID templateId) {
        ProjectEntity project = requireProject(workspaceId, projectId);
        CreativeTemplate template = requireTemplate(workspaceId, templateId);
        if (!template.isActive()) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Creative template is inactive");
        }
        Map<String, Object> payload = new LinkedHashMap<>(template.getTemplatePayload());
        payload.put("projectId", projectId.toString());
        payload.put("brandId", project.getBrandId().toString());
        publish(KafkaTopicConstants.CREATIVE_TEMPLATE_APPLIED, workspaceId, templateId, Map.of("projectId", projectId.toString()));
        return new AppliedCreativeTemplateView(templateId, workspaceId, projectId, project.getBrandId(), payload);
    }

    @Transactional
    public CampaignPackageView createCampaignPackage(CampaignPackageCommand command) {
        requireProject(command.workspaceId(), command.projectId());
        CampaignPackage pack = packageRepository.save(CampaignPackage.create(command.workspaceId(), command.projectId(), command.name(), command.description()));
        List<CampaignPackageItem> items = new ArrayList<>();
        for (CampaignPackageCommand.CampaignPackageItemCommand itemCommand : command.items() == null ? List.<CampaignPackageCommand.CampaignPackageItemCommand>of() : command.items()) {
            validatePackageItem(command.workspaceId(), command.projectId(), itemCommand);
            items.add(packageItemRepository.save(CampaignPackageItem.create(command.workspaceId(), pack.getId(), command.projectId(), itemCommand.itemType(), itemCommand.itemId())));
        }
        publish(KafkaTopicConstants.CAMPAIGN_PACKAGE_CREATED, command.workspaceId(), pack.getId(), Map.of("itemCount", items.size()));
        return mapper.toView(pack, items);
    }

    @Transactional(readOnly = true)
    public List<CampaignPackageView> listCampaignPackages(UUID workspaceId, UUID projectId) {
        requireProject(workspaceId, projectId);
        return packageRepository.findAllByWorkspaceIdAndProjectIdAndDeletedFalseOrderByCreatedAtDesc(workspaceId, projectId)
                .stream().map(pack -> mapper.toView(pack, packageItemRepository.findAllByWorkspaceIdAndCampaignPackageIdAndDeletedFalse(workspaceId, pack.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public CampaignPackageView getCampaignPackage(UUID workspaceId, UUID packageId) {
        CampaignPackage pack = requirePackage(workspaceId, packageId);
        return mapper.toView(pack, packageItemRepository.findAllByWorkspaceIdAndCampaignPackageIdAndDeletedFalse(workspaceId, packageId));
    }

    @Transactional
    public CampaignPackageExportUrlView exportUrl(UUID workspaceId, UUID packageId) {
        CampaignPackage pack = requirePackage(workspaceId, packageId);
        String objectKey = "campaign-packages/%s/%s/export.zip".formatted(workspaceId, packageId);
        Instant expiresAt = Instant.now().plus(Duration.ofMinutes(30));
        String signedUrl = "r2-signed://%s?expiresAt=%s".formatted(objectKey, expiresAt);
        pack.markExportRequested(objectKey, signedUrl, expiresAt);
        packageRepository.save(pack);
        usageRepository.save(UsageBillingLog.create(workspaceId, "CAMPAIGN_PACKAGE_EXPORT", REFERENCE_TYPE_EXPORT, packageId, BigDecimal.ZERO, null, null, null));
        publish(KafkaTopicConstants.CAMPAIGN_PACKAGE_EXPORT_REQUESTED, workspaceId, packageId, Map.of("r2ObjectKey", objectKey));
        publish(KafkaTopicConstants.USAGE_EVENT_RECORDED, workspaceId, packageId, Map.of("referenceType", REFERENCE_TYPE_EXPORT));
        return new CampaignPackageExportUrlView(packageId, objectKey, signedUrl, expiresAt);
    }

    @Transactional(readOnly = true)
    public BulkGenerationPreviewView previewBulk(BulkGenerationCommand command) {
        requireProject(command.workspaceId(), command.projectId());
        CreativeTool tool = requireBulkTool();
        requireBulkPolicy(command.workspaceId(), tool.getToolCode());
        BigDecimal unitCost = resolveCreditCost(tool.getId());
        int count = itemCount(command);
        BigDecimal total = unitCost.multiply(BigDecimal.valueOf(count));
        publish(KafkaTopicConstants.BULK_GENERATION_PREVIEW_CREATED, command.workspaceId(), command.projectId(), Map.of("estimatedCredits", total));
        return new BulkGenerationPreviewView(command.generationType(), count, unitCost, total);
    }

    @Transactional
    public BulkGenerationJobView queueBulk(BulkGenerationCommand command) {
        BulkGenerationPreviewView preview = previewBulk(command);
        String lockKey = "bulk-generation:%s:%s".formatted(command.workspaceId(), command.projectId());
        RedisLockService lockService = redisLockService.getIfAvailable();
        Optional<RedisLockService.RedisLockToken> token = lockService == null
                ? Optional.empty()
                : lockService.acquire(lockKey, Duration.ofSeconds(15));
        if (lockService != null && token.isEmpty()) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Bulk generation job is already being queued");
        }
        try {
            Map<String, Object> payload = requestPayload(command);
            BulkGenerationJob job = bulkJobRepository.save(BulkGenerationJob.queued(command.workspaceId(), command.projectId(), command.generationType(),
                    command.platform(), command.language(), preview.itemCount(), preview.estimatedCredits(), payload));
            for (UUID sourceId : command.sourceIds() == null ? List.<UUID>of() : command.sourceIds()) {
                bulkItemRepository.save(BulkGenerationItem.queued(command.workspaceId(), job.getId(), command.projectId(), sourceId, Map.of("sourceId", sourceId.toString())));
            }
            usageRepository.save(UsageBillingLog.create(command.workspaceId(), "BULK_GENERATION_QUEUE", REFERENCE_TYPE_BULK, job.getId(), preview.estimatedCredits(), null, null, null));
            publish(KafkaTopicConstants.BULK_GENERATION_JOB_QUEUED, command.workspaceId(), job.getId(), Map.of("estimatedCredits", preview.estimatedCredits()));
            publish(KafkaTopicConstants.USAGE_EVENT_RECORDED, command.workspaceId(), job.getId(), Map.of("referenceType", REFERENCE_TYPE_BULK));
            return mapper.toView(job);
        } finally {
            if (lockService != null) {
                token.ifPresent(lockService::releaseQuietly);
            }
        }
    }

    @Transactional(readOnly = true)
    public BulkGenerationJobView getBulkJob(UUID workspaceId, UUID jobId) {
        return mapper.toView(bulkJobRepository.findByIdAndWorkspaceIdAndDeletedFalse(jobId, workspaceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Bulk generation job not found")));
    }

    private CreativeTemplate requireTemplate(UUID workspaceId, UUID templateId) {
        return templateRepository.findByIdAndDeletedFalse(templateId)
                .filter(template -> template.accessibleTo(workspaceId))
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Creative template not found"));
    }

    private ProjectEntity requireProject(UUID workspaceId, UUID projectId) {
        return projectRepository.findByIdAndWorkspaceIdAndDeletedFalse(projectId, workspaceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Project not found"));
    }

    private CampaignPackage requirePackage(UUID workspaceId, UUID packageId) {
        return packageRepository.findByIdAndWorkspaceIdAndDeletedFalse(packageId, workspaceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Campaign package not found"));
    }

    private void validatePackageItem(UUID workspaceId, UUID projectId, CampaignPackageCommand.CampaignPackageItemCommand item) {
        if (item == null || item.itemType() == null || item.itemId() == null) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Campaign package item is invalid");
        }
        if (item.itemType() == CampaignPackageItemType.GENERATED_VERSION) {
            GeneratedVersionEntity version = generatedVersionRepository.findByIdAndWorkspaceIdAndDeletedFalse(item.itemId(), workspaceId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Generated version not found"));
            if (!projectId.equals(version.getProjectCampaignId())) {
                throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Generated version does not belong to project");
            }
        } else {
            CreativeTextToolOutput output = textOutputRepository.findByIdAndWorkspaceIdAndDeletedFalse(item.itemId(), workspaceId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Text tool output not found"));
            if (!projectId.equals(output.getProjectId())) {
                throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Text output does not belong to project");
            }
        }
    }

    private CreativeTool requireBulkTool() {
        return toolRepository.findByToolCodeAndDeletedFalse(BULK_GENERATION_TOOL_CODE)
                .filter(CreativeTool::isEnabled)
                .or(() -> toolRepository.findByToolCodeAndDeletedFalse(ProductImageCreativeService.TOOL_CODE).filter(CreativeTool::isEnabled))
                .or(() -> toolRepository.findByToolCodeAndDeletedFalse(CreativeTextToolType.POST.toolCode()).filter(CreativeTool::isEnabled))
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Bulk generation tool is not configured"));
    }

    private void requireBulkPolicy(UUID workspaceId, String toolCode) {
        PlanFeaturePolicyView policy = planContextService.getWorkspacePlanContext(workspaceId).featurePolicy();
        if (policy == null || !policy.creativeGenerationEnabled() || policy.enabledCreativeToolCodes() == null || !policy.enabledCreativeToolCodes().contains(toolCode)) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Package does not allow bulk generation");
        }
    }

    private BigDecimal resolveCreditCost(UUID toolId) {
        Instant now = Instant.now();
        return costPolicyRepository.findFirstByToolIdAndEnabledTrueAndDeletedFalseOrderByUpdatedAtDesc(toolId)
                .filter(policy -> (policy.getEffectiveFrom() == null || !policy.getEffectiveFrom().isAfter(now))
                        && (policy.getEffectiveUntil() == null || policy.getEffectiveUntil().isAfter(now)))
                .map(ToolCreditCostPolicy::getCreditCost)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Master bulk credit cost policy is not configured"));
    }

    private int itemCount(BulkGenerationCommand command) {
        int count = command.sourceIds() == null ? 0 : command.sourceIds().size();
        if (count < 1) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Bulk generation requires at least one source item");
        }
        return count;
    }

    private Map<String, Object> requestPayload(BulkGenerationCommand command) {
        Map<String, Object> payload = new LinkedHashMap<>(command.options() == null ? Map.of() : command.options());
        payload.put("generationType", command.generationType().name());
        payload.put("platform", command.platform() == null ? "" : command.platform().name());
        payload.put("language", command.language() == null ? "" : command.language().name());
        payload.put("sourceCount", itemCount(command));
        return payload;
    }

    private void publish(String topic, UUID workspaceId, UUID aggregateId, Map<String, Object> attributes) {
        if (domainEventPublisher != null) {
            domainEventPublisher.publish(topic, new BaseDomainEvent(topic, workspaceId, aggregateId, Instant.now(), attributes));
        }
    }
}
