package com.lebhas.creativesaas.ai;

import com.lebhas.ai.application.AiCredentialEncryptionService;
import com.lebhas.ai.application.MasterAiProviderToolRegistryService;
import com.lebhas.ai.application.dto.AiProviderCredentialCommand;
import com.lebhas.ai.application.dto.CreativeToolCommand;
import com.lebhas.ai.application.dto.ProviderRoutingPolicyCommand;
import com.lebhas.ai.application.dto.ResolvedProviderRouteView;
import com.lebhas.ai.domain.AiProviderCredential;
import com.lebhas.ai.domain.AiToolProvider;
import com.lebhas.ai.domain.ProviderEnvironment;
import com.lebhas.ai.domain.CreativeTool;
import com.lebhas.ai.domain.CreativeToolCategory;
import com.lebhas.ai.domain.ProviderHealthSnapshot;
import com.lebhas.ai.domain.ProviderStatus;
import com.lebhas.ai.domain.ProviderType;
import com.lebhas.ai.domain.ToolCreditCostPolicy;
import com.lebhas.ai.infrastructure.persistence.AiModelRepository;
import com.lebhas.ai.infrastructure.persistence.AiProviderCredentialRepository;
import com.lebhas.ai.infrastructure.persistence.AiToolProviderRepository;
import com.lebhas.ai.infrastructure.persistence.CreativeToolCapabilityRepository;
import com.lebhas.ai.infrastructure.persistence.CreativeToolRepository;
import com.lebhas.ai.infrastructure.persistence.ProviderHealthSnapshotRepository;
import com.lebhas.ai.infrastructure.persistence.ProviderRoutingPolicyRepository;
import com.lebhas.ai.infrastructure.persistence.ToolCreditCostPolicyRepository;
import com.lebhas.creativesaas.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class Day11AiProviderToolRegistryUnitTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-05-31T00:00:00Z"), ZoneOffset.UTC);
    private static final UUID PRIMARY_ID = UUID.fromString("00000000-0000-0000-0000-000000000011");
    private static final UUID FALLBACK_ID = UUID.fromString("00000000-0000-0000-0000-000000000012");
    private static final UUID MOCK_ID = UUID.fromString("00000000-0000-0000-0000-000000000013");
    private static final UUID TOOL_ID = UUID.fromString("00000000-0000-0000-0000-000000000021");

    private final AiToolProviderRepository providerRepository = mock(AiToolProviderRepository.class);
    private final AiModelRepository modelRepository = mock(AiModelRepository.class);
    private final AiProviderCredentialRepository credentialRepository = mock(AiProviderCredentialRepository.class);
    private final CreativeToolRepository toolRepository = mock(CreativeToolRepository.class);
    private final CreativeToolCapabilityRepository toolCapabilityRepository = mock(CreativeToolCapabilityRepository.class);
    private final ToolCreditCostPolicyRepository costPolicyRepository = mock(ToolCreditCostPolicyRepository.class);
    private final ProviderRoutingPolicyRepository routingPolicyRepository = mock(ProviderRoutingPolicyRepository.class);
    private final ProviderHealthSnapshotRepository healthSnapshotRepository = mock(ProviderHealthSnapshotRepository.class);
    private final MasterAiProviderToolRegistryService service = service();

    @Test
    void secretsAreEncryptedAndOnlyMaskedSecretIsReturned() {
        AiToolProvider provider = provider(PRIMARY_ID, "OPENAI", true);
        when(providerRepository.findByIdAndDeletedFalse(PRIMARY_ID)).thenReturn(Optional.of(provider));
        when(credentialRepository.save(any(AiProviderCredential.class))).thenAnswer(invocation -> withId(invocation.getArgument(0), UUID.randomUUID()));

        var view = service.createCredential(PRIMARY_ID, new AiProviderCredentialCommand(
                "production",
                "sk-live-secret",
                true,
                Map.of("secretNote", "drop", "region", "us")));

        assertThat(view.maskedSecret()).isEqualTo("sk-****ret");
        assertThat(view.toString()).doesNotContain("sk-live-secret");
        var saved = captureSavedCredential();
        assertThat(saved.getEncryptedSecret()).startsWith("enc:v1:");
        assertThat(saved.getEncryptedSecret()).doesNotContain("sk-live-secret");
        assertThat(saved.getMetadata()).doesNotContainKey("secretNote");
    }

    @Test
    void creativeToolSavesMasterControlledCreditCostPolicy() {
        when(toolRepository.existsByToolCodeAndDeletedFalse("SOCIAL_POST_BUILDER")).thenReturn(false);
        when(toolRepository.save(any(CreativeTool.class))).thenAnswer(invocation -> withId(invocation.getArgument(0), TOOL_ID));
        when(toolCapabilityRepository.findAllByToolIdAndDeletedFalseOrderByCapabilityCodeAsc(TOOL_ID)).thenReturn(List.of());
        when(costPolicyRepository.findAllByToolIdAndDeletedFalseOrderByPolicyCodeAsc(TOOL_ID)).thenReturn(List.of());

        var view = service.createTool(new CreativeToolCommand(
                "social-post-builder",
                "Social Post Builder",
                CreativeToolCategory.SOCIAL_POST,
                true,
                "Creates post creative",
                List.of(new CreativeToolCommand.CreativeToolCapabilityCommand("square-image", true, Map.of())),
                new CreativeToolCommand.ToolCreditCostPolicyCommand("standard-cost", BigDecimal.valueOf(2), true, null, null, Map.of()),
                Map.of()));

        assertThat(view.toolCode()).isEqualTo("SOCIAL_POST_BUILDER");
        verify(costPolicyRepository).save(any(ToolCreditCostPolicy.class));
    }

    @Test
    void disabledProviderCannotBeUsedInRoutingPolicy() {
        when(toolRepository.findByIdAndDeletedFalse(TOOL_ID)).thenReturn(Optional.of(tool()));
        when(providerRepository.findByIdAndDeletedFalse(PRIMARY_ID)).thenReturn(Optional.of(provider(PRIMARY_ID, "OPENAI", false)));

        assertThatThrownBy(() -> service.createRoutingPolicy(routingCommand(PRIMARY_ID, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Disabled AI provider cannot be used");
    }

    @Test
    void activeRoutingPolicyRequiresConfiguredProviderCredential() {
        when(toolRepository.findByIdAndDeletedFalse(TOOL_ID)).thenReturn(Optional.of(tool()));
        when(providerRepository.findByIdAndDeletedFalse(PRIMARY_ID)).thenReturn(Optional.of(provider(PRIMARY_ID, "OPENAI", true)));
        when(credentialRepository.findFirstByProviderIdAndActiveTrueAndDeletedFalseOrderByUpdatedAtDesc(PRIMARY_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createRoutingPolicy(routingCommand(PRIMARY_ID, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Active routing policy requires configured provider credentials");
    }

    @Test
    void fallbackProviderSelectedWhenPrimaryCircuitIsOpen() {
        when(toolRepository.findByIdAndDeletedFalse(TOOL_ID)).thenReturn(Optional.of(tool()));
        when(providerRepository.findByIdAndDeletedFalse(PRIMARY_ID)).thenReturn(Optional.of(provider(PRIMARY_ID, "OPENAI", true)));
        when(providerRepository.findByIdAndDeletedFalse(FALLBACK_ID)).thenReturn(Optional.of(provider(FALLBACK_ID, "GEMINI", true)));
        var policy = com.lebhas.ai.domain.ProviderRoutingPolicy.create(
                "primary-route",
                TOOL_ID,
                "default",
                PRIMARY_ID,
                null,
                FALLBACK_ID,
                null,
                1,
                true,
                3,
                Map.of());
        withId(policy, UUID.randomUUID());
        when(routingPolicyRepository.findAllByToolIdAndQualityModeAndEnabledTrueAndDeletedFalseOrderByPriorityOrderAsc(TOOL_ID, "DEFAULT"))
                .thenReturn(List.of(policy));
        when(healthSnapshotRepository.findFirstByProviderIdAndDeletedFalseOrderByLastCheckedAtDesc(PRIMARY_ID))
                .thenReturn(Optional.of(ProviderHealthSnapshot.create(PRIMARY_ID, "degraded", 3, true, CLOCK.instant(), "timeout", Map.of())));
        when(healthSnapshotRepository.findFirstByProviderIdAndDeletedFalseOrderByLastCheckedAtDesc(FALLBACK_ID)).thenReturn(Optional.empty());
        when(credentialRepository.findFirstByProviderIdAndActiveTrueAndDeletedFalseOrderByUpdatedAtDesc(FALLBACK_ID))
                .thenReturn(Optional.of(configuredCredential(FALLBACK_ID)));

        ResolvedProviderRouteView route = service.resolveProvider(TOOL_ID, "default");

        assertThat(route.providerId()).isEqualTo(FALLBACK_ID);
        assertThat(route.fallbackSelected()).isTrue();
    }

    @Test
    void activeProviderWithoutConfiguredCredentialIsNotResolved() {
        when(toolRepository.findByIdAndDeletedFalse(TOOL_ID)).thenReturn(Optional.of(tool()));
        when(providerRepository.findByIdAndDeletedFalse(PRIMARY_ID)).thenReturn(Optional.of(provider(PRIMARY_ID, "OPENAI", true)));
        var policy = com.lebhas.ai.domain.ProviderRoutingPolicy.create(
                "primary-route",
                TOOL_ID,
                "default",
                PRIMARY_ID,
                null,
                null,
                null,
                1,
                true,
                3,
                Map.of());
        withId(policy, UUID.randomUUID());
        when(routingPolicyRepository.findAllByToolIdAndQualityModeAndEnabledTrueAndDeletedFalseOrderByPriorityOrderAsc(TOOL_ID, "DEFAULT"))
                .thenReturn(List.of(policy));
        when(credentialRepository.findFirstByProviderIdAndActiveTrueAndDeletedFalseOrderByUpdatedAtDesc(PRIMARY_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resolveProvider(TOOL_ID, "default"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("No active AI provider with configured credentials is available");
    }

    @Test
    void mockProviderRemainsDefaultWhenNoRoutingPolicyExists() {
        when(toolRepository.findByIdAndDeletedFalse(TOOL_ID)).thenReturn(Optional.of(tool()));
        when(routingPolicyRepository.findAllByToolIdAndQualityModeAndEnabledTrueAndDeletedFalseOrderByPriorityOrderAsc(TOOL_ID, "DEFAULT"))
                .thenReturn(List.of());
        when(providerRepository.findByProviderCodeAndDeletedFalse("MOCK")).thenReturn(Optional.of(provider(MOCK_ID, "MOCK", true)));

        ResolvedProviderRouteView route = service.resolveProvider(TOOL_ID, null);

        assertThat(route.providerId()).isEqualTo(MOCK_ID);
        assertThat(route.reason()).isEqualTo("mock_default");
    }

    private MasterAiProviderToolRegistryService service() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("ai.credentials.encryption-key", "0123456789abcde!");
        return new MasterAiProviderToolRegistryService(
                providerRepository,
                modelRepository,
                credentialRepository,
                toolRepository,
                toolCapabilityRepository,
                costPolicyRepository,
                routingPolicyRepository,
                healthSnapshotRepository,
                new AiCredentialEncryptionService(environment),
                null,
                CLOCK);
    }

    private ProviderRoutingPolicyCommand routingCommand(UUID providerId, UUID fallbackProviderId) {
        return new ProviderRoutingPolicyCommand(
                "standard-route",
                TOOL_ID,
                "default",
                providerId,
                null,
                fallbackProviderId,
                null,
                1,
                true,
                3,
                Map.of());
    }

    private CreativeTool tool() {
        CreativeTool tool = CreativeTool.create("social-post-builder", "Social Post Builder", CreativeToolCategory.SOCIAL_POST, true, null, Map.of());
        return withId(tool, TOOL_ID);
    }

    private AiToolProvider provider(UUID id, String code, boolean enabled) {
        AiToolProvider provider = AiToolProvider.create(
                code,
                code + " Provider",
                "MOCK".equals(code) ? ProviderType.MOCK : ProviderType.MULTIMODAL_GENERATION,
                enabled ? ProviderStatus.ACTIVE : ProviderStatus.DISABLED,
                enabled,
                List.of("SOCIAL_POST"),
                null,
                true,
                true,
                true,
                Map.of(),
                Map.of(),
                Map.of());
        return withId(provider, id);
    }

    private AiProviderCredential configuredCredential(UUID providerId) {
        AiProviderCredential credential = AiProviderCredential.createProviderCredential(
                providerId,
                ProviderEnvironment.SANDBOX,
                "enc:v1:test",
                "sk-****test",
                null,
                true,
                Map.of());
        return withId(credential, UUID.randomUUID());
    }

    private AiProviderCredential captureSavedCredential() {
        org.mockito.ArgumentCaptor<AiProviderCredential> captor = org.mockito.ArgumentCaptor.forClass(AiProviderCredential.class);
        verify(credentialRepository).save(captor.capture());
        return captor.getValue();
    }

    private <T> T withId(T entity, UUID id) {
        ReflectionTestUtils.setField(entity, "id", id);
        return entity;
    }
}
