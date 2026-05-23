package com.lebhas.creativesaas.brand.application;

import com.lebhas.creativesaas.brand.application.dto.BrandView;
import com.lebhas.creativesaas.brand.domain.BrandEntity;
import org.springframework.stereotype.Component;

@Component
public class BrandViewMapper {

    public BrandView toView(BrandEntity brand) {
        return new BrandView(
                brand.getId(),
                brand.getWorkspaceId(),
                brand.getOwnerUserId(),
                brand.getName(),
                brand.getBusinessType(),
                brand.getIndustry(),
                brand.getTargetAudience(),
                brand.getBrandVoice(),
                brand.getPreferredCta(),
                brand.getPrimaryColor(),
                brand.getSecondaryColor(),
                brand.getWebsite(),
                brand.getFacebookUrl(),
                brand.getInstagramUrl(),
                brand.getLinkedinUrl(),
                brand.getTiktokUrl(),
                brand.getLanguagePreference(),
                brand.getStatus(),
                brand.getCreatedAt(),
                brand.getUpdatedAt());
    }
}
