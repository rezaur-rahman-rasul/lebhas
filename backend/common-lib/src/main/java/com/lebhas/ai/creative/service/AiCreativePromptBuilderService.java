package com.lebhas.ai.creative.service;

import com.lebhas.ai.application.BanglaTypographyQualityService;
import com.lebhas.ai.creative.dto.AiCreativeGenerateRequest;
import com.lebhas.ai.creative.enums.GenerationMode;
import org.springframework.stereotype.Service;

@Service
public class AiCreativePromptBuilderService {

    private static final String BANGLA_VISUAL_ONLY_PROMPT = """
            Leave sufficient empty space for text placement.
            Do not include any text.
            Do not include letters.
            Do not include words.
            Do not include typography.
            Do not include Bangla characters.
            Do not include English characters.
            Do not include logos.
            Do not include watermarks.
            Generate visual elements only.
            """;

    private static final String NO_TEXT_IN_IMAGE_POLICY = """
            Do not render any readable text, letters, words, labels, template filler text, field names, or CTA in the image.
            Leave clean empty text-safe areas for later typography overlay.
            The instruction text is not ad copy. Do not render the instruction sentence as visible text.
            Do not render the user's campaign idea or prompt as visible text.
            """;

    private final BanglaTypographyQualityService banglaTypographyQualityService;
    private final BengaliTypographyDatasetService bengaliTypographyDatasetService;

    public AiCreativePromptBuilderService(
            BanglaTypographyQualityService banglaTypographyQualityService,
            BengaliTypographyDatasetService bengaliTypographyDatasetService
    ) {
        this.banglaTypographyQualityService = banglaTypographyQualityService;
        this.bengaliTypographyDatasetService = bengaliTypographyDatasetService;
    }

