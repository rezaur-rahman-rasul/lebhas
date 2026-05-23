package com.lebhas.creativesaas.storage.application;

import com.lebhas.creativesaas.storage.application.dto.StorageFileView;
import com.lebhas.creativesaas.storage.domain.StorageFileEntity;
import org.springframework.stereotype.Component;

@Component
public class StorageFileViewMapper {

    public StorageFileView toView(StorageFileEntity file) {
        return new StorageFileView(
                file.getId(),
                file.getWorkspaceId(),
                file.getProjectId(),
                file.getProvider(),
                file.getBucket(),
                file.getObjectKey(),
                file.getCdnUrl(),
                file.getMimeType(),
                file.getFileExtension(),
                file.getFileSize(),
                file.getHash(),
                file.getWidth(),
                file.getHeight(),
                file.getDuration(),
                file.getStorageClass(),
                file.getFilePurpose(),
                file.getCreatedAt(),
                file.getUpdatedAt());
    }
}
