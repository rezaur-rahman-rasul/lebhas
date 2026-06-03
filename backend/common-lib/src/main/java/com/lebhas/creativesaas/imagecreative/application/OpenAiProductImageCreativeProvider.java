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
            OpenAiImageResult result = requestImage(openAi, apiKey, model, context, index, productImage);
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
            byte[] productImage
    ) {
        String responseBody;
        try {
            String boundary = "lebhas-openai-" + UUID.randomUUID();
            HttpRequest request = HttpRequest.newBuilder(resolveEditUri(openAi))
                    .timeout(properties.getRequestTimeout())
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .headers(optionalHeader("OpenAI-Organization", openAi.getOrganization()))
                    .headers(optionalHeader("OpenAI-Project", openAi.getProject()))
                    .POST(buildMultipartBody(boundary, model, context, variant, productImage))
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
            byte[] productImage
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
        addFilePart(
                parts,
                boundary,
                "image",
                context.productAsset().getOriginalFileName(),
                context.productAsset().getMimeType(),
                productImage);
        parts.add(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        return HttpRequest.BodyPublishers.ofByteArrays(parts);
    }

    private boolean isGptImageModel(String model) {
        return model != null && model.trim().toLowerCase().startsWith("gpt-image-");
    }

    private String buildPrompt(ProductImageCreativeContext context, int variant) {
        String productName = context.product() == null || context.product().getName() == null
                ? "the uploaded product"
                : context.product().getName();
        String cta = StringUtils.hasText(context.request().cta()) ? context.request().cta().trim() : "Shop Now";
        return """
                Create a polished ecommerce campaign creative.
                Product: %s
                Campaign idea: %s
                Platform: %s
                Format: %s
                Language: %s
                Style preset: %s
                Background style: %s
                CTA: %s
                Variant: %s
                Use the uploaded product image as the primary visual subject. Preserve the product's identity, shape, and key details.
                Keep text minimal and readable.
                """.formatted(
                productName,
                context.request().sourcePrompt(),
                context.request().platform(),
                context.request().creativeFormat(),
                context.request().language(),
                context.request().stylePreset() == null ? "default" : context.request().stylePreset(),
                context.request().backgroundStyle() == null ? "default" : context.request().backgroundStyle(),
                cta,
                variant);
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

    private String[] optionalHeader(String name, String value) {
        if (!StringUtils.hasText(value)) {
            return new String[0];
        }
        return new String[]{name, value.trim()};
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
