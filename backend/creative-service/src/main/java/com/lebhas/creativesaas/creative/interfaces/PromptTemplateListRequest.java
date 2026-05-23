package com.lebhas.creativesaas.creative.interfaces;

import com.lebhas.creativesaas.prompt.domain.CampaignObjective;
import com.lebhas.creativesaas.prompt.domain.PromptLanguage;
import com.lebhas.creativesaas.prompt.domain.PromptPlatform;
import com.lebhas.creativesaas.prompt.domain.PromptTemplateStatus;

public class PromptTemplateListRequest {

    private String category;
    private PromptPlatform platform;
    private CampaignObjective campaignObjective;
    private PromptLanguage language;
    private String businessType;
    private PromptTemplateStatus status;
    private String search;
    private Boolean systemDefault;
    private Boolean includeSystemDefaults = true;

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
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

    public PromptLanguage getLanguage() {
        return language;
    }

    public void setLanguage(PromptLanguage language) {
        this.language = language;
    }

    public String getBusinessType() {
        return businessType;
    }

    public void setBusinessType(String businessType) {
        this.businessType = businessType;
    }

    public PromptTemplateStatus getStatus() {
        return status;
    }

    public void setStatus(PromptTemplateStatus status) {
        this.status = status;
    }

    public String getSearch() {
        return search;
    }

    public void setSearch(String search) {
        this.search = search;
    }

    public Boolean getSystemDefault() {
        return systemDefault;
    }

    public void setSystemDefault(Boolean systemDefault) {
        this.systemDefault = systemDefault;
    }

    public Boolean getIncludeSystemDefaults() {
        return includeSystemDefaults;
    }

    public void setIncludeSystemDefaults(Boolean includeSystemDefaults) {
        this.includeSystemDefaults = includeSystemDefaults;
    }
}
