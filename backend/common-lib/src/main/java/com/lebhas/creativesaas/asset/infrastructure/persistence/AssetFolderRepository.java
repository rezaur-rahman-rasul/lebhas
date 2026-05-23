package com.lebhas.creativesaas.asset.infrastructure.persistence;

import com.lebhas.creativesaas.asset.domain.AssetFolderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AssetFolderRepository extends JpaRepository<AssetFolderEntity, UUID> {

    Optional<AssetFolderEntity> findByIdAndWorkspaceIdAndDeletedFalse(UUID id, UUID workspaceId);

    List<AssetFolderEntity> findAllByWorkspaceIdAndDeletedFalseOrderByNameAsc(UUID workspaceId);

    boolean existsByWorkspaceIdAndParentFolderIdAndNameIgnoreCaseAndDeletedFalse(
            UUID workspaceId,
            UUID parentFolderId,
            String name
    );

    boolean existsByWorkspaceIdAndParentFolderIdAndNameIgnoreCaseAndIdNotAndDeletedFalse(
            UUID workspaceId,
            UUID parentFolderId,
            String name,
            UUID id
    );

    long countByWorkspaceIdAndParentFolderIdAndDeletedFalse(UUID workspaceId, UUID parentFolderId);
}
