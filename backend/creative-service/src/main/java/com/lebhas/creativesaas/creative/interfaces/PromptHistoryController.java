package com.lebhas.creativesaas.creative.interfaces;

import com.lebhas.creativesaas.common.api.ApiResponse;
import com.lebhas.creativesaas.common.api.PagedResult;
import com.lebhas.creativesaas.prompt.application.PromptHistoryService;
import com.lebhas.creativesaas.prompt.application.dto.PromptHistoryFilter;
import com.lebhas.creativesaas.prompt.application.dto.PromptHistoryView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@Tag(name = "Prompt History")
@SecurityRequirement(name = "bearerAuth")
public class PromptHistoryController {

    private final PromptHistoryService promptHistoryService;

    public PromptHistoryController(PromptHistoryService promptHistoryService) {
        this.promptHistoryService = promptHistoryService;
    }

    @GetMapping("/api/v1/workspaces/{workspaceId}/projects/{projectId}/prompts/history")
    @PreAuthorize("hasAnyAuthority('PROMPT_HISTORY_VIEW','PROMPT_TEMPLATE_MANAGE')")
    @Operation(summary = "List prompt history for a project")
    public ApiResponse<PagedResult<PromptHistoryView>> listProjectHistory(
            @PathVariable UUID workspaceId,
            @PathVariable UUID projectId,
            @Valid @ModelAttribute PromptHistoryListRequest request
    ) {
        return ApiResponse.success(list(workspaceId, projectId, request));
    }

    @GetMapping("/api/v1/workspaces/{workspaceId}/prompt-history")
    @PreAuthorize("hasAnyAuthority('PROMPT_HISTORY_VIEW','PROMPT_TEMPLATE_MANAGE')")
    @Operation(summary = "List prompt history for a workspace")
    public ApiResponse<PagedResult<PromptHistoryView>> listWorkspaceHistory(
            @PathVariable UUID workspaceId,
            @Valid @ModelAttribute PromptHistoryListRequest request
    ) {
        return ApiResponse.success(list(workspaceId, null, request));
    }

    @GetMapping("/api/v1/workspaces/{workspaceId}/prompt-history/{historyId}")
    @PreAuthorize("hasAnyAuthority('PROMPT_HISTORY_VIEW','PROMPT_TEMPLATE_MANAGE')")
    @Operation(summary = "Get prompt history detail")
    public ApiResponse<PromptHistoryView> getHistory(
            @PathVariable UUID workspaceId,
            @PathVariable UUID historyId
    ) {
        return ApiResponse.success(promptHistoryService.getHistory(workspaceId, null, historyId));
    }

    private PagedResult<PromptHistoryView> list(
            UUID workspaceId,
            UUID projectId,
            PromptHistoryListRequest request
    ) {
        return promptHistoryService.listHistory(new PromptHistoryFilter(
                workspaceId,
                projectId,
                request.getUserId(),
                request.getSuggestionType(),
                request.getPlatform(),
                request.getCampaignObjective(),
                request.getStatus(),
                request.getCreatedFrom(),
                request.getCreatedTo(),
                request.getPage() == null ? 0 : request.getPage(),
                request.getSize() == null ? 20 : request.getSize()));
    }
}
