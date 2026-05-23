package com.lebhas.creativesaas.generation.provider;

import com.lebhas.creativesaas.generation.domain.CreativeOutputFormat;
import com.lebhas.creativesaas.generation.domain.CreativeType;
import com.lebhas.creativesaas.prompt.domain.CampaignObjective;
import com.lebhas.creativesaas.prompt.domain.PromptLanguage;
import com.lebhas.creativesaas.prompt.domain.PromptPlatform;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MockImageProviderTest {

    private final MockImageProvider provider = new MockImageProvider();

    @Test
    void shouldGenerateSyntheticPngImage() {
        AiGenerationResponse response = provider.generate(new AiGenerationRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                CreativeType.STATIC_IMAGE,
                PromptPlatform.FACEBOOK,
                CampaignObjective.SALES,
                CreativeOutputFormat.PNG,
                PromptLanguage.ENGLISH,
                "Summer jacket launch campaign",
                null,
                null,
                Map.of(),
                640,
                640,
                null));

        assertThat(response.providerName()).isEqualTo("MOCK");
        assertThat(response.outputFormat()).isEqualTo(CreativeOutputFormat.PNG);
        assertThat(response.mimeType()).isEqualTo("image/png");
        assertThat(response.content()).isNotEmpty();
        assertThat(response.width()).isEqualTo(640);
        assertThat(response.height()).isEqualTo(640);
    }

    @Test
    void shouldFallbackToPngWhenWebpWriterIsUnavailable() {
        AiGenerationResponse response = provider.generate(new AiGenerationRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                CreativeType.STATIC_IMAGE,
                PromptPlatform.INSTAGRAM,
                CampaignObjective.ENGAGEMENT,
                CreativeOutputFormat.WEBP,
                PromptLanguage.BANGLA,
                "Festive saree collection",
                null,
                null,
                Map.of(),
                512,
                512,
                null));

        assertThat(response.outputFormat()).isEqualTo(CreativeOutputFormat.PNG);
        assertThat(response.metadata()).containsEntry("requestedOutputFormat", "WEBP");
    }
}
