package com.lebhas.creativesaas.creative.interfaces;

import com.lebhas.creativesaas.generation.domain.CreativeGenerationStatus;
import com.lebhas.creativesaas.generation.domain.CreativeType;
import com.lebhas.creativesaas.prompt.domain.PromptPlatform;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.util.UUID;

public class CreativeGenerationListRequest {

    private UUID userId;
    private CreativeGenerationStatus status;
    private CreativeType creativeType;
    private PromptPlatform platform;

    @Min(value = 0, message = "Page index must be zero or greater")
    private Integer page = 0;

    @Min(value = 1, message = "Page size must be at least 1")
    @Max(value = 100, message = "Page size must not exceed 100")
    private Integer size = 20;

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public CreativeGenerationStatus getStatus() {
        return status;
    }

    public void setStatus(CreativeGenerationStatus status) {
        this.status = status;
    }

    public CreativeType getCreativeType() {
        return creativeType;
    }

    public void setCreativeType(CreativeType creativeType) {
        this.creativeType = creativeType;
    }

    public PromptPlatform getPlatform() {
        return platform;
    }

    public void setPlatform(PromptPlatform platform) {
        this.platform = platform;
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }
}
