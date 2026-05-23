package com.lebhas.asset.consumer;

import com.lebhas.asset.event.AssetVariantGenerateEvent;

public interface AssetVariantHooks {

    default void generateThumbnail(AssetVariantGenerateEvent event) {
    }
}
