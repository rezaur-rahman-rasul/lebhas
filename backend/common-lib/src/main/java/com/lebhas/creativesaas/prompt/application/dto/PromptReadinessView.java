package com.lebhas.creativesaas.prompt.application.dto;

import java.util.List;

public record PromptReadinessView(
        boolean ready,
        List<String> blockingReasons,
        List<String> warnings
) {
    public static PromptReadinessView ready(List<String> warnings) {
        return new PromptReadinessView(true, List.of(), warnings == null ? List.of() : List.copyOf(warnings));
    }
}
