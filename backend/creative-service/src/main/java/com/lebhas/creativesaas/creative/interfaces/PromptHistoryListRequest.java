package com.lebhas.creativesaas.creative.interfaces;

import com.lebhas.creativesaas.prompt.domain.CampaignObjective;
import com.lebhas.creativesaas.prompt.domain.PromptHistoryStatus;
import com.lebhas.creativesaas.prompt.domain.PromptPlatform;
import com.lebhas.creativesaas.prompt.domain.SuggestionType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.Instant;
import java.util.UUID;

public class PromptHistoryListRequest {

    private UUID userId;
    private SuggestionType suggestionType;
    private PromptPlatform platform;
    private CampaignObjective campaignObjective;
    private PromptHistoryStatus status;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private Instant createdFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private Instant createdTo;

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

    public SuggestionType getSuggestionType() {
        return suggestionType;
    }

    public void setSuggestionType(SuggestionType suggestionType) {
        this.suggestionType = suggestionType;
    }

    public PromptPlatform getPlatform() {
        return platform;
    }

    public void setPlatform(PromptPlatform platform) {
        this.platform = platform;
    }

    public CampaignObjective getCampaignObjective() {
        return campaignObjective;
    }

    public void setCampaignObjective(CampaignObjective campaignObjective) {
        this.campaignObjective = campaignObjective;
    }

    public PromptHistoryStatus getStatus() {
        return status;
    }

    public void setStatus(PromptHistoryStatus status) {
        this.status = status;
    }

    public Instant getCreatedFrom() {
        return createdFrom;
    }

    public void setCreatedFrom(Instant createdFrom) {
        this.createdFrom = createdFrom;
    }

    public Instant getCreatedTo() {
        return createdTo;
    }

    public void setCreatedTo(Instant createdTo) {
        this.createdTo = createdTo;
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
