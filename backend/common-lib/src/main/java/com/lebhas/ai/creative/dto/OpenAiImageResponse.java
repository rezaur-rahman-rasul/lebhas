package com.lebhas.ai.creative.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record OpenAiImageResponse(
        Long created,
        List<ImageData> data
) {
    public record ImageData(
            @JsonProperty("b64_json")
            String b64Json,
            @JsonProperty("revised_prompt")
            String revisedPrompt
    ) {
    }
}
