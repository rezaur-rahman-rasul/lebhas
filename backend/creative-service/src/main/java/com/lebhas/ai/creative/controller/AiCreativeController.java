package com.lebhas.ai.creative.controller;

import com.lebhas.ai.creative.dto.AiCreativeGenerateRequest;
import com.lebhas.ai.creative.dto.AiCreativeResponse;
import com.lebhas.ai.creative.dto.CreativeCreditAvailabilityRequest;
import com.lebhas.ai.creative.dto.CreativeCreditAvailabilityResponse;
import com.lebhas.ai.creative.dto.TextCreativeRequest;
import com.lebhas.ai.creative.enums.CreativePlatform;
import com.lebhas.ai.creative.enums.CreativeQuality;
import com.lebhas.ai.creative.enums.CreativeTone;
import com.lebhas.ai.creative.enums.CreativeType;
import com.lebhas.ai.creative.enums.ModelQuality;
import com.lebhas.ai.creative.enums.OutputFormat;
import com.lebhas.ai.creative.service.AiCreativeService;
import com.lebhas.ai.creative.service.CreativeCreditAvailabilityService;
import com.lebhas.creativesaas.common.api.ApiResponse;
import com.lebhas.creativesaas.common.security.context.CurrentUserContext;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.UUID;

@RestController
@Tag(name = "AI Creatives")
@SecurityRequirement(name = "bearerAuth")
public class AiCreativeController {

    private final AiCreativeService service;
    private final CreativeCreditAvailabilityService creditAvailabilityService;
    private final ObjectMapper objectMapper;
    private final CurrentUserContext currentUserContext;

    public AiCreativeController(
            AiCreativeService service,
            CreativeCreditAvailabilityService creditAvailabilityService,
            ObjectMapper objectMapper,
            CurrentUserContext currentUserContext
    ) {
        this.service = service;
        this.creditAvailabilityService = creditAvailabilityService;
        this.objectMapper = objectMapper;
        this.currentUserContext = currentUserContext;
    }

    @PostMapping("/api/v1/ai/creatives/credit-preview")
    @PreAuthorize("hasAuthority('WORKSPACE_VIEW')")
    public ApiResponse<CreativeCreditAvailabilityResponse> creditAvailability(
            @Valid @RequestBody CreativeCreditAvailabilityRequest request
    ) {
        return ApiResponse.success("Credit availability checked", creditAvailabilityService.check(request));
    }

