package com.lebhas.creativesaas.creative.interfaces;

import com.lebhas.creativesaas.campaignpackage.application.CreativeTemplateLibraryService;
import com.lebhas.creativesaas.campaignpackage.application.dto.AppliedCreativeTemplateView;
import com.lebhas.creativesaas.campaignpackage.application.dto.BulkGenerationCommand;
import com.lebhas.creativesaas.campaignpackage.application.dto.BulkGenerationJobView;
import com.lebhas.creativesaas.campaignpackage.application.dto.BulkGenerationPreviewView;
import com.lebhas.creativesaas.campaignpackage.application.dto.CampaignPackageCommand;
import com.lebhas.creativesaas.campaignpackage.application.dto.CampaignPackageExportUrlView;
import com.lebhas.creativesaas.campaignpackage.application.dto.CampaignPackageView;
import com.lebhas.creativesaas.campaignpackage.application.dto.CreativeTemplateCommand;
import com.lebhas.creativesaas.campaignpackage.application.dto.CreativeTemplateView;
import com.lebhas.creativesaas.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
public class CreativeTemplateLibraryController {

    private final CreativeTemplateLibraryService service;

    public CreativeTemplateLibraryController(CreativeTemplateLibraryService service) {
        this.service = service;
    }

    @PostMapping("/api/v1/workspaces/{workspaceId}/creative-templates")
    @PreAuthorize("hasAuthority('PROMPT_TEMPLATE_MANAGE')")
    public ApiResponse<CreativeTemplateView> createTemplate(@PathVariable UUID workspaceId, @Valid @RequestBody CreativeTemplateApiRequest request) {
        return ApiResponse.success(service.createWorkspaceTemplate(command(workspaceId, request)));
    }

    @GetMapping("/api/v1/workspaces/{workspaceId}/creative-templates")
    @PreAuthorize("hasAnyAuthority('PROMPT_TEMPLATE_VIEW','PROMPT_TEMPLATE_MANAGE')")
    public ApiResponse<List<CreativeTemplateView>> listTemplates(@PathVariable UUID workspaceId) {
        return ApiResponse.success(service.listWorkspaceTemplates(workspaceId));
    }

    @GetMapping("/api/v1/workspaces/{workspaceId}/creative-templates/{templateId}")
    @PreAuthorize("hasAnyAuthority('PROMPT_TEMPLATE_VIEW','PROMPT_TEMPLATE_MANAGE')")
    public ApiResponse<CreativeTemplateView> getTemplate(@PathVariable UUID workspaceId, @PathVariable UUID templateId) {
        return ApiResponse.success(service.getTemplate(workspaceId, templateId));
    }

    @PutMapping("/api/v1/workspaces/{workspaceId}/creative-templates/{templateId}")
    @PreAuthorize("hasAuthority('PROMPT_TEMPLATE_MANAGE')")
    public ApiResponse<CreativeTemplateView> updateTemplate(@PathVariable UUID workspaceId, @PathVariable UUID templateId, @Valid @RequestBody CreativeTemplateApiRequest request) {
        return ApiResponse.success(service.updateTemplate(workspaceId, templateId, command(workspaceId, request)));
    }

    @PostMapping("/api/v1/workspaces/{workspaceId}/projects/{projectId}/creative-templates/{templateId}/apply")
    @PreAuthorize("hasAuthority('PROMPT_TEMPLATE_VIEW')")
    public ApiResponse<AppliedCreativeTemplateView> applyTemplate(@PathVariable UUID workspaceId, @PathVariable UUID projectId, @PathVariable UUID templateId) {
        return ApiResponse.success(service.applyTemplate(workspaceId, projectId, templateId));
    }

    @PostMapping("/api/v1/master/creative-templates")
    @PreAuthorize("hasAuthority('MASTER_ADMIN')")
    public ApiResponse<CreativeTemplateView> createMasterTemplate(@Valid @RequestBody CreativeTemplateApiRequest request) {
        return ApiResponse.success(service.createMasterTemplate(command(new UUID(0L, 0L), request)));
    }

    @GetMapping("/api/v1/master/creative-templates")
    @PreAuthorize("hasAuthority('MASTER_ADMIN')")
    public ApiResponse<List<CreativeTemplateView>> listMasterTemplates() {
        return ApiResponse.success(service.listMasterTemplates());
    }

