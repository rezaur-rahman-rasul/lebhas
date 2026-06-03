package com.lebhas.creativesaas.creative.interfaces;

import com.lebhas.creativesaas.common.api.ApiResponse;
import com.lebhas.creativesaas.prompt.application.PromptBuilderService;
import com.lebhas.creativesaas.prompt.application.PromptDraftService;
import com.lebhas.creativesaas.prompt.application.PromptReadinessService;
import com.lebhas.creativesaas.prompt.application.dto.CreatePromptTemplateCommand;
import com.lebhas.creativesaas.prompt.application.dto.PromptBuilderContextView;
import com.lebhas.creativesaas.prompt.application.dto.PromptDraftCommand;
import com.lebhas.creativesaas.prompt.application.dto.PromptDraftView;
import com.lebhas.creativesaas.prompt.application.dto.PromptReadinessView;
import com.lebhas.creativesaas.prompt.application.dto.PromptTemplateFilter;
import com.lebhas.creativesaas.prompt.application.dto.PromptTemplateReuseView;
import com.lebhas.creativesaas.prompt.application.dto.PromptTemplateView;
import com.lebhas.creativesaas.prompt.application.dto.PromptValidationCommand;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@Tag(name = "Prompt Builder")
@SecurityRequirement(name = "bearerAuth")
public class PromptBuilderController {

    private final PromptReadinessService promptReadinessService;
    private final PromptDraftService promptDraftService;
    private final PromptBuilderService promptBuilderService;

    public PromptBuilderController(
            PromptReadinessService promptReadinessService,
            PromptDraftService promptDraftService,
            PromptBuilderService promptBuilderService
    ) {
        this.promptReadinessService = promptReadinessService;
        this.promptDraftService = promptDraftService;
        this.promptBuilderService = promptBuilderService;
    }

    @GetMapping("/api/v1/workspaces/{workspaceId}/projects/{projectId}/prompts/context")
    @PreAuthorize("hasAuthority('PROMPT_INTELLIGENCE_USE')")
    public ApiResponse<PromptBuilderContextView> context(@PathVariable UUID workspaceId, @PathVariable UUID projectId) {
        return ApiResponse.success(promptReadinessService.getContext(workspaceId, projectId));
    }

    @PostMapping("/api/v1/workspaces/{workspaceId}/projects/{projectId}/prompts/validate")
    @PreAuthorize("hasAuthority('PROMPT_INTELLIGENCE_USE')")
    public ApiResponse<PromptReadinessView> validate(
            @PathVariable UUID workspaceId,
            @PathVariable UUID projectId,
            @Valid @RequestBody PromptValidateRequest request
    ) {
        return ApiResponse.success(promptReadinessService.validate(new PromptValidationCommand(
                workspaceId,
                projectId,
                request.promptText(),
                request.language(),
                request.assetIds(),
                request.requireEnhancement(),
                request.requireSuggestions(),
                request.requireTemplates())));
    }

    @PostMapping("/api/v1/workspaces/{workspaceId}/projects/{projectId}/prompts/drafts")
    @PreAuthorize("hasAuthority('PROMPT_INTELLIGENCE_USE')")
    public ApiResponse<PromptDraftView> createDraft(
            @PathVariable UUID workspaceId,
            @PathVariable UUID projectId,
            @Valid @RequestBody PromptDraftRequest request
    ) {
        return ApiResponse.success(promptDraftService.create(toDraftCommand(workspaceId, projectId, request)));
    }

    @PutMapping("/api/v1/workspaces/{workspaceId}/projects/{projectId}/prompts/drafts/{draftId}")
    @PreAuthorize("hasAuthority('PROMPT_INTELLIGENCE_USE')")
    public ApiResponse<PromptDraftView> updateDraft(
            @PathVariable UUID workspaceId,
            @PathVariable UUID projectId,
            @PathVariable UUID draftId,
            @Valid @RequestBody PromptDraftRequest request
    ) {
        return ApiResponse.success(promptDraftService.update(workspaceId, projectId, draftId, toDraftCommand(workspaceId, projectId, request)));
    }

    @GetMapping("/api/v1/workspaces/{workspaceId}/projects/{projectId}/prompts/drafts")
    @PreAuthorize("hasAuthority('PROMPT_INTELLIGENCE_USE')")
    public ApiResponse<List<PromptDraftView>> listDrafts(@PathVariable UUID workspaceId, @PathVariable UUID projectId) {
        return ApiResponse.success(promptDraftService.list(workspaceId, projectId));
    }

    @PostMapping("/api/v1/workspaces/{workspaceId}/prompts/templates")
    @PreAuthorize("hasAuthority('PROMPT_TEMPLATE_MANAGE')")
    public ApiResponse<PromptTemplateView> createTemplate(
            @PathVariable UUID workspaceId,
            @Valid @RequestBody CreatePromptTemplateRequest request
    ) {
        return ApiResponse.success(promptBuilderService.createTemplate(new CreatePromptTemplateCommand(
                workspaceId,
                request.name(),
                request.category(),
                request.description(),
                request.platform(),
                request.campaignObjective(),
                request.businessType(),
                request.language(),
                request.templateText(),
                request.systemDefault(),
                request.status())));
    }

    @GetMapping("/api/v1/workspaces/{workspaceId}/prompts/templates")
    @PreAuthorize("hasAnyAuthority('PROMPT_TEMPLATE_VIEW','PROMPT_TEMPLATE_MANAGE')")
    public ApiResponse<List<PromptTemplateView>> listTemplates(
            @PathVariable UUID workspaceId,
            @Valid @ModelAttribute PromptTemplateListRequest request
    ) {
        return ApiResponse.success(promptBuilderService.listTemplates(new PromptTemplateFilter(
                workspaceId,
                request.getCategory(),
                request.getPlatform(),
                request.getCampaignObjective(),
                request.getLanguage(),
                request.getBusinessType(),
                request.getStatus(),
                request.getSearch(),
                request.getSystemDefault(),
                request.getIncludeSystemDefaults() == null || request.getIncludeSystemDefaults())));
    }

    @PostMapping("/api/v1/workspaces/{workspaceId}/projects/{projectId}/prompts/templates/{templateId}/reuse")
    @PreAuthorize("hasAnyAuthority('PROMPT_TEMPLATE_VIEW','PROMPT_TEMPLATE_MANAGE')")
    public ApiResponse<PromptTemplateReuseView> reuseTemplate(
            @PathVariable UUID workspaceId,
            @PathVariable UUID projectId,
            @PathVariable UUID templateId
    ) {
        return ApiResponse.success(promptBuilderService.reuseTemplate(workspaceId, projectId, templateId));
    }

    private PromptDraftCommand toDraftCommand(UUID workspaceId, UUID projectId, PromptDraftRequest request) {
        return new PromptDraftCommand(
                workspaceId,
                projectId,
                request.title(),
                request.promptText(),
                request.language(),
                request.platform(),
                request.campaignObjective(),
                request.templateId());
    }

}
