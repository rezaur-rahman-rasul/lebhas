package com.lebhas.creativesaas.asset.application;

import com.lebhas.creativesaas.asset.application.dto.AssetFolderView;
import com.lebhas.creativesaas.asset.application.dto.CreateAssetFolderCommand;
import com.lebhas.creativesaas.asset.application.dto.UpdateAssetFolderCommand;
import com.lebhas.creativesaas.asset.domain.AssetFolderEntity;
import com.lebhas.creativesaas.asset.infrastructure.persistence.AssetFolderRepository;
import com.lebhas.creativesaas.asset.infrastructure.persistence.AssetRepository;
import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.common.security.Permission;
import com.lebhas.creativesaas.identity.application.WorkspaceAuthorizationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;

@Service
public class AssetFolderService {

    private final WorkspaceAuthorizationService workspaceAuthorizationService;
    private final AssetFolderRepository assetFolderRepository;
    private final AssetRepository assetRepository;
    private final AssetViewMapper assetViewMapper;
    private final AssetActivityLogger assetActivityLogger;

    public AssetFolderService(
            WorkspaceAuthorizationService workspaceAuthorizationService,
            AssetFolderRepository assetFolderRepository,
            AssetRepository assetRepository,
            AssetViewMapper assetViewMapper,
            AssetActivityLogger assetActivityLogger
    ) {
        this.workspaceAuthorizationService = workspaceAuthorizationService;
        this.assetFolderRepository = assetFolderRepository;
        this.assetRepository = assetRepository;
        this.assetViewMapper = assetViewMapper;
        this.assetActivityLogger = assetActivityLogger;
    }

    @Transactional
    public AssetFolderView createFolder(CreateAssetFolderCommand command) {
        WorkspaceAuthorizationService.WorkspaceAccess access = workspaceAuthorizationService
                .requirePermission(command.workspaceId(), Permission.ASSET_FOLDER_MANAGE);
        UUID parentFolderId = validateParentFolder(command.workspaceId(), command.parentFolderId(), null);
        ensureNameAvailable(command.workspaceId(), parentFolderId, command.name(), null);
        AssetFolderEntity folder = AssetFolderEntity.create(
                command.workspaceId(),
                normalizeFolderName(command.name()),
                parentFolderId,
                normalizeDescription(command.description()));
        assetFolderRepository.save(folder);
        assetActivityLogger.logFolderCreated(command.workspaceId(), folder.getId(), access.currentUser().userId(), folder.getName());
        return assetViewMapper.toFolderView(folder);
    }

    @Transactional(readOnly = true)
    public List<AssetFolderView> listFolders(UUID workspaceId) {
        workspaceAuthorizationService.requirePermission(workspaceId, Permission.ASSET_VIEW);
        return assetFolderRepository.findAllByWorkspaceIdAndDeletedFalseOrderByNameAsc(workspaceId).stream()
                .map(assetViewMapper::toFolderView)
                .toList();
    }

    @Transactional
    public AssetFolderView updateFolder(UpdateAssetFolderCommand command) {
        WorkspaceAuthorizationService.WorkspaceAccess access = workspaceAuthorizationService
                .requirePermission(command.workspaceId(), Permission.ASSET_FOLDER_MANAGE);
        AssetFolderEntity folder = requireFolder(command.workspaceId(), command.folderId());
        UUID parentFolderId = validateParentFolder(command.workspaceId(), command.parentFolderId(), folder.getId());
        ensureNameAvailable(command.workspaceId(), parentFolderId, command.name(), folder.getId());
        folder.update(
                normalizeFolderName(command.name()),
                parentFolderId,
                normalizeDescription(command.description()));
        assetFolderRepository.save(folder);
        assetActivityLogger.logFolderUpdated(command.workspaceId(), folder.getId(), access.currentUser().userId(), folder.getName());
        return assetViewMapper.toFolderView(folder);
    }

    @Transactional
    public void deleteFolder(UUID workspaceId, UUID folderId) {
        WorkspaceAuthorizationService.WorkspaceAccess access = workspaceAuthorizationService
                .requirePermission(workspaceId, Permission.ASSET_FOLDER_MANAGE);
        AssetFolderEntity folder = requireFolder(workspaceId, folderId);
        if (assetRepository.countByWorkspaceIdAndFolderIdAndDeletedFalse(workspaceId, folderId) > 0
                || assetFolderRepository.countByWorkspaceIdAndParentFolderIdAndDeletedFalse(workspaceId, folderId) > 0) {
            throw new BusinessException(ErrorCode.ASSET_FOLDER_NOT_EMPTY);
        }
        folder.markDeleted();
        assetFolderRepository.save(folder);
        assetActivityLogger.logFolderDeleted(workspaceId, folderId, access.currentUser().userId());
    }

    @Transactional(readOnly = true)
    public AssetFolderEntity requireFolder(UUID workspaceId, UUID folderId) {
        return assetFolderRepository.findByIdAndWorkspaceIdAndDeletedFalse(folderId, workspaceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ASSET_FOLDER_NOT_FOUND));
    }

    private UUID validateParentFolder(UUID workspaceId, UUID parentFolderId, UUID currentFolderId) {
        if (parentFolderId == null) {
            return null;
        }
        if (parentFolderId.equals(currentFolderId)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "A folder cannot be its own parent");
        }
        return requireFolder(workspaceId, parentFolderId).getId();
    }

    private void ensureNameAvailable(UUID workspaceId, UUID parentFolderId, String name, UUID folderIdToIgnore) {
        String normalizedName = normalizeFolderName(name);
        boolean exists = folderIdToIgnore == null
                ? assetFolderRepository.existsByWorkspaceIdAndParentFolderIdAndNameIgnoreCaseAndDeletedFalse(
                workspaceId, parentFolderId, normalizedName)
                : assetFolderRepository.existsByWorkspaceIdAndParentFolderIdAndNameIgnoreCaseAndIdNotAndDeletedFalse(
                workspaceId, parentFolderId, normalizedName, folderIdToIgnore);
        if (exists) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "An asset folder with the same name already exists");
        }
    }

    private String normalizeFolderName(String name) {
        if (!StringUtils.hasText(name)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Folder name is required");
        }
        return name.trim();
    }

    private String normalizeDescription(String description) {
        return StringUtils.hasText(description) ? description.trim() : null;
    }
}