    @PutMapping("/api/v1/master/creative-templates/{templateId}")
    @PreAuthorize("hasAuthority('MASTER_ADMIN')")
    public ApiResponse<CreativeTemplateView> updateMasterTemplate(@PathVariable UUID templateId, @Valid @RequestBody CreativeTemplateApiRequest request) {
        return ApiResponse.success(service.updateMasterTemplate(templateId, command(new UUID(0L, 0L), request)));
    }

    @PostMapping("/api/v1/workspaces/{workspaceId}/projects/{projectId}/campaign-packages")
    @PreAuthorize("hasAuthority('CREATIVE_GENERATE')")
    public ApiResponse<CampaignPackageView> createPackage(@PathVariable UUID workspaceId, @PathVariable UUID projectId, @Valid @RequestBody CampaignPackageApiRequest request) {
        return ApiResponse.success(service.createCampaignPackage(new CampaignPackageCommand(
                workspaceId,
                projectId,
                request.name(),
                request.description(),
                request.items() == null ? List.of() : request.items().stream()
                        .map(item -> new CampaignPackageCommand.CampaignPackageItemCommand(item.itemType(), item.itemId()))
                        .toList())));
    }

    @GetMapping("/api/v1/workspaces/{workspaceId}/projects/{projectId}/campaign-packages")
    @PreAuthorize("hasAuthority('WORKSPACE_VIEW')")
    public ApiResponse<List<CampaignPackageView>> listPackages(@PathVariable UUID workspaceId, @PathVariable UUID projectId) {
        return ApiResponse.success(service.listCampaignPackages(workspaceId, projectId));
    }

    @GetMapping("/api/v1/workspaces/{workspaceId}/campaign-packages/{packageId}")
    @PreAuthorize("hasAuthority('WORKSPACE_VIEW')")
    public ApiResponse<CampaignPackageView> getPackage(@PathVariable UUID workspaceId, @PathVariable UUID packageId) {
        return ApiResponse.success(service.getCampaignPackage(workspaceId, packageId));
    }

    @PostMapping("/api/v1/workspaces/{workspaceId}/campaign-packages/{packageId}/export-url")
    @PreAuthorize("hasAuthority('DOWNLOAD_GENERATED_VERSION')")
    public ApiResponse<CampaignPackageExportUrlView> exportPackage(@PathVariable UUID workspaceId, @PathVariable UUID packageId) {
        return ApiResponse.success(service.exportUrl(workspaceId, packageId));
    }

    @PostMapping("/api/v1/workspaces/{workspaceId}/projects/{projectId}/bulk-generation/preview")
    @PreAuthorize("hasAuthority('CREATIVE_GENERATE')")
    public ApiResponse<BulkGenerationPreviewView> previewBulk(@PathVariable UUID workspaceId, @PathVariable UUID projectId, @Valid @RequestBody BulkGenerationApiRequest request) {
        return ApiResponse.success(service.previewBulk(bulkCommand(workspaceId, projectId, request)));
    }

    @PostMapping("/api/v1/workspaces/{workspaceId}/projects/{projectId}/bulk-generation/queue")
    @PreAuthorize("hasAuthority('CREATIVE_GENERATE')")
    public ApiResponse<BulkGenerationJobView> queueBulk(@PathVariable UUID workspaceId, @PathVariable UUID projectId, @Valid @RequestBody BulkGenerationApiRequest request) {
        return ApiResponse.success(service.queueBulk(bulkCommand(workspaceId, projectId, request)));
    }

    @GetMapping("/api/v1/workspaces/{workspaceId}/bulk-generation-jobs/{jobId}")
    @PreAuthorize("hasAuthority('WORKSPACE_VIEW')")
    public ApiResponse<BulkGenerationJobView> getBulkJob(@PathVariable UUID workspaceId, @PathVariable UUID jobId) {
        return ApiResponse.success(service.getBulkJob(workspaceId, jobId));
    }

    private CreativeTemplateCommand command(UUID workspaceId, CreativeTemplateApiRequest request) {
        return new CreativeTemplateCommand(workspaceId, request.name(), request.category(), request.description(), request.platform(),
                request.language(), request.campaignObjective(), request.templatePayload(), request.status());
    }

    private BulkGenerationCommand bulkCommand(UUID workspaceId, UUID projectId, BulkGenerationApiRequest request) {
        return new BulkGenerationCommand(workspaceId, projectId, request.generationType(), request.platform(), request.language(), request.sourceIds(), request.options());
    }
}
