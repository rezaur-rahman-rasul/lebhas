package com.lebhas.creativesaas.workspace.interfaces;

import com.lebhas.creativesaas.brand.application.BrandService;
import com.lebhas.creativesaas.brand.application.dto.BrandView;
import com.lebhas.creativesaas.common.api.ApiResponse;
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
@RequestMapping("/api/v1/workspaces/{workspaceId}/brands")
@Tag(name = "Brands")
@SecurityRequirement(name = "bearerAuth")
public class BrandController {

    private final BrandService brandService;

    public BrandController(BrandService brandService) {
        this.brandService = brandService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('BRAND_VIEW')")
    @Operation(summary = "List brands in a workspace")
    public ApiResponse<List<BrandView>> listBrands(@PathVariable UUID workspaceId) {
        return ApiResponse.success(brandService.listBrands(workspaceId));
    }

    @GetMapping("/{brandId}")
    @PreAuthorize("hasAuthority('BRAND_VIEW')")
    @Operation(summary = "Get a brand by id")
    public ApiResponse<BrandView> getBrand(@PathVariable UUID workspaceId, @PathVariable UUID brandId) {
        return ApiResponse.success(brandService.getBrand(workspaceId, brandId));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('BRAND_MANAGE')")
    @Operation(summary = "Create a brand")
    public ApiResponse<BrandView> createBrand(
            @PathVariable UUID workspaceId,
            @Valid @RequestBody CreateBrandRequest request
    ) {
        return ApiResponse.success(brandService.createBrand(
                workspaceId,
                request.name(),
                request.businessType(),
                request.industry(),
                request.targetAudience(),
                request.brandVoice(),
                request.preferredCta(),
                request.primaryColor(),
                request.secondaryColor(),
                request.website(),
                request.facebookUrl(),
                request.instagramUrl(),
                request.linkedinUrl(),
                request.tiktokUrl(),
                request.languagePreference()));
    }

    @PutMapping("/{brandId}")
    @PreAuthorize("hasAuthority('BRAND_MANAGE')")
    @Operation(summary = "Update a brand")
    public ApiResponse<BrandView> updateBrand(
            @PathVariable UUID workspaceId,
            @PathVariable UUID brandId,
            @Valid @RequestBody UpdateBrandRequest request
    ) {
        return ApiResponse.success(brandService.updateBrand(
                workspaceId,
                brandId,
                request.name(),
                request.businessType(),
                request.industry(),
                request.targetAudience(),
                request.brandVoice(),
                request.preferredCta(),
                request.primaryColor(),
                request.secondaryColor(),
                request.website(),
                request.facebookUrl(),
                request.instagramUrl(),
                request.linkedinUrl(),
                request.tiktokUrl(),
                request.languagePreference(),
                request.status()));
    }

    @DeleteMapping("/{brandId}")
    @PreAuthorize("hasAuthority('BRAND_MANAGE')")
    @Operation(summary = "Delete a brand")
    public ApiResponse<Void> deleteBrand(@PathVariable UUID workspaceId, @PathVariable UUID brandId) {
        brandService.deleteBrand(workspaceId, brandId);
        return ApiResponse.success("Brand deleted", null);
    }
}
