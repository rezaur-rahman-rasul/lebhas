package com.lebhas.creativesaas.imagecreative.application.dto;

import com.lebhas.creativesaas.generatedversion.application.dto.GeneratedVersionView;
import com.lebhas.creativesaas.generation.application.dto.CreativePipelineRunView;

import java.util.List;

public record ProductImageCreativeGenerationResult(
        ImageCreativeGenerationView generation,
        List<GeneratedVersionView> generatedVersions,
        CreativePipelineRunView pipeline
) {
}