    public String buildFinalImagePrompt(
            AiCreativeGenerateRequest request,
            GenerationMode mode,
            String backgroundPrompt,
            AiCreativePersistenceService.CreativeContext context,
            String resolvedSize
    ) {
        String base = basePromptForMode(request, mode, backgroundPrompt);
        boolean typographyDisabled = request.includeTypography() != null && !request.includeTypography();
        boolean internalTypography = usesInternalTypography(request);
        boolean banglaTypography = !typographyDisabled && requiresBanglaTypography(request);
        String visibleTextPolicy = typographyDisabled
                ? "NO_TEXT_IMAGE_ONLY"
                : internalTypography ? "NO_AI_TEXT_INTERNAL_TYPOGRAPHY" : "MODEL_TEXT_ALLOWED_ONLY_FOR_EXPLICIT_TEXT_LAYERS";
        String visualPrompt = visualPrompt(request, banglaTypography);
        boolean scrubTextForBangla = requiresBanglaTypography(request);
        return """
                %s

                Structured Lebhas creative brief:
                - Brand name: %s
                - Brand business type: %s
                - Brand industry: %s
                - Brand voice: %s
                - Brand slogan: %s
                - Brand colors: %s
                - Product/service: %s
                - Product category: %s
                - Product description: %s
                - Product selling points / benefits: %s
                - Campaign/project: %s
                - Campaign description: %s
                - Campaign objective: %s
                - Campaign type: %s
                - Target audience: %s
                - Preferred CTA: %s
                - Visual instruction: %s
                - Visible text policy: %s
                - Text layer separation JSON:
                {
                  "visualPrompt": "%s",
                  "visibleTextPolicy": "%s",
                  "textLayers": {
                    "headline": %s,
                    "subheadline": %s,
                    "offer": %s,
                    "cta": %s
                  }
                }
                - Platform: %s
                - Creative type: %s
                - Size: %s
                - Language: %s
                - Tone: %s

                Fixed Lebhas rules:
                - Market: Bangladesh
                - Treat brand, product/service, and campaign context as authoritative unless the user prompt explicitly overrides a visual detail.
                - Use available brand colors and brand voice in the layout direction.
                - Promote the selected product/service and highlight inherited selling points.
                - Match the inherited campaign objective and CTA.
                %s
                %s
                - Output must look professional, paid-campaign ready, modern, clean, premium, and conversion-focused.
                - Keep safe empty space for optional headline, offer text, logo, and CTA.
                - The instruction text is not ad copy. Do not render the instruction sentence as visible text.
                - The user prompt/campaign idea is only direction for composition. It must never appear as image text.
                - Do not include real third-party logos.
                - Do not include irrelevant text.
                - Do not include template filler text, internal field names, debug labels, or template tokens.
                - Do not use human models unless explicitly allowed.
                - Do not distort the product if a product/reference image is provided.
                - Avoid cluttered backgrounds and low-quality typography.
                - Use commercial lighting and clean composition.
                %s
                """.formatted(
                base,
                promptSafeText(context.brandName(), "Selected brand", scrubTextForBangla),
                promptSafeText(contextValue(context.brand(), "businessType", "Not specified"), "Not specified", scrubTextForBangla),
                promptSafeText(contextValue(context.brand(), "industry", "Not specified"), "Not specified", scrubTextForBangla),
                promptSafeText(contextValue(context.brand(), "brandVoice", "Not specified"), "Not specified", scrubTextForBangla),
                promptSafeText(context.slogan(), "Not specified", scrubTextForBangla),
                context.colors().isEmpty() ? "Not specified" : String.join(", ", context.colors()),
                promptSafeText(context.productServiceName(), "Selected product", scrubTextForBangla),
                promptSafeText(contextValue(context.productService(), "category", "Not specified"), "Not specified", scrubTextForBangla),
                promptSafeText(contextValue(context.productService(), "description", safe(request.productDescription(), "Not specified")), "Not specified", scrubTextForBangla),
                promptSafeText(context.sellingPoints(), "Not specified", scrubTextForBangla),
                promptSafeText(context.campaignName(), "Quick generation", scrubTextForBangla),
                promptSafeText(contextValue(context.campaign(), "description", "Not specified"), "Not specified", scrubTextForBangla),
                promptSafeText(context.campaignObjective(), safe(request.campaignObjective(), "Campaign conversion"), scrubTextForBangla),
                promptSafeText(contextValue(context.campaign(), "campaignType", "Not specified"), "Not specified", scrubTextForBangla),
                promptSafeText(context.audience(), safe(request.targetAudience(), "Bangladeshi customers"), scrubTextForBangla),
                scrubTextForBangla ? "Backend overlay only" : safe(context.preferredCTA(), "None"),
                visualPrompt,
                visibleTextPolicy,
                jsonString(visualPrompt),
                visibleTextPolicy,
                textLayerJsonValue(request, request.headline()),
                textLayerJsonValue(request, request.subheadline()),
                textLayerJsonValue(request, request.offerText()),
                ctaJsonValue(request),
                safe(context.platform(), String.valueOf(request.platform())),
                safe(context.creativeType(), String.valueOf(request.creativeType())),
                resolvedSize,
                safe(context.language(), safe(request.language(), "English")),
                safe(context.tone(), String.valueOf(request.tone())),
                typographyDisabled
                        ? "- Typography disabled: do not render headlines, captions, labels, CTA text, readable words, letters, or any text. Create an image-only visual."
                        : internalTypography
                        ? "- Typography is handled by Lebhas after generation: do not render headlines, captions, labels, offer text, CTA text, readable words, letters, or any text. Leave clean text-safe areas."
                        : "- Typography enabled: use text only when explicitly provided and keep it minimal.",
                adaptiveLogoInstruction(request),
                banglaTypographySystemInstruction(request));
    }

