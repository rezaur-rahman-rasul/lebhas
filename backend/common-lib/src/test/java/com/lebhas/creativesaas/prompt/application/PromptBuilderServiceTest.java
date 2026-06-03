package com.lebhas.creativesaas.prompt.application;

import com.lebhas.creativesaas.common.security.Permission;
import com.lebhas.creativesaas.common.security.Role;
import com.lebhas.creativesaas.common.security.context.CurrentUser;
import com.lebhas.creativesaas.identity.application.WorkspaceAuthorizationService;
import com.lebhas.creativesaas.messaging.kafka.DomainEventPublisher;
import com.lebhas.creativesaas.prompt.application.dto.PromptEnhancementCommand;
import com.lebhas.creativesaas.prompt.application.dto.PromptEnhancementView;
import com.lebhas.creativesaas.prompt.application.dto.PromptSuggestionCommand;
import com.lebhas.creativesaas.prompt.application.dto.PromptSuggestionsView;
import com.lebhas.creativesaas.prompt.cache.PromptEnhancementCacheService;
import com.lebhas.creativesaas.prompt.cache.PromptRedisAccessSupport;
import com.lebhas.creativesaas.prompt.domain.CampaignObjective;
import com.lebhas.creativesaas.prompt.domain.PromptLanguage;
import com.lebhas.creativesaas.prompt.domain.PromptPlatform;
import com.lebhas.creativesaas.prompt.domain.SuggestionType;
import com.lebhas.creativesaas.workspace.domain.WorkspaceEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PromptBuilderServiceTest {

    @Mock
    private WorkspaceAuthorizationService workspaceAuthorizationService;
    @Mock
    private PromptReadinessService promptReadinessService;
    @Mock
    private PromptHistoryService promptHistoryService;
    @Mock
    private PromptJsonCodec promptJsonCodec;
    @Mock
    private PromptTemplateService promptTemplateService;
    @Mock
    private PromptDraftService promptDraftService;
    @Mock
    private PromptEnhancementCacheService promptEnhancementCacheService;
    @Mock
    private PromptRedisAccessSupport promptRedisAccessSupport;
    @Mock
    private DomainEventPublisher domainEventPublisher;

    @InjectMocks
    private PromptBuilderService service;

    @Test
    void placeholderEnhancementSucceedsWithoutProviderRouter() {
        UUID workspaceId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(workspaceAuthorizationService.requirePermission(workspaceId, Permission.PROMPT_INTELLIGENCE_USE))
                .thenReturn(access(workspaceId, userId));
        when(promptJsonCodec.write(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.anyString()))
                .thenReturn("{}");
        when(promptEnhancementCacheService.sha256("{}")).thenReturn("hash");
        when(promptEnhancementCacheService.get("hash")).thenReturn(Optional.empty());
        when(promptRedisAccessSupport.acquireLock(ArgumentMatchers.anyString(), ArgumentMatchers.any(), ArgumentMatchers.anyString(), ArgumentMatchers.any()))
                .thenReturn(Optional.empty());

        PromptEnhancementView view = service.enhance(new PromptEnhancementCommand(
                workspaceId,
                projectId,
                "Launch the winter collection",
                List.of(),
                null,
                "Fashion retail",
                CampaignObjective.SALES,
                PromptPlatform.FACEBOOK,
                null,
                PromptLanguage.ENGLISH,
                null,
                "Urban professionals",
                "20 percent off",
                "Shop now",
                false,
                "127.0.0.1"));

        assertThat(view.aiProvider()).isEqualTo("INTERNAL_FOUNDATION");
        assertThat(view.enhancedPrompt()).contains("Launch the winter collection", "Business type: Fashion retail");
        assertThat(view.suggestedMissingFields()).isEmpty();
        verify(promptReadinessService).assertReady(ArgumentMatchers.any());
        verify(promptHistoryService).recordSuccess(
                ArgumentMatchers.eq(workspaceId),
                ArgumentMatchers.eq(projectId),
                ArgumentMatchers.isNull(),
                ArgumentMatchers.eq(userId),
                ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString(),
                ArgumentMatchers.eq(PromptLanguage.ENGLISH),
                ArgumentMatchers.eq(PromptPlatform.FACEBOOK),
                ArgumentMatchers.eq(CampaignObjective.SALES),
                ArgumentMatchers.eq("Fashion retail"),
                ArgumentMatchers.isNull(),
                ArgumentMatchers.eq(SuggestionType.ENHANCEMENT),
                ArgumentMatchers.eq("INTERNAL_FOUNDATION"),
                ArgumentMatchers.eq("PROMPT_BUILDER_PLACEHOLDER_V1"),
                ArgumentMatchers.isNull());
    }

    @Test
    void placeholderSuggestionsSucceedWithoutProviderRouter() {
        UUID workspaceId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(workspaceAuthorizationService.requirePermission(workspaceId, Permission.PROMPT_INTELLIGENCE_USE))
                .thenReturn(access(workspaceId, userId));
        when(promptJsonCodec.write(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.anyString()))
                .thenReturn("{}");

        PromptSuggestionsView view = service.suggestions(new PromptSuggestionCommand(
                workspaceId,
                projectId,
                "Premium Eid outfit",
                List.of(),
                null,
                "Fashion retail",
                CampaignObjective.AWARENESS,
                PromptPlatform.INSTAGRAM,
                null,
                PromptLanguage.BANGLA,
                null,
                null,
                null,
                null,
                false,
                Set.of(SuggestionType.HEADLINE_SUGGESTIONS),
                "127.0.0.1"));

        assertThat(view.aiProvider()).isEqualTo("INTERNAL_FOUNDATION");
        assertThat(view.headlineSuggestions()).isNotEmpty();
        verify(promptReadinessService).assertReady(ArgumentMatchers.any());
    }

    @Test
    void serviceHasNoAiProviderDependency() {
        assertThat(PromptBuilderService.class.getDeclaredFields())
                .extracting(field -> field.getType().getName())
                .noneMatch(type -> type.contains("AiProviderRouter") || type.contains("TextAiProvider"));
        verifyNoInteractions(promptTemplateService, promptDraftService);
    }

    private WorkspaceAuthorizationService.WorkspaceAccess access(UUID workspaceId, UUID userId) {
        WorkspaceEntity workspace = mock(WorkspaceEntity.class);
        CurrentUser currentUser = new CurrentUser(
                userId,
                workspaceId,
                "device",
                "owner@lebhas.test",
                Set.of(Role.ADMIN),
                Set.of(Permission.PROMPT_INTELLIGENCE_USE),
                "token",
                Instant.now().plusSeconds(3600));
        return new WorkspaceAuthorizationService.WorkspaceAccess(
                workspace,
                currentUser,
                null,
                Role.ADMIN,
                Set.of(Permission.PROMPT_INTELLIGENCE_USE));
    }
}
