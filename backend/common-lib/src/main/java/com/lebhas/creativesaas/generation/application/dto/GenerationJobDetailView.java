package com.lebhas.creativesaas.generation.application.dto;

import com.lebhas.creativesaas.generatedversion.application.dto.GeneratedVersionView;

import java.util.List;

public record GenerationJobDetailView(
        GenerationJobView job,
        List<GeneratedVersionView> generatedVersions
) {
}