    @PostMapping(value = "/api/v1/ai/creatives/generate", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('CREATIVE_GENERATE')")
    public ApiResponse<AiCreativeResponse> generateJson(@Valid @RequestBody AiCreativeGenerateRequest request) {
        return ApiResponse.success(generateInternal(
                request.workspaceId(),
                request.brandId(),
                request.productServiceId(),
                request.campaignId(),
                request.platform(),
                request.language(),
                request.creativeType(),
                request.outputFormat(),
                request.tone(),
                request.modelQuality(),
                request.campaignIdea(),
                request.headline(),
                request.subheadline(),
                request.offerText(),
                request.cta(),
                request.campaignObjective(),
                request.targetAudience(),
                request.productDescription(),
                request.includeCta(),
                request.includeLogo(),
                request.includeTypography(),
                request.versions(),
                request.existingAssetId(),
                request.logoAssetId(),
                request.noHumanModel(),
                request.size(),
                request.quality(),
                request.background(),
                null,
                null,
                null,
                null,
                null));
    }

    @PostMapping(value = "/api/v1/ai/creatives/generate", consumes = MediaType.TEXT_PLAIN_VALUE)
    @PreAuthorize("hasAuthority('CREATIVE_GENERATE')")
    public ApiResponse<AiCreativeResponse> generatePlainJson(@RequestBody String body) {
        try {
            AiCreativeGenerateRequest request = objectMapper.readValue(body, AiCreativeGenerateRequest.class);
            return generateJson(request);
        } catch (JacksonException exception) {
            throw new IllegalArgumentException("Request body must be valid JSON");
        }
    }

    @PostMapping(value = "/api/v1/ai/creatives/generate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('CREATIVE_GENERATE')")
    public ApiResponse<AiCreativeResponse> generate(MultipartHttpServletRequest form) {
        return ApiResponse.success(generateInternal(
                requiredUuid(form, "workspaceId"),
                requiredUuid(form, "brandId"),
                optionalUuid(form, "productServiceId"),
                optionalUuid(form, "campaignId"),
                requiredEnum(form, "platform", CreativePlatform.class),
                value(form, "language", "English"),
                requiredEnum(form, "creativeType", CreativeType.class),
                enumValue(form, "outputFormat", OutputFormat.class, OutputFormat.png),
                enumValue(form, "tone", CreativeTone.class, CreativeTone.PREMIUM),
                enumValue(form, "modelQuality", ModelQuality.class, ModelQuality.BASIC),
                value(form, "campaignIdea", null),
                value(form, "headline", null),
                value(form, "subheadline", null),
                value(form, "offerText", null),
                value(form, "cta", null),
                value(form, "campaignObjective", null),
                value(form, "targetAudience", null),
                value(form, "productDescription", null),
                booleanValue(form, "includeCta", true),
                booleanValue(form, "includeLogo", true),
                booleanValue(form, "includeTypography", true),
                intValue(form, "versions"),
                optionalUuid(form, "existingAssetId"),
                optionalUuid(form, "logoAssetId"),
                booleanValue(form, "noHumanModel", true),
                value(form, "size", null),
                enumValue(form, "quality", CreativeQuality.class, null),
                value(form, "background", "opaque"),
                value(form, "backgroundPrompt", null),
                form.getFile("productImage"),
                form.getFile("logoImage"),
                form.getFile("referenceImage"),
                form.getFile("maskImage")));
    }

    @PostMapping("/api/v1/ai/creatives/text")
    @PreAuthorize("hasAuthority('CREATIVE_GENERATE')")
    public ApiResponse<AiCreativeResponse> text(@Valid @RequestBody TextCreativeRequest request) {
        return ApiResponse.success(service.generateText(request));
    }

    @PostMapping(value = "/api/v1/ai/creatives/product-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('CREATIVE_GENERATE')")
    public ApiResponse<AiCreativeResponse> productImage(
            @RequestParam("workspaceId") UUID workspaceId,
            @RequestParam("brandId") UUID brandId,
            @RequestParam(value = "productServiceId", required = false) UUID productServiceId,
            @RequestParam(value = "campaignId", required = false) UUID campaignId,
            @RequestParam("platform") CreativePlatform platform,
            @RequestParam("creativeType") CreativeType creativeType,
            @RequestParam(value = "headline", required = false) String headline,
            @RequestParam(value = "cta", required = false) String cta,
            @RequestParam(value = "language", defaultValue = "English") String language,
            @RequestParam(value = "tone", defaultValue = "PREMIUM") CreativeTone tone,
            @RequestParam(value = "modelQuality", defaultValue = "BASIC") ModelQuality modelQuality,
            @RequestParam(value = "size", required = false) String size,
            @RequestParam(value = "quality", required = false) CreativeQuality quality,
            @RequestParam(value = "outputFormat", defaultValue = "png") OutputFormat outputFormat,
            @RequestParam(value = "background", defaultValue = "opaque") String background,
            @RequestPart("productImage") MultipartFile productImage
    ) {
        return ApiResponse.success(generateInternal(workspaceId, brandId, productServiceId, campaignId, platform, language, creativeType, outputFormat,
                tone, modelQuality, null, headline, null, null, cta, null, null, null, true, true, true, null, null, null, true, size, quality, background, null,
                productImage, null, null, null));
    }

    @PostMapping(value = "/api/v1/ai/creatives/multi-reference", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('CREATIVE_GENERATE')")
    public ApiResponse<AiCreativeResponse> multiReference(
            @RequestParam("workspaceId") UUID workspaceId,
            @RequestParam("brandId") UUID brandId,
            @RequestParam(value = "productServiceId", required = false) UUID productServiceId,
            @RequestParam(value = "campaignId", required = false) UUID campaignId,
            @RequestParam("platform") CreativePlatform platform,
            @RequestParam("creativeType") CreativeType creativeType,
            @RequestParam(value = "headline", required = false) String headline,
            @RequestParam(value = "cta", required = false) String cta,
            @RequestParam(value = "language", defaultValue = "English") String language,
            @RequestParam(value = "tone", defaultValue = "PREMIUM") CreativeTone tone,
            @RequestParam(value = "modelQuality", defaultValue = "BASIC") ModelQuality modelQuality,
            @RequestParam(value = "outputFormat", defaultValue = "png") OutputFormat outputFormat,
            @RequestPart("productImage") MultipartFile productImage,
            @RequestPart(value = "logoImage", required = false) MultipartFile logoImage,
            @RequestPart(value = "referenceImage", required = false) MultipartFile referenceImage
    ) {
        return ApiResponse.success(generateInternal(workspaceId, brandId, productServiceId, campaignId, platform, language, creativeType, outputFormat,
                tone, modelQuality, null, headline, null, null, cta, null, null, null, true, true, true, null, null, null, true, null, null, "opaque", null,
                productImage, logoImage, referenceImage, null));
    }

    @PostMapping(value = "/api/v1/ai/creatives/background-replace", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('CREATIVE_GENERATE')")
    public ApiResponse<AiCreativeResponse> backgroundReplace(
            @RequestParam("workspaceId") UUID workspaceId,
            @RequestParam("brandId") UUID brandId,
            @RequestParam(value = "campaignId", required = false) UUID campaignId,
            @RequestParam(value = "backgroundPrompt", required = false) String backgroundPrompt,
            @RequestParam(value = "headline", required = false) String headline,
            @RequestParam(value = "cta", required = false) String cta,
            @RequestParam(value = "platform", defaultValue = "FACEBOOK") CreativePlatform platform,
            @RequestParam(value = "creativeType", defaultValue = "SQUARE_POST") CreativeType creativeType,
            @RequestParam(value = "language", defaultValue = "English") String language,
            @RequestParam(value = "tone", defaultValue = "PREMIUM") CreativeTone tone,
            @RequestParam(value = "outputFormat", defaultValue = "png") OutputFormat outputFormat,
            @RequestPart("originalImage") MultipartFile originalImage,
            @RequestPart("maskImage") MultipartFile maskImage
    ) {
        return ApiResponse.success(generateInternal(workspaceId, brandId, null, campaignId, platform, language, creativeType, outputFormat,
                tone, ModelQuality.PREMIUM, null, headline, null, null, cta, null, null, null, true, true, true, null, null, null, true, null, CreativeQuality.high, "opaque", backgroundPrompt,
                originalImage, null, null, maskImage));
    }

    @PostMapping(value = "/api/v1/ai/creatives/transparent-asset", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('CREATIVE_GENERATE')")
    public ApiResponse<AiCreativeResponse> transparentAsset(
            @RequestParam("workspaceId") UUID workspaceId,
            @RequestParam("brandId") UUID brandId,
            @RequestParam(value = "campaignId", required = false) UUID campaignId,
            @RequestParam(value = "platform", defaultValue = "OTHER") CreativePlatform platform,
            @RequestParam(value = "creativeType", defaultValue = "PRODUCT_AD") CreativeType creativeType,
            @RequestParam(value = "language", defaultValue = "English") String language,
            @RequestParam(value = "outputFormat", defaultValue = "png") OutputFormat outputFormat,
            @RequestPart(value = "productImage", required = false) MultipartFile productImage
    ) {
        return ApiResponse.success(generateInternal(workspaceId, brandId, null, campaignId, platform, language, creativeType, outputFormat,
                CreativeTone.PREMIUM, ModelQuality.PREMIUM, null, null, null, null, null, null, null, null, false, true, false, null, null, null, true, null, CreativeQuality.high, "transparent", null,
                productImage, null, null, null));
    }

    @GetMapping("/api/v1/ai/creatives/{creativeId}/progress")
    @PreAuthorize("hasAuthority('WORKSPACE_VIEW')")
    public ApiResponse<Map<String, Object>> progress(@PathVariable UUID creativeId) {
        return ApiResponse.success(service.progress(creativeId));
    }

    private AiCreativeResponse generateInternal(
            UUID workspaceId,
            UUID brandId,
            UUID productServiceId,
            UUID campaignId,
            CreativePlatform platform,
            String language,
            CreativeType creativeType,
            OutputFormat outputFormat,
            CreativeTone tone,
            ModelQuality modelQuality,
            String campaignIdea,
            String headline,
            String subheadline,
            String offerText,
            String cta,
            String campaignObjective,
            String targetAudience,
            String productDescription,
            Boolean includeCta,
            Boolean includeLogo,
            Boolean includeTypography,
            Integer versions,
            UUID existingAssetId,
            UUID logoAssetId,
            Boolean noHumanModel,
            String size,
            CreativeQuality quality,
            String background,
            String backgroundPrompt,
            MultipartFile productImage,
            MultipartFile logoImage,
            MultipartFile referenceImage,
            MultipartFile maskImage
    ) {
        return service.generate(new AiCreativeGenerateRequest(
                workspaceId,
                brandId,
                productServiceId,
                campaignId,
                platform,
                language,
                creativeType,
                outputFormat,
                tone,
                modelQuality,
                campaignIdea,
                headline,
                subheadline,
                offerText,
                cta,
                campaignObjective,
                targetAudience,
                productDescription,
                includeCta,
                includeLogo,
                includeTypography,
                versions,
                existingAssetId,
                logoAssetId,
                noHumanModel,
                size,
                quality,
                background,
                null,
                null,
                currentUserContext.requireCurrentUser().userId()), productImage, logoImage, referenceImage, maskImage, backgroundPrompt);
    }

    private String value(MultipartHttpServletRequest form, String name, String defaultValue) {
        String value = form.getParameter(name);
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    private UUID requiredUuid(MultipartHttpServletRequest form, String name) {
        String value = value(form, name, null);
        if (value == null) {
            throw new IllegalArgumentException(name + " is required");
        }
        return parseUuid(name, value);
    }

    private UUID optionalUuid(MultipartHttpServletRequest form, String name) {
        String value = value(form, name, null);
        return value == null ? null : parseUuid(name, value);
    }

    private UUID parseUuid(String name, String value) {
        try {
            return UUID.fromString(value);
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException(name + " must be a valid UUID");
        }
    }

    private Integer intValue(MultipartHttpServletRequest form, String name) {
        String value = value(form, name, null);
        if (value == null) {
            return null;
        }
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(name + " must be a valid number");
        }
    }

    private Boolean booleanValue(MultipartHttpServletRequest form, String name, boolean defaultValue) {
        String value = value(form, name, null);
        return value == null ? defaultValue : Boolean.valueOf(value);
    }

    private <T extends Enum<T>> T requiredEnum(MultipartHttpServletRequest form, String name, Class<T> enumType) {
        String value = value(form, name, null);
        if (value == null) {
            throw new IllegalArgumentException(name + " is required");
        }
        return parseEnum(name, enumType, value);
    }

    private <T extends Enum<T>> T enumValue(MultipartHttpServletRequest form, String name, Class<T> enumType, T defaultValue) {
        String value = value(form, name, null);
        return value == null ? defaultValue : parseEnum(name, enumType, value);
    }

    private <T extends Enum<T>> T parseEnum(String name, Class<T> enumType, String value) {
        try {
            return Enum.valueOf(enumType, value.trim());
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException(name + " has an invalid value");
        }
    }
}
