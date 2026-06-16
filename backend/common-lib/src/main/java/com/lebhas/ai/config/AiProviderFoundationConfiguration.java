package com.lebhas.ai.config;

import com.lebhas.ai.job.AiGenerationJobService;
import com.lebhas.ai.application.AiCredentialEncryptionService;
import com.lebhas.ai.application.AiProviderRegistryMapper;
import com.lebhas.ai.application.CreativePipelineMapper;
import com.lebhas.ai.application.MasterAiProviderManagementService;
import com.lebhas.ai.application.MasterAiProviderToolRegistryService;
import com.lebhas.ai.application.MasterCreativePipelineManagementService;
import com.lebhas.ai.application.OpenAiCostTrackingService;
import com.lebhas.ai.application.MasterProviderSettingsService;
import com.lebhas.ai.application.OpenAiCostSyncScheduler;
import com.lebhas.ai.credit.application.CreditValuePolicyService;
import com.lebhas.ai.credit.application.ProviderCreditPoolService;
import com.lebhas.ai.infrastructure.persistence.AiModelRepository;
import com.lebhas.ai.infrastructure.persistence.AiProviderCredentialRepository;
import com.lebhas.ai.infrastructure.persistence.AiToolCapabilityRepository;
import com.lebhas.ai.infrastructure.persistence.AiToolProviderRepository;
import com.lebhas.ai.infrastructure.persistence.CreativeToolCapabilityRepository;
import com.lebhas.ai.infrastructure.persistence.CreativeToolRepository;
import com.lebhas.ai.infrastructure.persistence.CreativePipelineLayerRepository;
import com.lebhas.ai.infrastructure.persistence.CreativePipelineRepository;
import com.lebhas.ai.infrastructure.persistence.LayerCostPolicyRepository;
import com.lebhas.ai.infrastructure.persistence.LayerQualityPolicyRepository;
import com.lebhas.ai.infrastructure.persistence.LayerRoutingPolicyRepository;
import com.lebhas.ai.infrastructure.persistence.LayerToolMappingRepository;
import com.lebhas.ai.infrastructure.persistence.ProviderHealthSnapshotRepository;
import com.lebhas.ai.infrastructure.persistence.ProviderRoutingPolicyRepository;
import com.lebhas.ai.infrastructure.persistence.ToolCreditCostPolicyRepository;
import com.lebhas.creativesaas.asset.application.AssetEventPublisher;
import com.lebhas.ai.provider.AiProvider;
import com.lebhas.ai.provider.AiProviderRouter;
import com.lebhas.ai.provider.GeminiProviderFoundation;
import com.lebhas.ai.provider.MockAiProviderForTests;
import com.lebhas.ai.provider.OpenAiProviderFoundation;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import com.lebhas.creativesaas.auditlog.application.AuditLogService;

import java.time.Clock;
import java.util.List;

@Configuration
@Import(AiRedisFoundationConfiguration.class)
@EnableConfigurationProperties({AiProviderProperties.class, AiProviderRegistryProperties.class})
public class AiProviderFoundationConfiguration {

    @Bean
    AiProviderRegistryMapper aiProviderRegistryMapper(AiProviderRegistryProperties properties) {
        return new AiProviderRegistryMapper(properties);
    }

    @Bean
    CreativePipelineMapper creativePipelineMapper() {
        return new CreativePipelineMapper();
    }

    @Bean
    MasterAiProviderManagementService masterAiProviderManagementService(
            AiToolProviderRepository providerRepository,
            AiModelRepository modelRepository,
            AiToolCapabilityRepository capabilityRepository,
            AiProviderRegistryMapper mapper,
            ObjectProvider<AssetEventPublisher> eventPublisher
    ) {
        return new MasterAiProviderManagementService(
                providerRepository,
                modelRepository,
                capabilityRepository,
                mapper,
                eventPublisher.getIfAvailable());
    }

    @Bean
    AiCredentialEncryptionService aiCredentialEncryptionService(Environment environment) {
        return new AiCredentialEncryptionService(environment);
    }

