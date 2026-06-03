package com.lebhas.creativesaas.imagecreative.application;

import com.lebhas.ai.application.dto.ResolvedProviderRouteView;

import java.util.List;

public interface ProductImageCreativeProvider {

    List<ProductImageCreativeProviderOutput> generate(ProductImageCreativeContext context, int count, ResolvedProviderRouteView route);
}
