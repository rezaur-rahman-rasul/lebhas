package com.lebhas.asset.consumer;

import com.lebhas.asset.event.AssetProcessEvent;

public interface AssetProcessHooks {

    default void optimizeImage(AssetProcessEvent event) {
    }

    default void extractMetadata(AssetProcessEvent event) {
    }

    default void preprocessForAi(AssetProcessEvent event) {
    }
}
