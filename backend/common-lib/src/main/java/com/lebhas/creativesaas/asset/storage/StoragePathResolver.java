package com.lebhas.creativesaas.asset.storage;

import com.lebhas.creativesaas.storage.application.StoragePathBuilder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class StoragePathResolver {

    private final StoragePathBuilder storagePathBuilder;

    public StoragePathResolver(StoragePathBuilder storagePathBuilder) {
        this.storagePathBuilder = storagePathBuilder;
    }

    public String resolveRaw(UUID workspaceId, UUID projectId, UUID assetId) {
        return storagePathBuilder.buildRawPath(workspaceId, projectId, assetId);
    }

    public String resolveGenerated(UUID workspaceId, UUID projectId, UUID outputId) {
        return storagePathBuilder.buildGeneratedPath(workspaceId, projectId, outputId);
    }
}
