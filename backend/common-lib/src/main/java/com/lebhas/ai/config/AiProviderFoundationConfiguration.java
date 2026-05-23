package com.lebhas.ai.config;

import com.lebhas.ai.job.AiGenerationJobService;
import com.lebhas.ai.application.AiProviderRegistryMapper;
import com.lebhas.ai.application.CreativePipelineMapper;
import com.lebhas.ai.application.MasterAiProviderManagementService;
import com.lebhas.ai.application.MasterCreativePipelineManagementService;
import com.lebhas.ai.infrastructure.persistence.AiModelRepository;
import com.lebhas.ai.infrastructure.persistence.AiToolCapabilityRepository;
import com.lebhas.ai.infrastructure.persistence.AiToolProviderRepository;
import com.lebhas.ai.infrastructure.persistence.CreativePipelineLayerRepository;
import com.lebhas.ai.infrastructure.persistence.CreativePipelineRepository;
import com.lebhas.ai.infrastructure.persistence.LayerCostPolicyRepository;
import com.lebhas.ai.infrastructure.persistence.LayerQualityPolicyRepository;
import com.lebhas.ai.infrastructure.persistence.LayerRoutingPolicyRepository;
import com.lebhas.ai.infrastructure.persistence.LayerToolMappingRepository;
import com.lebhas.ai.provider.AiProvider;
import com.lebhas.ai.provider.AiProviderRouter;
import com.lebhas.ai.provider.GeminiProviderFoundation;
import com.lebhas.ai.provider.MockAiProviderForTests;
import com.lebhas.ai.provider.OpenAiProviderFoundation;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

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
            AiProviderRegistryMapper mapper
    ) {
        return new MasterAiProviderManagementService(providerRepository, modelRepository, capabilityRepository, mapper);
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
