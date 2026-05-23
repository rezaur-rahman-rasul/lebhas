package com.lebhas.creativesaas.generation.application;

import com.lebhas.creativesaas.generation.provider.AiGenerationRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class CreativeGenerationPromptRenderer {

    public String render(GenerationWorkItem workItem) {
        StringBuilder prompt = new StringBuilder(4096);
        prompt.append("Generate a high-converting advertising creative.\n");
        prompt.append("Creative type: ").append(workItem.creativeType()).append('\n');
        prompt.append("Platform: ").append(workItem.platform()).append('\n');
        prompt.append("Campaign objective: ").append(workItem.campaignObjective()).append('\n');
        prompt.append("Language: ").append(workItem.language()).append('\n');
        if (workItem.width() != null && workItem.height() != null) {
            prompt.append("Canvas: ").append(workItem.width()).append('x').append(workItem.height()).append('\n');
        }
        if (StringUtils.hasText(workItem.brandContextSnapshot()) && !"{}".equals(workItem.brandContextSnapshot())) {
            prompt.append("Brand context JSON: ").append(workItem.brandContextSnapshot()).append('\n');
        }
        if (StringUtils.hasText(workItem.assetContextSnapshot()) && !"[]".equals(workItem.assetContextSnapshot())) {
            prompt.append("Asset context JSON: ").append(workItem.assetContextSnapshot()).append('\n');
        }
        if (StringUtils.hasText(workItem.generationConfig()) && !"{}".equals(workItem.generationConfig())) {
            prompt.append("Generation config JSON: ").append(workItem.generationConfig()).append('\n');
        }
        prompt.append("Creative prompt: ").append(workItem.providerPrompt());
        return prompt.toString();
    }

    public AiGenerationRequest toProviderRequest(GenerationWorkItem workItem) {
        return new AiGenerationRequest(
                workItem.workspaceId(),
                workItem.requestId(),
                workItem.creativeType(),
                workItem.platform(),
                workItem.campaignObjective(),
                workItem.outputFormat(),
                workItem.language(),
                render(workItem),
                workItem.brandContextSnapshot(),
                workItem.assetContextSnapshot(),
                workItem.generationConfigMap(),
                workItem.width(),
                workItem.height(),
                workItem.duration());
    }
}
