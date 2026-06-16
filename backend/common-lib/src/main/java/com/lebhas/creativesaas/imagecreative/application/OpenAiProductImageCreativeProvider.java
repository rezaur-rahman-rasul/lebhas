package com.lebhas.creativesaas.imagecreative.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lebhas.ai.application.AiCredentialEncryptionService;
import com.lebhas.ai.application.dto.ResolvedProviderRouteView;
import com.lebhas.ai.domain.AiProviderCredential;
import com.lebhas.ai.domain.CredentialStatus;
import com.lebhas.ai.domain.ProviderEnvironment;
import com.lebhas.ai.infrastructure.persistence.AiModelRepository;
import com.lebhas.ai.infrastructure.persistence.AiProviderCredentialRepository;
import com.lebhas.creativesaas.asset.storage.StorageService;
import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.generation.application.CreativeGenerationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class OpenAiProductImageCreativeProvider implements ProductImageCreativeProvider {

    private static final String OUTPUT_MIME_TYPE = "image/png";
    private static final String OUTPUT_EXTENSION = "png";

    private final CreativeGenerationProperties properties;
    private final AiProviderCredentialRepository credentialRepository;
    private final AiModelRepository modelRepository;
    private final AiCredentialEncryptionService encryptionService;
    private final StorageService storageService;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public OpenAiProductImageCreativeProvider(
            CreativeGenerationProperties properties,
            AiProviderCredentialRepository credentialRepository,
            AiModelRepository modelRepository,
            AiCredentialEncryptionService encryptionService,
            StorageService storageService,
            ObjectMapper objectMapper
    ) {
        this.properties = properties;
        this.credentialRepository = credentialRepository;
        this.modelRepository = modelRepository;
        this.encryptionService = encryptionService;
        this.storageService = storageService;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.getRequestTimeout())
                .build();
    }

    @Override
    public List<ProductImageCreativeProviderOutput> generate(
            ProductImageCreativeContext context,
            int count,
            ResolvedProviderRouteView route
    ) {
        if (route == null || route.providerId() == null) {
            throw new BusinessException(ErrorCode.AI_ROUTING_POLICY_INVALID, "OpenAI provider route is not resolved for campaign creative generation");
        }
        if (context.productAsset() == null) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "productAssetId is required");
        }

        CreativeGenerationProperties.OpenAi openAi = properties.getOpenAi();
        String apiKey = resolveApiKey(route);
        String model = resolveModel(route, openAi);
        byte[] productImage = storageService.readBytes(context.productAsset());
        if (productImage.length == 0) {
            throw new BusinessException(ErrorCode.ASSET_STORAGE_FAILURE, "Selected product image content is empty");
        }
        List<ProductImageCreativeProviderOutput> outputs = new ArrayList<>();
        for (int index = 1; index <= count; index++) {
            OpenAiImageResult result = requestImage(openAi, apiKey, model, context, index, productImage, null);
            String objectKey = "image-creatives/%s/%s/%s-v%s.png".formatted(
                    context.project().getWorkspaceId(),
                    context.project().getId(),
                    context.generationId(),
                    index);
            Map<String, Object> metadata = new LinkedHashMap<>(result.metadata());
            metadata.put("provider", "OPENAI");
            metadata.put("model", model);
            metadata.put("variant", index);
            metadata.put("openAiEndpoint", resolveEditUri(openAi).toString());
            outputs.add(new ProductImageCreativeProviderOutput(
                    objectKey,
                    "image-creative-%s.png".formatted(index),
                    OUTPUT_MIME_TYPE,
                    OUTPUT_EXTENSION,
                    context.request().creativeFormat().width(),
                    context.request().creativeFormat().height(),
                    result.content(),
                    metadata));
        }
        return outputs;
    }

    private String resolveApiKey(ResolvedProviderRouteView route) {
        AiProviderCredential credential = credentialRepository
                .findFirstByProviderIdAndEnvironmentAndDeletedFalseOrderByUpdatedAtDesc(route.providerId(), ProviderEnvironment.SANDBOX)
                .or(() -> credentialRepository.findFirstByProviderIdAndActiveTrueAndDeletedFalseOrderByUpdatedAtDesc(route.providerId()))
                .filter(AiProviderCredential::isActive)
                .filter(item -> item.getCredentialStatus() == CredentialStatus.CONFIGURED)
                .filter(item -> StringUtils.hasText(item.getEncryptedSecret()))
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.GENERATION_PROVIDER_UNAVAILABLE,
                        "OpenAI provider credential is not configured for campaign creative generation"));

        String decrypted = encryptionService.decryptNullable(credential.getEncryptedSecret());
        if (!StringUtils.hasText(decrypted)) {
            throw new BusinessException(ErrorCode.GENERATION_PROVIDER_UNAVAILABLE, "OpenAI provider credential is empty");
        }
        return decrypted.trim();
    }

    private String resolveModel(ResolvedProviderRouteView route, CreativeGenerationProperties.OpenAi openAi) {
        if (route.modelId() != null) {
            return modelRepository.findByIdAndProviderIdAndDeletedFalse(route.modelId(), route.providerId())
                    .map(model -> model.getModelCode())
                    .filter(StringUtils::hasText)
                    .orElseGet(openAi::getModel);
        }
        return StringUtils.hasText(openAi.getModel()) ? openAi.getModel().trim() : "gpt-image-1";
    }

    private OpenAiImageResult requestImage(
            CreativeGenerationProperties.OpenAi openAi,
            String apiKey,
            String model,
            ProductImageCreativeContext context,
            int variant,
            byte[] productImage,
            byte[] logoImage
    ) {
        String responseBody;
        try {
            String boundary = "lebhas-openai-" + UUID.randomUUID();
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(resolveEditUri(openAi))
                    .timeout(properties.getRequestTimeout())
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary);
            addOptionalHeader(requestBuilder, "OpenAI-Organization", openAi.getOrganization());
            addOptionalHeader(requestBuilder, "OpenAI-Project", openAi.getProject());
            HttpRequest request = requestBuilder
                    .POST(buildMultipartBody(boundary, model, context, variant, productImage, logoImage))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            responseBody = response.body();
            if (response.statusCode() >= 400) {
                throw toProviderException(response.statusCode(), responseBody);
            }
        } catch (BusinessException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.GENERATION_PROVIDER_UNAVAILABLE, "OpenAI image provider is unavailable");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.GENERATION_PROVIDER_UNAVAILABLE, "OpenAI image request was interrupted");
        }

        return parseResponse(responseBody);
    }

    private HttpRequest.BodyPublisher buildMultipartBody(
            String boundary,
            String model,
            ProductImageCreativeContext context,
            int variant,
            byte[] productImage,
            byte[] logoImage
    ) {
        List<byte[]> parts = new ArrayList<>();
        addTextPart(parts, boundary, "model", model);
        addTextPart(parts, boundary, "prompt", buildPrompt(context, variant));
        addTextPart(parts, boundary, "n", "1");
        addTextPart(parts, boundary, "size", resolveOpenAiSize(context));
        if (isGptImageModel(model)) {
            addTextPart(parts, boundary, "output_format", OUTPUT_EXTENSION);
            addTextPart(parts, boundary, "input_fidelity", context.productAsset() == null ? "low" : "high");
        } else {
            addTextPart(parts, boundary, "response_format", "b64_json");
        }
        boolean hasLogoImage = context.logoAsset() != null && logoImage != null && logoImage.length > 0;
        String imageFieldName = hasLogoImage ? "image[]" : "image";
        addFilePart(
                parts,
                boundary,
                imageFieldName,
                context.productAsset().getOriginalFileName(),
                context.productAsset().getMimeType(),
                productImage);
        if (hasLogoImage) {
            addFilePart(
                    parts,
                    boundary,
                    "image[]",
                    context.logoAsset().getOriginalFileName(),
                    context.logoAsset().getMimeType(),
                    logoImage);
        }
        parts.add(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        return HttpRequest.BodyPublishers.ofByteArrays(parts);
    }

    private boolean isGptImageModel(String model) {
        return model != null && model.trim().toLowerCase().startsWith("gpt-image-");
    }

    private String buildPrompt(ProductImageCreativeContext context, int variant) {
        CreativeGenerationContext generationContext = context.generationContext() == null
                ? CreativeGenerationContext.from(context.project(), context.brand(), context.product(), context.productAsset(), context.request())
                : context.generationContext();
        String productName = promptSafe(context, value(generationContext.productService(), "name", "the uploaded product"), "the uploaded product");
        String brandName = promptSafe(context, value(generationContext.brand(), "name", "the selected brand"), "the selected brand");
        String campaignName = promptSafe(context, value(generationContext.campaign(), "name", "campaign creative"), "campaign creative");
        String ctaInstruction = "CTA: backend overlay only. Do not render CTA text, CTA buttons, readable words, labels, prices, offers, or invented text in the AI image.";
        return """
                Create a polished ecommerce campaign creative using the full Lebhas hierarchy context.
                Brand context:
                - Brand: %s
                - Business type: %s
                - Industry: %s
                - Audience: %s
                - Brand voice: %s
                - Brand colors: %s
                - Website/socials: %s

                Product/service context:
                Product: %s
                - Category: %s
                - Description: %s
                - Selling points / benefits: %s

                Campaign context:
                - Campaign: %s
                - Campaign description: %s
                - Campaign objective: %s
                - Campaign type: %s
                - Target platform: %s

                User prompt / campaign idea:
                %s

                Creative settings:
                - Platform: %s
                - Format: %s
                - Language: %s
                - Tone/style preset: %s
                - Quality: %s
                - Background style: %s
                %s
                Variant: %s
                Use the uploaded product image as the primary visual subject. Preserve the product's identity, shape, and key details.
                Apply brand colors when they are available. Highlight the inherited selling points and campaign objective.
                Do not render any readable text, letters, words, labels, CTA, prices, offer badges, watermarks, or template text.
                Leave clean safe zones for Lebhas internal typography and logo overlay.
                %s
                %s
                """.formatted(
                brandName,
                value(generationContext.brand(), "businessType", "Not specified"),
                value(generationContext.brand(), "industry", "Not specified"),
                promptSafe(context, generationContext.audience(), "Not specified"),
                promptSafe(context, value(generationContext.brand(), "brandVoice", "Not specified"), "Not specified"),
                generationContext.colors().isEmpty() ? "Not specified" : String.join(", ", generationContext.colors()),
                compactSocials(generationContext.brand()),
                productName,
                promptSafe(context, value(generationContext.productService(), "category", "Not specified"), "Not specified"),
                promptSafe(context, value(generationContext.productService(), "description", "Not specified"), "Not specified"),
                promptSafe(context, generationContext.sellingPoints(), "Not specified"),
                campaignName,
                promptSafe(context, value(generationContext.campaign(), "description", "Not specified"), "Not specified"),
                promptSafe(context, generationContext.campaignObjective(), "Not specified"),
                promptSafe(context, value(generationContext.campaign(), "campaignType", "Not specified"), "Not specified"),
                safe(generationContext.platform(), "Not specified"),
                promptDirection(context, generationContext),
                safe(generationContext.platform(), "Not specified"),
                safe(generationContext.creativeType(), "Not specified"),
                safe(generationContext.language(), "English"),
                safe(generationContext.tone(), "default"),
                safe(generationContext.quality(), "default"),
                value(generationContext.creativeRequest(), "backgroundStyle", "default"),
                ctaInstruction,
                variant,
                adaptiveLogoInstruction(context),
                "Typography and logo placement are handled by Lebhas after generation. Create an image-only visual.");
    }

    private String adaptiveLogoInstruction(ProductImageCreativeContext context) {
        return "Brand logo is handled by Lebhas after generation. Do not place, draw, imitate, or reserve a logo inside the AI image.";
    }

    private String promptDirection(ProductImageCreativeContext context, CreativeGenerationContext generationContext) {
        if (context.request().language() != null && "BANGLA".equalsIgnoreCase(context.request().language().name())) {
            return "Premium Bangladesh-market product advertising composition. Leave sufficient empty space for text placement. Do not include any text, letters, words, typography, Bangla characters, English characters, logos, or watermarks. Generate visual elements only.";
        }
        String prompt = value(generationContext.creativeRequest(), "prompt", "Use inherited hierarchy context.");
        return containsBangla(prompt)
                ? "Premium Bangladesh-market product advertising composition. Do not render prompt text in the image."
                : prompt;
    }

    private String promptSafe(ProductImageCreativeContext context, String value, String fallback) {
        String safeValue = safe(value, fallback);
        if (context.request().language() != null
                && "BANGLA".equalsIgnoreCase(context.request().language().name())
                && containsBangla(safeValue)) {
            return "Backend text context only; do not render as visible text";
        }
        return containsBangla(safeValue)
                ? "Backend text context only; do not render as visible text"
                : safeValue;
    }

    private boolean containsBangla(String value) {
        return value != null && value.codePoints().anyMatch(codePoint -> codePoint >= 0x0980 && codePoint <= 0x09FF);
    }

    private String value(Map<String, Object> source, String key, String fallback) {
        if (source == null) {
            return fallback;
        }
        Object value = source.get(key);
        return value == null || !StringUtils.hasText(String.valueOf(value)) ? fallback : String.valueOf(value).trim();
    }

    private String safe(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private String compactSocials(Map<String, Object> brand) {
        if (brand == null || brand.isEmpty()) {
            return "Not specified";
        }
        List<String> values = List.of("website", "facebookUrl", "instagramUrl", "linkedinUrl", "tiktokUrl").stream()
                .map(key -> value(brand, key, null))
                .filter(StringUtils::hasText)
                .toList();
        return values.isEmpty() ? "Not specified" : String.join(", ", values);
    }

    private String resolveOpenAiSize(ProductImageCreativeContext context) {
        int width = context.request().creativeFormat().width();
        int height = context.request().creativeFormat().height();
        if (height > width) {
            return "1024x1536";
        }
        if (width > height) {
            return "1536x1024";
        }
        return "1024x1024";
    }

    private URI resolveEditUri(CreativeGenerationProperties.OpenAi openAi) {
        String baseUrl = openAi.getBaseUrl().endsWith("/")
                ? openAi.getBaseUrl().substring(0, openAi.getBaseUrl().length() - 1)
                : openAi.getBaseUrl();
        String configuredPath = openAi.getImagePath();
        String editPath = configuredPath == null || configuredPath.isBlank()
                ? "/v1/images/edits"
                : configuredPath.replace("/generations", "/edits");
        String path = editPath.startsWith("/") ? editPath : "/" + editPath;
        return URI.create(baseUrl + path);
    }

    private OpenAiImageResult parseResponse(String responseBody) {
        try {
            JsonNode json = objectMapper.readTree(responseBody);
            JsonNode item = json.path("data").path(0);
            byte[] content = decodeContent(item);
            if (content.length == 0) {
                throw new BusinessException(ErrorCode.GENERATION_PROVIDER_RESPONSE_INVALID, "OpenAI image provider returned no image content");
            }

            Map<String, Object> metadata = new LinkedHashMap<>();
            putIfPresent(metadata, "responseId", json.path("id").asText(null));
            putIfPresent(metadata, "created", json.path("created").asText(null));
            putIfPresent(metadata, "revisedPrompt", item.path("revised_prompt").asText(null));
            return new OpenAiImageResult(content, metadata);
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.GENERATION_PROVIDER_RESPONSE_INVALID, "OpenAI image response could not be parsed");
        }
    }

    private byte[] decodeContent(JsonNode item) {
        String base64 = item.path("b64_json").asText(null);
        if (StringUtils.hasText(base64)) {
            return Base64.getDecoder().decode(base64);
        }
        String url = item.path("url").asText(null);
        if (!StringUtils.hasText(url)) {
            return new byte[0];
        }
        try {
            HttpResponse<byte[]> response = httpClient.send(
                    HttpRequest.newBuilder(URI.create(url)).timeout(properties.getRequestTimeout()).GET().build(),
                    HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() >= 400) {
                throw new BusinessException(ErrorCode.GENERATION_PROVIDER_RESPONSE_INVALID, "OpenAI image URL could not be downloaded");
            }
            return response.body();
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.GENERATION_PROVIDER_UNAVAILABLE, "OpenAI generated image download failed");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.GENERATION_PROVIDER_UNAVAILABLE, "OpenAI generated image download was interrupted");
        }
    }

    private void addOptionalHeader(HttpRequest.Builder builder, String name, String value) {
        if (StringUtils.hasText(value)) {
            builder.header(name, value.trim());
        }
    }

    private void addTextPart(List<byte[]> parts, String boundary, String name, String value) {
        parts.add(("--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n"
                + (value == null ? "" : value)
                + "\r\n").getBytes(StandardCharsets.UTF_8));
    }

    private void addFilePart(
            List<byte[]> parts,
            String boundary,
            String name,
            String fileName,
            String mimeType,
            byte[] content
    ) {
        parts.add(("--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"" + name + "\"; filename=\"" + sanitizeFileName(fileName) + "\"\r\n"
                + "Content-Type: " + (StringUtils.hasText(mimeType) ? mimeType.trim() : "application/octet-stream") + "\r\n\r\n")
                .getBytes(StandardCharsets.UTF_8));
        parts.add(content);
        parts.add("\r\n".getBytes(StandardCharsets.UTF_8));
    }

    private String sanitizeFileName(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            return "product-image.png";
        }
        return fileName.replace("\"", "").replace("\r", "").replace("\n", "");
    }

    private void putIfPresent(Map<String, Object> metadata, String key, String value) {
        if (StringUtils.hasText(value)) {
            metadata.put(key, value);
        }
    }

    private BusinessException toProviderException(int statusCode, String responseBody) {
        String message = responseBody == null ? "" : responseBody.replaceAll("\\s+", " ").trim();
        if (message.length() > 240) {
            message = message.substring(0, 240);
        }
        if (statusCode == 401 || statusCode == 403) {
            return new BusinessException(ErrorCode.GENERATION_PROVIDER_UNAVAILABLE, "OpenAI image provider rejected the configured credentials");
        }
        if (statusCode >= 500) {
            return new BusinessException(ErrorCode.GENERATION_PROVIDER_UNAVAILABLE, "OpenAI image provider is temporarily unavailable");
        }
        return new BusinessException(
                ErrorCode.GENERATION_PROVIDER_REQUEST_FAILED,
                "OpenAI image provider rejected the request" + (message.isBlank() ? "" : ": " + message));
    }

    private record OpenAiImageResult(byte[] content, Map<String, Object> metadata) {
    }
}
