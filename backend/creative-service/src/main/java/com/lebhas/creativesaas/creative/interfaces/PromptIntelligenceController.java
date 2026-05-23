package com.lebhas.creativesaas.creative.interfaces;

import com.lebhas.creativesaas.common.api.ApiResponse;
import com.lebhas.creativesaas.prompt.application.PromptEnhancementService;
import com.lebhas.creativesaas.prompt.application.PromptSuggestionService;
import com.lebhas.creativesaas.prompt.application.dto.PromptEnhancementCommand;
import com.lebhas.creativesaas.prompt.application.dto.PromptEnhancementView;
import com.lebhas.creativesaas.prompt.application.dto.PromptSuggestionCommand;
import com.lebhas.creativesaas.prompt.application.dto.PromptSuggestionsView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/projects/{projectId}/prompts")
@Tag(name = "Prompt Intelligence")
@SecurityRequirement(name = "bearerAuth")
public class PromptIntelligenceController {

    private final PromptEnhancementService promptEnhancementService;
    private final PromptSuggestionService promptSuggestionService;

    public PromptIntelligenceController(
            PromptEnhancementService promptEnhancementService,
            PromptSuggestionService promptSuggestionService
    ) {
        this.promptEnhancementService = promptEnhancementService;
        this.promptSuggestionService = promptSuggestionService;
    }

    @PostMapping("/enhance")
    @PreAuthorize("hasAuthority('PROMPT_INTELLIGENCE_USE')")
    @Operation(summary = "Enhance a project prompt")
    public ApiResponse<PromptEnhancementView> enhance(
            @PathVariable UUID workspaceId,
            @PathVariable UUID projectId,
            @Valid @RequestBody PromptEnhanceRequest request,
            HttpServletRequest httpServletRequest
    ) {
        return ApiResponse.success(promptEnhancementService.enhance(new PromptEnhancementCommand(
                workspaceId,
                projectId,
                request.customPrompt(),
                request.assetIds(),
                request.templateId(),
                request.businessType(),
                request.campaignObjective(),
                request.platform(),
                request.creativeStyle(),
                request.language(),
                request.tone(),
                request.targetAudience(),
                request.offerDetails(),
                request.ctaPreference(),
                request.useBrandProfile(),
                resolveClientIp(httpServletRequest))));
    }

    @PostMapping("/suggestions")
    @PreAuthorize("hasAuthority('PROMPT_INTELLIGENCE_USE')")
    @Operation(summary = "Generate project prompt suggestions")
    public ApiResponse<PromptSuggestionsView> suggestions(
            @PathVariable UUID workspaceId,
            @PathVariable UUID projectId,
            @Valid @RequestBody PromptSuggestionsRequest request,
            HttpServletRequest httpServletRequest
    ) {
        return ApiResponse.success(promptSuggestionService.generateSuggestions(new PromptSuggestionCommand(
                workspaceId,
                projectId,
                request.customPrompt(),
                request.assetIds(),
                request.templateId(),
                request.businessType(),
                request.campaignObjective(),
                request.platform(),
                request.creativeStyle(),
                request.language(),
                request.tone(),
                request.targetAudience(),
                request.offerDetails(),
                request.ctaPreference(),
                request.useBrandProfile(),
                request.suggestionTypes(),
                resolveClientIp(httpServletRequest))));
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwardedFor)) {
            int separator = forwardedFor.indexOf(',');
            return (separator >= 0 ? forwardedFor.substring(0, separator) : forwardedFor).trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        return StringUtils.hasText(realIp) ? realIp.trim() : request.getRemoteAddr();
    }
}
