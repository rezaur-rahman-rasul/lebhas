package com.lebhas.creativesaas.storage.application;

import com.lebhas.creativesaas.storage.domain.StorageFilePurpose;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class StoragePathBuilder {

    public String buildRawPath(UUID workspaceId, UUID projectId, UUID fileId) {
        return build(StorageFilePurpose.RAW, workspaceId, projectId, fileId);
    }

    public String buildProcessedPath(UUID workspaceId, UUID projectId, UUID fileId) {
        return build(StorageFilePurpose.PROCESSED, workspaceId, projectId, fileId);
    }

    public String buildGeneratedPath(UUID workspaceId, UUID projectId, UUID fileId) {
        return build(StorageFilePurpose.GENERATED, workspaceId, projectId, fileId);
    }

    public String buildThumbnailPath(UUID workspaceId, UUID projectId, UUID fileId) {
        return build(StorageFilePurpose.THUMBNAIL, workspaceId, projectId, fileId);
    }

    public String build(StorageFilePurpose purpose, UUID workspaceId, UUID projectId, UUID fileId) {
        String prefix = switch (purpose) {
            case RAW -> "raw";
            case PROCESSED -> "processed";
            case GENERATED -> "generated";
            case THUMBNAIL -> "thumbnails";
        };
        return "%s/workspaces/%s/projects/%s/%s".formatted(prefix, workspaceId, projectId, fileId);
    }
}
