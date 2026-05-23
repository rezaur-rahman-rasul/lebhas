package com.lebhas.creativesaas.creativerequest.application;

import com.lebhas.creativesaas.brand.application.BrandService;
import com.lebhas.creativesaas.brand.domain.BrandEntity;
import com.lebhas.creativesaas.brand.domain.BrandStatus;
import com.lebhas.creativesaas.campaign.application.ProjectCampaignService;
import com.lebhas.creativesaas.campaign.domain.ProjectCampaignEntity;
import com.lebhas.creativesaas.campaign.domain.ProjectCampaignStatus;
import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.creativerequest.application.dto.CreateCreativeRequestCommand;
import com.lebhas.creativesaas.product.application.ProductServiceCatalogService;
import com.lebhas.creativesaas.product.domain.ProductServiceEntity;
import com.lebhas.creativesaas.product.domain.ProductServiceStatus;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class CreativeHierarchyValidationService {

    private final BrandService brandService;
    private final ProductServiceCatalogService productServiceCatalogService;
    private final ProjectCampaignService projectCampaignService;

    public CreativeHierarchyValidationService(
            BrandService brandService,
            ProductServiceCatalogService productServiceCatalogService,
            ProjectCampaignService projectCampaignService
    ) {
        this.brandService = brandService;
        this.productServiceCatalogService = productServiceCatalogService;
        this.projectCampaignService = projectCampaignService;
    }

    public CreativeHierarchyContext validate(UUID workspaceId, CreateCreativeRequestCommand command) {
        ProjectCampaignEntity projectCampaign = projectCampaignService.requireProjectCampaign(workspaceId, command.projectCampaignId());
        if (projectCampaign.getStatus() != ProjectCampaignStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.PROJECT_CAMPAIGN_NOT_FOUND, "Project campaign is not active");
        }

        UUID productServiceId = command.productServiceId() == null
                ? projectCampaign.getProductServiceId()
                : command.productServiceId();
        ProductServiceEntity productService = productServiceCatalogService.requireProductService(workspaceId, productServiceId);
        if (productService.getStatus() != ProductServiceStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.PRODUCT_SERVICE_NOT_FOUND, "Product service is not active");
        }

        UUID brandId = command.brandId() == null
                ? productService.getBrandId()
                : command.brandId();
        BrandEntity brand = brandService.requireBrand(workspaceId, brandId);
        if (brand.getStatus() != BrandStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.BRAND_NOT_FOUND, "Brand is not active");
        }

        if (!productService.getBrandId().equals(brand.getId())) {
            throw new BusinessException(
                    ErrorCode.GENERATION_VALIDATION_FAILED,
                    "Selected product service does not belong to the selected brand");
        }
        if (!projectCampaign.getProductServiceId().equals(productService.getId())) {
            throw new BusinessException(
                    ErrorCode.GENERATION_VALIDATION_FAILED,
                    "Selected project campaign does not belong to the selected product service");
        }
        if (!projectCampaign.getBrandId().equals(brand.getId())) {
            throw new BusinessException(
                    ErrorCode.GENERATION_VALIDATION_FAILED,
                    "Selected project campaign does not belong to the selected brand");
        }

        return new CreativeHierarchyContext(brand, productService, projectCampaign);
    }

    public record CreativeHierarchyContext(
            BrandEntity brand,
            ProductServiceEntity productService,
            ProjectCampaignEntity projectCampaign
    ) {
    }
}
