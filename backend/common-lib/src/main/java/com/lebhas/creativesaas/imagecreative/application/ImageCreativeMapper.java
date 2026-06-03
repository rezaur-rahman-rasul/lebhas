package com.lebhas.creativesaas.imagecreative.application;

import com.lebhas.creativesaas.imagecreative.application.dto.ImageCreativeGenerationView;
import com.lebhas.creativesaas.imagecreative.domain.ImageCreativeGeneration;
import org.springframework.stereotype.Component;

@Component
public class ImageCreativeMapper {

    public ImageCreativeGenerationView toView(ImageCreativeGeneration generation) {
        return new ImageCreativeGenerationView(
                generation.getId(),
                generation.getWorkspaceId(),
                generation.getProjectId(),
                generation.getCreativeRequestId(),
                generation.getBrandId(),
                generation.getProductServiceId(),
                generation.getProductAssetId(),
                generation.getToolCode(),
                generation.getCreativeFormat(),
                generation.getPlatform(),
                generation.getLanguage(),
                generation.getQualityMode(),
                generation.getRequestedVersionCount(),
                generation.getCreditCost(),
                generation.getCreditReservationId(),
                generation.getStatus(),
                generation.getFailureReason(),
                generation.getGeneratedVersionIds(),
                generation.getRequestPayload(),
                generation.getCreatedAt());
    }
}
