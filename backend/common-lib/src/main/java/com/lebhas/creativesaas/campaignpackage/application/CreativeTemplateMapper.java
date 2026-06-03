package com.lebhas.creativesaas.campaignpackage.application;

import com.lebhas.creativesaas.campaignpackage.application.dto.BulkGenerationJobView;
import com.lebhas.creativesaas.campaignpackage.application.dto.CampaignPackageView;
import com.lebhas.creativesaas.campaignpackage.application.dto.CreativeTemplateView;
import com.lebhas.creativesaas.campaignpackage.domain.BulkGenerationJob;
import com.lebhas.creativesaas.campaignpackage.domain.CampaignPackage;
import com.lebhas.creativesaas.campaignpackage.domain.CampaignPackageItem;
import com.lebhas.creativesaas.campaignpackage.domain.CreativeTemplate;
import com.lebhas.creativesaas.campaignpackage.domain.CreativeTemplateStatus;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CreativeTemplateMapper {
    public CreativeTemplateView toView(CreativeTemplate template) {
        return new CreativeTemplateView(
                template.getId(),
                template.getWorkspaceId(),
                template.getName(),
                template.getCategory(),
                template.getDescription(),
                template.getPlatform(),
                template.getLanguage(),
                template.getCampaignObjective(),
                template.isMasterTemplate(),
                template.isActive() ? CreativeTemplateStatus.ACTIVE : CreativeTemplateStatus.INACTIVE,
                template.getTemplatePayload(),
                template.getCreatedAt(),
                template.getUpdatedAt());
    }

    public CampaignPackageView toView(CampaignPackage pack, List<CampaignPackageItem> items) {
        return new CampaignPackageView(
                pack.getId(),
                pack.getWorkspaceId(),
                pack.getProjectId(),
                pack.getName(),
                pack.getDescription(),
                pack.getStatus(),
                pack.getR2ObjectKey(),
                pack.getExportUrl(),
                pack.getExportUrlExpiresAt(),
                items.stream()
                        .map(item -> new CampaignPackageView.ItemView(item.getId(), item.getItemType(), item.getItemId()))
                        .toList());
    }

    public BulkGenerationJobView toView(BulkGenerationJob job) {
        return new BulkGenerationJobView(
                job.getId(),
                job.getWorkspaceId(),
                job.getProjectId(),
                job.getGenerationType(),
                job.getPlatform(),
                job.getLanguage(),
                job.getItemCount(),
                job.getEstimatedCredits(),
                job.getStatus(),
                job.getRequestPayload());
    }
}
