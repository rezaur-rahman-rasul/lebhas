package com.lebhas.creativesaas.creative.interfaces;

import com.lebhas.creativesaas.common.api.ApiResponse;
import com.lebhas.creativesaas.prompt.application.PromptTemplateService;
import com.lebhas.creativesaas.prompt.application.dto.CreatePromptTemplateCommand;
import com.lebhas.creativesaas.prompt.application.dto.PromptTemplateFilter;
import com.lebhas.creativesaas.prompt.application.dto.PromptTemplateView;
import com.lebhas.creativesaas.prompt.application.dto.UpdatePromptTemplateCommand;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/v1/workspaces/{workspaceId}/prompt-templates")
@Tag(name = "Prompt Templates")
@SecurityRequirement(name = "bearerAuth")
public class PromptTemplateController {

    private final PromptTemplateService promptTemplateService;

    public PromptTemplateController(PromptTemplateService promptTemplateService) {
        this.promptTemplateService = promptTemplateService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PROMPT_TEMPLATE_MANAGE')")
    @Operation(summary = "Create a workspace or system prompt template")
    public ApiResponse<PromptTemplateView> createTemplate(
            @PathVariable UUID workspaceId,
            @Valid @RequestBody CreatePromptTemplateRequest request
    ) {
        return ApiResponse.success(promptTemplateService.createTemplate(new CreatePromptTemplateCommand(
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

    @GetMapping
    @PreAuthorize("hasAnyAuthority('PROMPT_TEMPLATE_VIEW','PROMPT_TEMPLATE_MANAGE')")
    @Operation(summary = "List prompt templates available in a workspace")
    public ApiResponse<List<PromptTemplateView>> listTemplates(
            @PathVariable UUID workspaceId,
            @Valid @ModelAttribute PromptTemplateListRequest request
    ) {
        return ApiResponse.success(promptTemplateService.listTemplates(new PromptTemplateFilter(
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

    @GetMapping("/{templateId}")
    @PreAuthorize("hasAnyAuthority('PROMPT_TEMPLATE_VIEW','PROMPT_TEMPLATE_MANAGE')")
    @Operation(summary = "Get a prompt template")
    public ApiResponse<PromptTemplateView> getTemplate(@PathVariable UUID workspaceId, @PathVariable UUID templateId) {
        return ApiResponse.success(promptTemplateService.getTemplate(workspaceId, templateId));
    }

    @PutMapping("/{templateId}")
    @PreAuthorize("hasAuthority('PROMPT_TEMPLATE_MANAGE')")
    @Operation(summary = "Update a prompt template")
    public ApiResponse<PromptTemplateView> updateTemplate(
            @PathVariable UUID workspaceId,
            @PathVariable UUID templateId,
            @Valid @RequestBody UpdatePromptTemplateRequest request
    ) {
        return ApiResponse.success(promptTemplateService.updateTemplate(new UpdatePromptTemplateCommand(
                workspaceId,
                templateId,
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

    @DeleteMapping("/{templateId}")
    @PreAuthorize("hasAuthority('PROMPT_TEMPLATE_MANAGE')")
    @Operation(summary = "Soft delete a prompt template")
    public ApiResponse<Void> deleteTemplate(@PathVariable UUID workspaceId, @PathVariable UUID templateId) {
        promptTemplateService.deleteTemplate(workspaceId, templateId);
        return ApiResponse.success("Prompt template deleted", null);
    }
}
