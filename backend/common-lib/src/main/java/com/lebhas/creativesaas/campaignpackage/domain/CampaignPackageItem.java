package com.lebhas.creativesaas.campaignpackage.domain;

import com.lebhas.creativesaas.common.audit.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "campaign_package_items", schema = "platform")
public class CampaignPackageItem extends TenantAwareEntity {

    @Column(name = "campaign_package_id", nullable = false)
    private UUID campaignPackageId;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false, length = 40)
    private CampaignPackageItemType itemType;

    @Column(name = "item_id", nullable = false)
    private UUID itemId;

    protected CampaignPackageItem() {
    }

    public static CampaignPackageItem create(UUID workspaceId, UUID packageId, UUID projectId, CampaignPackageItemType type, UUID itemId) {
        CampaignPackageItem item = new CampaignPackageItem();
        item.assignWorkspace(workspaceId);
        item.campaignPackageId = packageId;
        item.projectId = projectId;
        item.itemType = type;
        item.itemId = itemId;
        return item;
    }

    public UUID getCampaignPackageId() { return campaignPackageId; }
    public UUID getProjectId() { return projectId; }
    public CampaignPackageItemType getItemType() { return itemType; }
    public UUID getItemId() { return itemId; }
}
