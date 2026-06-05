package com.lebhas.ai.creative.service;

import com.lebhas.ai.creative.dto.AiCreativeGenerateRequest;
import com.lebhas.ai.creative.enums.GenerationMode;
import org.springframework.stereotype.Service;

@Service
public class AiCreativePromptBuilderService {

    public String buildFinalImagePrompt(
            AiCreativeGenerateRequest request,
            GenerationMode mode,
            String backgroundPrompt,
            AiCreativePersistenceService.CreativeContext context,
            String resolvedSize
    ) {
        String base = switch (mode) {
            case TEXT_TO_CREATIVE -> buildTextCreativePrompt(request);
            case PRODUCT_IMAGE_TO_CREATIVE -> buildProductImageCreativePrompt(request);
            case MULTI_REFERENCE -> buildMultiReferenceCreativePrompt(request);
            case BACKGROUND_REPLACE -> buildBackgroundReplacementPrompt(request, backgroundPrompt);
            case TRANSPARENT_ASSET -> buildTransparentAssetPrompt(request);
        };
        return """
                %s

                Structured Lebhas creative brief:
                - Brand name: %s
                - Product/service: %s
                - Campaign/project: %s
                - Campaign idea: %s
                - Headline: %s
                - Subheadline: %s
                - Offer text: %s
                - CTA: %s
                - Platform: %s
                - Creative type: %s
                - Size: %s
                - Language: %s
                - Tone: %s

                Fixed Lebhas rules:
                - Market: Bangladesh
                - Output must look professional, paid-campaign ready, modern, clean, premium, and conversion-focused.
                - Keep safe space for headline, offer text, logo, and CTA.
                - Text must be readable and correctly spelled in the selected language.
                - Do not include real third-party logos.
                - Do not include irrelevant text.
                - Do not use human models unless explicitly allowed.
                - Do not distort the product if a product/reference image is provided.
                - Avoid cluttered backgrounds and low-quality typography.
                - Use commercial lighting and clean composition.
                """.formatted(
                base,
                safe(context.brandName(), "Selected brand"),
                safe(context.productServiceName(), safe(request.productDescription(), "Not selected")),
                safe(context.campaignName(), "Quick generation"),
                safe(request.campaignIdea(), safe(request.productDescription(), "Campaign creative")),
                safe(request.headline(), safe(request.campaignIdea(), "Campaign creative")),
                safe(request.subheadline(), ""),
                safe(request.offerText(), ""),
                safe(request.cta(), "Shop Now"),
                request.platform(),
                request.creativeType(),
                resolvedSize,
                safe(request.language(), "English"),
                request.tone());
    }

    public String buildTextCreativePrompt(AiCreativeGenerateRequest request) {
        return """
                Create a professional %s campaign ad creative for a Bangladeshi brand.

                Brand context:
                - Product/service: %s
                - Target audience: %s
                - Market: Bangladesh
                - Language: %s
                - Tone: %s
                - Campaign objective: %s

                Creative requirements:
                - Platform: %s
                - Creative type: %s
                - Size: %s
                - Main headline: "%s"
                - CTA button: "%s"
                - Design style: premium, modern, clean, conversion-focused
                - Use elegant lighting, clean shadows, and a high-quality advertising layout
                - Keep enough safe space around text
                - Make the creative suitable for paid social media campaign
                - Do not use human models if requested
                - Do not use real third-party logos
                - Do not include irrelevant text
                - Match the visual style to the Bangladesh market
                %s
                """.formatted(
                request.platform(),
                safe(request.productDescription(), safe(request.campaignIdea(), "Brand Attire creative generation")),
                safe(request.targetAudience(), "Bangladeshi business owners"),
                safe(request.language(), "English"),
                request.tone(),
                safe(request.campaignObjective(), "Campaign conversion"),
                request.platform(),
                request.creativeType(),
                safe(request.size(), "1024x1024"),
                safe(request.headline(), safe(request.campaignIdea(), "Create Ads Beyond Imagination")),
                safe(request.cta(), "Start Now"),
                banglaInstruction(request.language()));
    }

    public String buildProductImageCreativePrompt(AiCreativeGenerateRequest request) {
        return """
                Create a professional %s campaign ad creative using the uploaded product image as the main product.

                Design requirements:
                - Keep the uploaded product realistic, clear, and undistorted
                - Do not change the product shape, color, logo, packaging, or core appearance
                - Remove or improve messy background if needed
                - Use premium studio lighting and clean shadows
                - Use a modern advertising layout suitable for %s
                - Creative type: %s
                - Size: %s
                - Add headline text: "%s"
                - Add CTA button: "%s"
                - Use brand tone: %s
                - Target audience: %s
                - Market: Bangladesh
                - No human models if requested
                - No real third-party logos
                - Product must remain the visual focus
                %s
                """.formatted(
                request.platform(),
                request.platform(),
                request.creativeType(),
                safe(request.size(), "1024x1024"),
                safe(request.headline(), "Create Ads Beyond Imagination"),
                safe(request.cta(), "Start Now"),
                request.tone(),
                safe(request.targetAudience(), "Bangladeshi customers"),
                banglaInstruction(request.language()));
    }

    public String buildMultiReferenceCreativePrompt(AiCreativeGenerateRequest request) {
        return buildProductImageCreativePrompt(request)
                + "\nUse additional uploaded logo/reference/packaging images only as brand and visual references. Preserve brand feeling without inventing fake third-party logos.";
    }

    public String buildBackgroundReplacementPrompt(AiCreativeGenerateRequest request, String backgroundPrompt) {
        return """
                Replace or enhance only the background of the uploaded image.
                - Keep the main product unchanged
                - Do not distort product shape, color, packaging, or logo
                - Replace messy background with: %s
                - Use realistic lighting and shadows
                - Add headline: "%s" if provided
                - Add CTA: "%s" if provided
                - Keep text readable
                - Output must look like a polished professional ad creative
                """.formatted(
                safe(backgroundPrompt, safe(request.background(), "premium clean studio background")),
                safe(request.headline(), ""),
                safe(request.cta(), ""));
    }

    public String buildTransparentAssetPrompt(AiCreativeGenerateRequest request) {
        return """
                Create a clean transparent product asset.
                - Isolate the product
                - Keep product realistic and undistorted
                - Remove background completely
                - Output must have transparent background
                - Use PNG output
                - Do not add text
                - Do not add unrelated objects
                - Preserve product edges cleanly
                """;
    }

    private String banglaInstruction(String language) {
        if (language == null) {
            return "";
        }
        String normalized = language.trim().toLowerCase();
        if (!normalized.equals("bn") && !normalized.contains("bangla") && !normalized.contains("bengali")) {
            return "";
        }
        return "Use Bangla text with correct spelling, readable placement, and natural Bangladeshi marketing language.";
    }

    private String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