    @Bean
    MasterAiProviderToolRegistryService masterAiProviderToolRegistryService(
            AiToolProviderRepository providerRepository,
            AiModelRepository modelRepository,
            AiProviderCredentialRepository credentialRepository,
            CreativeToolRepository toolRepository,
            CreativeToolCapabilityRepository toolCapabilityRepository,
            ToolCreditCostPolicyRepository costPolicyRepository,
            ProviderRoutingPolicyRepository routingPolicyRepository,
            ProviderHealthSnapshotRepository healthSnapshotRepository,
            AiCredentialEncryptionService encryptionService,
            ObjectProvider<AssetEventPublisher> eventPublisher,
            Clock clock
    ) {
        return new MasterAiProviderToolRegistryService(
                providerRepository,
                modelRepository,
                credentialRepository,
                toolRepository,
                toolCapabilityRepository,
                costPolicyRepository,
                routingPolicyRepository,
                healthSnapshotRepository,
                encryptionService,
                eventPublisher.getIfAvailable(),
                clock);
    }

    @Bean
    MasterProviderSettingsService masterProviderSettingsService(
            AiToolProviderRepository providerRepository,
            AiProviderCredentialRepository credentialRepository,
            AiCredentialEncryptionService encryptionService,
            ObjectProvider<AssetEventPublisher> eventPublisher,
            ObjectProvider<AuditLogService> auditLogService,
            ObjectProvider<CreditValuePolicyService> creditValuePolicyService,
            Clock clock
    ) {
        return new MasterProviderSettingsService(
                providerRepository,
                credentialRepository,
                encryptionService,
                eventPublisher.getIfAvailable(),
                auditLogService.getIfAvailable(),
                creditValuePolicyService,
                clock);
    }

    @Bean
    OpenAiCostTrackingService openAiCostTrackingService(
            AiToolProviderRepository providerRepository,
            AiCredentialEncryptionService encryptionService,
            ObjectProvider<CreditValuePolicyService> creditValuePolicyService,
            ObjectProvider<ProviderCreditPoolService> providerCreditPoolService,
            ObjectProvider<AuditLogService> auditLogService,
            Clock clock
    ) {
        return new OpenAiCostTrackingService(
                providerRepository,
                encryptionService,
                creditValuePolicyService,
                providerCreditPoolService,
                auditLogService.getIfAvailable(),
                clock);
    }

    @Bean
    OpenAiCostSyncScheduler openAiCostSyncScheduler(OpenAiCostTrackingService costTrackingService) {
        return new OpenAiCostSyncScheduler(costTrackingService);
    }

    @Bean
    MasterCreativePipelineManagementService masterCreativePipelineManagementService(
            CreativePipelineRepository pipelineRepository,
            CreativePipelineLayerRepository layerRepository,
            LayerToolMappingRepository toolMappingRepository,
            LayerRoutingPolicyRepository routingPolicyRepository,
            LayerCostPolicyRepository costPolicyRepository,
            LayerQualityPolicyRepository qualityPolicyRepository,
            AiToolProviderRepository providerRepository,
            AiModelRepository modelRepository,
            AiToolCapabilityRepository capabilityRepository,
            CreativePipelineMapper mapper
    ) {
        return new MasterCreativePipelineManagementService(
                pipelineRepository,
                layerRepository,
                toolMappingRepository,
                routingPolicyRepository,
                costPolicyRepository,
                qualityPolicyRepository,
                providerRepository,
                modelRepository,
                capabilityRepository,
                mapper);
    }

    @Bean
    OpenAiProviderFoundation openAiProviderFoundation(AiProviderProperties properties) {
        return new OpenAiProviderFoundation(properties);
    }

    @Bean
    GeminiProviderFoundation geminiProviderFoundation(AiProviderProperties properties) {
        return new GeminiProviderFoundation(properties);
    }

    @Bean
    MockAiProviderForTests mockAiProviderForTests(AiProviderProperties properties) {
        return new MockAiProviderForTests(properties);
    }

    @Bean
    AiProviderRouter generationAiProviderRouter(List<AiProvider> providers, AiProviderProperties properties) {
        return new AiProviderRouter(providers, properties);
    }

    @Bean
    AiGenerationJobService aiGenerationJobService(AiProviderRouter aiProviderRouter, AiProviderProperties properties) {
        return new AiGenerationJobService(aiProviderRouter, properties);
    }
}
