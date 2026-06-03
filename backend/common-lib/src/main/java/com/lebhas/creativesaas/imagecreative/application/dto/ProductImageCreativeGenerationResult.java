package com.lebhas.creativesaas.imagecreative.application.dto;

import com.lebhas.creativesaas.generatedversion.application.dto.GeneratedVersionView;

import java.util.List;

public record ProductImageCreativeGenerationResult(
        ImageCreativeGenerationView generation,
        List<GeneratedVersionView> generatedVersions
) {
}
