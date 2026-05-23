package com.lebhas.creativesaas.asset.domain;

import com.lebhas.creativesaas.common.audit.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "asset_folders", schema = "platform")
public class AssetFolderEntity extends TenantAwareEntity {

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "parent_folder_id")
    private UUID parentFolderId;

    @Column(name = "description", length = 500)
    private String description;

    protected AssetFolderEntity() {
    }

    public static AssetFolderEntity create(UUID workspaceId, String name, UUID parentFolderId, String description) {
        AssetFolderEntity folder = new AssetFolderEntity();
        folder.assignWorkspace(workspaceId);
        folder.name = name;
        folder.parentFolderId = parentFolderId;
        folder.description = description;
        return folder;
    }

    public String getName() {
        return name;
    }

    public UUID getParentFolderId() {
        return parentFolderId;
    }

    public String getDescription() {
        return description;
    }

    public void update(String name, UUID parentFolderId, String description) {
        this.name = name;
        this.parentFolderId = parentFolderId;
        this.description = description;
    }
}
