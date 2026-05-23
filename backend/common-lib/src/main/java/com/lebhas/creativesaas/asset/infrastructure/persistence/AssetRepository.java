package com.lebhas.creativesaas.asset.infrastructure.persistence;

import com.lebhas.creativesaas.asset.domain.AssetEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AssetRepository extends JpaRepository<AssetEntity, UUID>, JpaSpecificationExecutor<AssetEntity> {

    Optional<AssetEntity> findByIdAndWorkspaceIdAndDeletedFalse(UUID id, UUID workspaceId);

    Optional<AssetEntity> findByIdAndDeletedFalse(UUID id);

    Optional<AssetEntity> findFirstByWorkspaceIdAndChecksumAndDeletedFalse(UUID workspaceId, String checksum);

    long countByWorkspaceIdAndDeletedFalse(UUID workspaceId);

    long countByWorkspaceIdAndFolderIdAndDeletedFalse(UUID workspaceId, UUID folderId);

    Optional<AssetEntity> findFirstByWorkspaceIdAndProjectIdAndStorageFileIdAndDeletedFalse(
            UUID workspaceId,
            UUID projectId,
            UUID storageFileId
    );

    List<AssetEntity> findAllByWorkspaceIdAndDeletedFalse(UUID workspaceId);

    long countByStorageFileIdAndDeletedFalse(UUID storageFileId);
}
