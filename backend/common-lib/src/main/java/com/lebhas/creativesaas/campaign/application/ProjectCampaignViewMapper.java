package com.lebhas.creativesaas.campaign.application;

import com.lebhas.creativesaas.campaign.application.dto.ProjectCampaignView;
import com.lebhas.creativesaas.campaign.domain.ProjectCampaignEntity;
import org.springframework.stereotype.Component;

@Component
public class ProjectCampaignViewMapper {

    public ProjectCampaignView toView(ProjectCampaignEntity entity) {
        return new ProjectCampaignView(
                entity.getId(),
                entity.getWorkspaceId(),
                entity.getBrandId(),
                entity.getProductServiceId(),
                entity.getCreatedByUserId(),
                entity.getName(),
                entity.getDescription(),
                entity.getCampaignObjective(),
                entity.getTargetPlatform(),
                entity.getCampaignType(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