    public String buildTextCreativePrompt(AiCreativeGenerateRequest request) {
        if (usesInternalTypography(request)) {
            return buildBanglaVisualOnlyPrompt(request, "Create a professional campaign advertising background/composition.");
        }
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
                - Visual instruction: %s
                - Main headline text layer: %s
                %s
                - Design style: premium, modern, clean, conversion-focused
                - Use elegant lighting, clean shadows, and a high-quality advertising layout
                - Keep enough safe space around text
                - The instruction text is not ad copy. Do not render the instruction sentence as visible text.
                - Do not include template filler text or internal field names.
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
                safe(request.campaignIdea(), safe(request.productDescription(), "Create premium campaign visual")),
                textLayerValue(request, request.headline()),
                ctaPromptInstruction(request),
                banglaInstruction(request.language()));
    }

    private String basePromptForMode(AiCreativeGenerateRequest request, GenerationMode mode, String backgroundPrompt) {
        if (mode == GenerationMode.TEXT_TO_CREATIVE) {
            return buildTextCreativePrompt(request);
        }
        if (mode == GenerationMode.PRODUCT_IMAGE_TO_CREATIVE) {
            return buildProductImageCreativePrompt(request);
        }
        if (mode == GenerationMode.MULTI_REFERENCE) {
            return buildMultiReferenceCreativePrompt(request);
        }
        if (mode == GenerationMode.BACKGROUND_REPLACE) {
            return buildBackgroundReplacementPrompt(request, backgroundPrompt);
        }
        if (mode == GenerationMode.TRANSPARENT_ASSET) {
            return buildTransparentAssetPrompt(request);
        }
        return buildTextCreativePrompt(request);
    }

    public String buildProductImageCreativePrompt(AiCreativeGenerateRequest request) {
        if (usesInternalTypography(request)) {
            return buildBanglaVisualOnlyPrompt(request, "Create a professional product advertising background/composition using the uploaded product image as the main product.");
        }
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
                - Visual instruction: %s
                - Headline text layer: %s
                %s
                - Use brand tone: %s
                - Target audience: %s
                - Market: Bangladesh
                - The instruction text is not ad copy. Do not render the instruction sentence as visible text.
                - Do not include template filler text or internal field names.
                - No human models if requested
                - No real third-party logos
                - Product must remain the visual focus
                %s
                """.formatted(
                request.platform(),
                request.platform(),
                request.creativeType(),
                safe(request.size(), "1024x1024"),
                safe(request.campaignIdea(), safe(request.productDescription(), "Create premium product ad visual")),
                textLayerValue(request, request.headline()),
                ctaPromptInstruction(request),
                request.tone(),
                safe(request.targetAudience(), "Bangladeshi customers"),
                banglaInstruction(request.language()));
    }

    public String buildMultiReferenceCreativePrompt(AiCreativeGenerateRequest request) {
        return buildProductImageCreativePrompt(request)
                + "\nUse additional uploaded logo/reference/packaging images as adaptive brand and visual references. Preserve brand feeling without inventing fake third-party logos."
                + "\n" + adaptiveLogoInstruction(request);
    }

    public String buildBackgroundReplacementPrompt(AiCreativeGenerateRequest request, String backgroundPrompt) {
        if (usesInternalTypography(request)) {
            return buildBanglaVisualOnlyPrompt(request, "Replace or enhance only the product background with " + safe(backgroundPrompt, safe(request.background(), "a premium clean studio background")) + ".");
        }
        return """
                Replace or enhance only the background of the uploaded image.
                - Keep the main product unchanged
                - Do not distort product shape, color, packaging, or logo
                - Replace messy background with: %s
                - Use realistic lighting and shadows
                - Headline text layer: %s
                %s
                - The instruction text is not ad copy. Do not render the instruction sentence as visible text.
                - Do not include template filler text or internal field names.
                - Output must look like a polished professional ad creative
                """.formatted(
                safe(backgroundPrompt, safe(request.background(), "premium clean studio background")),
                textLayerValue(request, request.headline()),
                ctaPromptInstruction(request));
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
        return """
                Bengali typography safety:
                - %s
                - Do not draw Bengali letters, conjuncts, CTA words, fake Bengali-looking symbols, Latin ad copy, or template filler text.
                - Leave clean premium text-safe regions for backend Unicode Bangla typography overlay.
                - Preserve contrast-safe regions for post-rendered Bangla text.
                """.formatted(NO_TEXT_IN_IMAGE_POLICY.strip());
    }

    private String banglaTypographySystemInstruction(AiCreativeGenerateRequest request) {
        if (!requiresBanglaTypography(request)) {
            return "";
        }
        return """
                Bengali typography overlay contract:
                - Final Bengali copy is backend-only metadata; do not rasterize it.
                - Image model must create layout, lighting, product composition, and empty text-safe zones only.
                - Backend will render Unicode NFC Bangla with OpenType-capable fonts after image generation.
                - Font intelligence: %s for headlines, %s for CTA, conjunct dataset size %d.
                - OCR/quality policy: reject fake glyphs, malformed hasanta, fragmented conjuncts, unreadable CTA.
                """.formatted(
                bengaliTypographyDatasetService.bestFontFor(BengaliTypographyDatasetService.TextRole.HEADLINE).family(),
                bengaliTypographyDatasetService.bestFontFor(BengaliTypographyDatasetService.TextRole.CTA).family(),
                bengaliTypographyDatasetService.conjunctExamples().size());
    }

    private String adaptiveLogoInstruction(AiCreativeGenerateRequest request) {
        if (request.includeLogo() != null && !request.includeLogo()) {
            return "- Brand logo disabled: do not place, draw, imply, or reserve a logo area.";
        }
        return "- Brand logo is handled by Lebhas after generation. Do not place, draw, imitate, or reserve a logo inside the AI image. Leave clean composition space.";
    }

    private String buildBanglaVisualOnlyPrompt(AiCreativeGenerateRequest request, String modeInstruction) {
        return """
                %s

                Bangla creative visual-only policy:
                - %s
                - Use the campaign idea only as non-visible composition direction: %s
                - Do not render any readable text in any language.
                - Do not render Bangla letters, Latin letters, labels, product callouts, captions, CTA buttons, prices, offers, or template tokens.
                - Keep clean premium empty regions for backend-rendered Unicode Bangla headline, subheadline, offer, and CTA only if those fields were explicitly provided.
                - No template/debug text.
                """.formatted(
                modeInstruction,
                BANGLA_VISUAL_ONLY_PROMPT.strip(),
                visualPrompt(request, true));
    }

    private String ctaPromptInstruction(AiCreativeGenerateRequest request) {
        if (request.includeCta() != null && !request.includeCta()) {
            return "- CTA: disabled. Do not include CTA text, CTA buttons, or invented CTA.";
        }
        String cta = cleanTextLayer(request.cta());
        if (cta == null) {
            return "- CTA: none. Do not include any call-to-action button, CTA text, or invented CTA. Do not add \"Shop Now\".";
        }
        if (usesInternalTypography(request)) {
            return "- CTA text layer: backend overlay only. The CTA value is intentionally omitted from the image prompt. Do not render CTA text in the AI image.";
        }
        return "- CTA: \"%s\". Include this CTA exactly as provided.".formatted(
                cta);
    }

    private String ctaJsonValue(AiCreativeGenerateRequest request) {
        if (request.includeCta() != null && !request.includeCta()) {
            return "null";
        }
        String cta = cleanTextLayer(request.cta());
        if (cta == null || usesInternalTypography(request)) {
            return "null";
        }
        return jsonString(cta);
    }

    private String textLayerValue(AiCreativeGenerateRequest request, String value) {
        String cleaned = cleanTextLayer(value);
        if (cleaned == null) {
            return "none";
        }
        if (usesInternalTypography(request)) {
            return "backend overlay only; value intentionally omitted from image prompt";
        }
        return "\"%s\"".formatted(cleaned);
    }

    private String textLayerJsonValue(AiCreativeGenerateRequest request, String value) {
        String cleaned = cleanTextLayer(value);
        if (cleaned == null || usesInternalTypography(request)) {
            return "null";
        }
        return jsonString(cleaned);
    }

    private String cleanTextLayer(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String cleaned = value.trim();
        String normalized = cleaned.toUpperCase(java.util.Locale.ROOT).replace('-', '_').replace(' ', '_');
        if (normalized.contains("PLACEHOLDER")
                || normalized.equals("HEADLINE")
                || normalized.equals("SUBHEADLINE")
                || normalized.equals("OFFER")
                || normalized.equals("CTA")) {
            return null;
        }
        return cleaned;
    }

    private String visualPrompt(AiCreativeGenerateRequest request, boolean banglaTypography) {
        if (banglaTypography) {
            if (banglaTypographyQualityService.isBanglaLanguage(request.language())) {
                return "Premium Bangladesh-market product advertising composition with clean empty copy space, no visible text, no logo, no watermark";
            }
            String productContext = cleanVisualInstruction(request.productDescription());
            String campaignContext = cleanVisualInstruction(request.campaignIdea());
            if (productContext != null && campaignContext != null && !isGenericGenerationPrompt(campaignContext)) {
                return productContext + ". Composition direction: " + campaignContext;
            }
            if (productContext != null) {
                return productContext;
            }
            if (campaignContext != null && !isGenericGenerationPrompt(campaignContext)) {
                return campaignContext;
            }
            return "Premium Bangladesh-market product advertising composition with clean empty copy space";
        }
        return safe(cleanVisualInstruction(request.campaignIdea()), safe(cleanVisualInstruction(request.productDescription()), "Campaign creative"));
    }

    private String cleanVisualInstruction(String value) {
        String cleaned = cleanTextLayer(value);
        if (cleaned == null) {
            return null;
        }
        return cleaned
                .replace("\"", "'")
                .replace("\r", " ")
                .replace("\n", " ")
                .replaceAll("\\s{2,}", " ")
                .trim();
    }

    private boolean isGenericGenerationPrompt(String value) {
        String normalized = value.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9 ]", " ").replaceAll("\\s+", " ").trim();
        return normalized.equals("generate a creative")
                || normalized.equals("generate creative")
                || normalized.equals("create a creative")
                || normalized.equals("make a creative")
                || normalized.equals("creative")
                || normalized.equals("generate an ad")
                || normalized.equals("create an ad");
    }

    private String jsonString(String value) {
        return "\"" + value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n") + "\"";
    }

    private boolean requiresBanglaTypography(AiCreativeGenerateRequest request) {
        if (request.includeTypography() != null && !request.includeTypography()) {
            return false;
        }
        return banglaTypographyQualityService.isBanglaLanguage(request.language())
                || banglaTypographyQualityService.containsBangla(request.headline())
                || banglaTypographyQualityService.containsBangla(request.subheadline())
                || banglaTypographyQualityService.containsBangla(request.offerText())
                || banglaTypographyQualityService.containsBangla(request.cta());
    }

    private boolean usesInternalTypography(AiCreativeGenerateRequest request) {
        if (request.includeTypography() != null && !request.includeTypography()) {
            return false;
        }
        return banglaTypographyQualityService.isBanglaLanguage(request.language())
                || hasText(request.headline())
                || hasText(request.subheadline())
                || hasText(request.offerText())
                || (request.includeCta() != null && request.includeCta() && hasText(request.cta()));
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String promptSafeText(String value, String fallback, boolean scrubBangla) {
        String safeValue = safe(value, fallback);
        if (!scrubBangla) {
            return safeValue;
        }
        return banglaTypographyQualityService.containsBangla(safeValue)
                ? "Backend typography/context only; do not render as text"
                : safeValue;
    }

    private String contextValue(java.util.Map<String, Object> source, String key, String fallback) {
        if (source == null || source.isEmpty()) {
            return fallback;
        }
        Object value = source.get(key);
        if (value == null || !org.springframework.util.StringUtils.hasText(String.valueOf(value))) {
            return fallback;
        }
        return String.valueOf(value).trim();
    }
}
