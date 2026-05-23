package com.lebhas.asset.consumer;

import com.lebhas.asset.event.AssetCleanupEvent;

public interface AssetCleanupHooks {

    default void cleanupAssetArtifacts(AssetCleanupEvent event) {
    }
}
