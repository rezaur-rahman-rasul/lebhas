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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/projects/{projectId}/prompts/history")
@Tag(name = "Prompt History")
@SecurityRequirement(name = "bearerAuth")
public class PromptHistoryController {

    private final PromptHistoryService promptHistoryService;

    public PromptHistoryController(PromptHistoryService promptHistoryService) {
        this.promptHistoryService = promptHistoryService;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('PROMPT_HISTORY_VIEW','PROMPT_TEMPLATE_MANAGE')")
    @Operation(summary = "List prompt history for a project")
    public ApiResponse<PagedResult<PromptHistoryView>> listHistory(
            @PathVariable UUID workspaceId,
            @PathVariable UUID projectId,
            @Valid @ModelAttribute PromptHistoryListRequest request
    ) {
        return ApiResponse.success(promptHistoryService.listHistory(new PromptHistoryFilter(
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
                request.getSize() == null ? 20 : request.getSize())));
    }
}
