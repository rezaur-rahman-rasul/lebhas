package com.lebhas.creativesaas.project.application;

import com.lebhas.creativesaas.project.application.dto.ProjectView;
import com.lebhas.creativesaas.project.domain.ProjectEntity;
import org.springframework.stereotype.Component;

@Component
public class ProjectViewMapper {

    public ProjectView toView(ProjectEntity project) {
        return new ProjectView(
                project.getId(),
                project.getWorkspaceId(),
                project.getBrandId(),
                project.getName(),
                project.getDescription(),
                project.getCampaignObjective(),
                project.getTargetPlatform(),
                project.getStatus(),
                project.getCreatedBy(),
                project.getCreatedAt(),
                project.getUpdatedAt());
    }
}
