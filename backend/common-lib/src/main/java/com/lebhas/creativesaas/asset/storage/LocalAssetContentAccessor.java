package com.lebhas.creativesaas.asset.storage;

import com.lebhas.creativesaas.asset.domain.AssetEntity;
import org.springframework.core.io.Resource;

public interface LocalAssetContentAccessor {

    Resource open(AssetEntity asset);
}
