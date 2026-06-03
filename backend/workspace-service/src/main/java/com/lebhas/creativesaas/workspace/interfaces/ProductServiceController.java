package com.lebhas.creativesaas.workspace.interfaces;

import com.lebhas.creativesaas.common.api.ApiResponse;
import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.product.application.ProductServiceCatalogService;
import com.lebhas.creativesaas.product.application.dto.ProductServiceView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/workspaces")
@Tag(name = "Product Services")
@SecurityRequirement(name = "bearerAuth")
public class ProductServiceController {

    private final ProductServiceCatalogService productServiceCatalogService;

    public ProductServiceController(ProductServiceCatalogService productServiceCatalogService) {
        this.productServiceCatalogService = productServiceCatalogService;
    }

    @PostMapping("/{workspaceId}/product-services")
    @PreAuthorize("hasAuthority('PRODUCT_MANAGE')")
    @Operation(summary = "Create a product or service")
    public ApiResponse<ProductServiceView> createProductService(
            @PathVariable UUID workspaceId,
            @Valid @RequestBody CreateProductServiceRequest request
    ) {
        if (request.brandId() == null) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "brandId is required.");
        }
        return ApiResponse.success(productServiceCatalogService.createProductService(
                workspaceId,
                request.brandId(),
                request.name(),
                request.description(),
                request.category(),
                request.targetAudience(),
                request.sellingPoints()));
    }

    @PostMapping("/{workspaceId}/brands/{brandId}/product-services")
    @PreAuthorize("hasAuthority('PRODUCT_MANAGE')")
    @Operation(summary = "Create a product or service under a brand")
    public ApiResponse<ProductServiceView> createProductServiceUnderBrand(
            @PathVariable UUID workspaceId,
            @PathVariable UUID brandId,
            @Valid @RequestBody CreateProductServiceRequest request
    ) {
        return ApiResponse.success(productServiceCatalogService.createProductService(
                workspaceId,
                brandId,
                request.name(),
                request.description(),
                request.category(),
                request.targetAudience(),
                request.sellingPoints()));
    }

    @GetMapping("/{workspaceId}/product-services")
    @PreAuthorize("hasAuthority('PRODUCT_VIEW')")
    @Operation(summary = "List products and services in a workspace")
    public ApiResponse<List<ProductServiceView>> listProductServices(@PathVariable UUID workspaceId) {
        return ApiResponse.success(productServiceCatalogService.listProductServices(workspaceId));
    }

    @GetMapping("/{workspaceId}/brands/{brandId}/product-services")
    @PreAuthorize("hasAuthority('PRODUCT_VIEW')")
    @Operation(summary = "List products and services under a brand")
    public ApiResponse<List<ProductServiceView>> listProductServicesByBrand(
            @PathVariable UUID workspaceId,
            @PathVariable UUID brandId
    ) {
        return ApiResponse.success(productServiceCatalogService.listProductServicesByBrand(workspaceId, brandId));
    }

    @GetMapping("/{workspaceId}/product-services/{productServiceId}")
    @PreAuthorize("hasAuthority('PRODUCT_VIEW')")
    @Operation(summary = "Get a product or service by id")
    public ApiResponse<ProductServiceView> getProductService(
            @PathVariable UUID workspaceId,
            @PathVariable UUID productServiceId
    ) {
        return ApiResponse.success(productServiceCatalogService.getProductService(workspaceId, productServiceId));
    }

    @PutMapping("/{workspaceId}/product-services/{productServiceId}")
    @PreAuthorize("hasAuthority('PRODUCT_MANAGE')")
    @Operation(summary = "Update a product or service")
    public ApiResponse<ProductServiceView> updateProductService(
            @PathVariable UUID workspaceId,
            @PathVariable UUID productServiceId,
            @Valid @RequestBody UpdateProductServiceRequest request
    ) {
        return ApiResponse.success(productServiceCatalogService.updateProductService(
                workspaceId,
                productServiceId,
                request.name(),
                request.description(),
                request.category(),
                request.targetAudience(),
                request.sellingPoints(),
                request.status()));
    }

    @DeleteMapping("/{workspaceId}/product-services/{productServiceId}")
    @PreAuthorize("hasAuthority('PRODUCT_MANAGE')")
    @Operation(summary = "Delete a product or service")
    public ApiResponse<Void> deleteProductService(
            @PathVariable UUID workspaceId,
            @PathVariable UUID productServiceId
    ) {
        productServiceCatalogService.deleteProductService(workspaceId, productServiceId);
        return ApiResponse.success("Product service deleted", null);
    }
}
