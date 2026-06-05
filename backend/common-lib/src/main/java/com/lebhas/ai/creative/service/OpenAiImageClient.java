package com.lebhas.ai.creative.service;

import com.lebhas.ai.application.AiCredentialEncryptionService;
import com.lebhas.ai.creative.dto.OpenAiImageResponse;
import com.lebhas.ai.creative.enums.CreativeQuality;
import com.lebhas.ai.creative.enums.OutputFormat;
import com.lebhas.ai.domain.CredentialStatus;
import com.lebhas.ai.domain.ProviderStatus;
import com.lebhas.ai.infrastructure.persistence.AiProviderCredentialRepository;
import com.lebhas.ai.infrastructure.persistence.AiToolProviderRepository;
import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.generation.application.CreativeGenerationProperties;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class OpenAiImageClient {

    private static final String OPENAI_PROVIDER_CODE = "OPENAI";

    private final CreativeGenerationProperties properties;
    private final OpenAiDirectProperties directProperties;
    private final AiToolProviderRepository providerRepository;
    private final AiProviderCredentialRepository credentialRepository;
    private final AiCredentialEncryptionService encryptionService;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public OpenAiImageClient(
            CreativeGenerationProperties properties,
            OpenAiDirectProperties directProperties,
            AiToolProviderRepository providerRepository,
            AiProviderCredentialRepository credentialRepository,
            AiCredentialEncryptionService encryptionService,
            ObjectMapper objectMapper
    ) {
        this.properties = properties;
        this.directProperties = directProperties;
        this.providerRepository = providerRepository;
        this.credentialRepository = credentialRepository;
        this.encryptionService = encryptionService;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder().connectTimeout(timeout()).build();
    }

    public OpenAiImageResponse generateImage(String prompt, String size, CreativeQuality quality, OutputFormat format, String background) {
        CreativeGenerationProperties.OpenAi openAi = properties.getOpenAi();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", resolveModel(openAi));
        payload.put("prompt", prompt);
        payload.put("size", size);
        payload.put("quality", quality.name());
        payload.put("background", background);
        payload.put("output_format", format.name());
        payload.put("n", 1);
        return sendJson(resolveUri(openAi, "/v1/images/generations"), payload, openAi);
    }

    public OpenAiImageResponse editImage(
            String prompt,
            String size,
            CreativeQuality quality,
            OutputFormat format,
            String background,
            MultipartFile productImage,
            MultipartFile logoImage,
            MultipartFile referenceImage,
            MultipartFile maskImage
    ) {
        CreativeGenerationProperties.OpenAi openAi = properties.getOpenAi();
        String boundary = "lebhas-openai-" + UUID.randomUUID();
        List<byte[]> parts = new ArrayList<>();
        addTextPart(parts, boundary, "model", resolveModel(openAi));
        addTextPart(parts, boundary, "prompt", prompt);
        addTextPart(parts, boundary, "size", size);
        addTextPart(parts, boundary, "quality", quality.name());
        addTextPart(parts, boundary, "background", background);
        addTextPart(parts, boundary, "output_format", format.name());
        addTextPart(parts, boundary, "n", "1");
        addFilePart(parts, boundary, "image", productImage);
        addFilePart(parts, boundary, "image[]", logoImage);
        addFilePart(parts, boundary, "image[]", referenceImage);
        addFilePart(parts, boundary, "mask", maskImage);
        parts.add(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        return sendMultipart(resolveUri(openAi, "/v1/images/edits"), parts, boundary, openAi);
    }

    public String model() {
        return resolveModel(properties.getOpenAi());
    }

    private OpenAiImageResponse sendJson(URI uri, Map<String, Object> payload, CreativeGenerationProperties.OpenAi openAi) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
                    .timeout(timeout())
                    .header("Authorization", "Bearer " + requireApiKey(openAi))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)));
            addOptionalHeader(builder, "OpenAI-Organization", openAi.getOrganization());
            addOptionalHeader(builder, "OpenAI-Project", openAi.getProject());
            HttpRequest request = builder.build();
            return parseResponse(httpClient.send(request, HttpResponse.BodyHandlers.ofString()));
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.GENERATION_PROVIDER_UNAVAILABLE, "OpenAI image generation is unavailable");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.GENERATION_PROVIDER_UNAVAILABLE, "OpenAI image generation was interrupted");
        }
    }

    private OpenAiImageResponse sendMultipart(URI uri, List<byte[]> parts, String boundary, CreativeGenerationProperties.OpenAi openAi) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
                    .timeout(timeout())
                    .header("Authorization", "Bearer " + requireApiKey(openAi))
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(HttpRequest.BodyPublishers.ofByteArrays(parts));
            addOptionalHeader(builder, "OpenAI-Organization", openAi.getOrganization());
            addOptionalHeader(builder, "OpenAI-Project", openAi.getProject());
            HttpRequest request = builder.build();
            return parseResponse(httpClient.send(request, HttpResponse.BodyHandlers.ofString()));
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.GENERATION_PROVIDER_UNAVAILABLE, "OpenAI image edit is unavailable");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.GENERATION_PROVIDER_UNAVAILABLE, "OpenAI image edit was interrupted");
        }
    }

    private OpenAiImageResponse parseResponse(HttpResponse<String> response) throws IOException {
        if (response.statusCode() >= 400) {
            throw toProviderException(response.statusCode());
        }
        return objectMapper.readValue(response.body(), OpenAiImageResponse.class);
    }

    private BusinessException toProviderException(int statusCode) {
        if (statusCode == 401 || statusCode == 403) {
            return new BusinessException(ErrorCode.GENERATION_PROVIDER_UNAVAILABLE, "OpenAI rejected the configured backend API key");
        }
        if (statusCode >= 500) {
            return new BusinessException(ErrorCode.GENERATION_PROVIDER_UNAVAILABLE, "OpenAI image provider is temporarily unavailable");
        }
        return new BusinessException(ErrorCode.GENERATION_PROVIDER_REQUEST_FAILED, "OpenAI image provider rejected the request");
    }

    private void addTextPart(List<byte[]> parts, String boundary, String name, String value) {
        parts.add(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"" + name + "\"\r\n\r\n"
                + (value == null ? "" : value) + "\r\n").getBytes(StandardCharsets.UTF_8));
    }

    private void addFilePart(List<byte[]> parts, String boundary, String name, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return;
        }
        try {
            parts.add(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"" + name + "\"; filename=\"" + sanitize(file.getOriginalFilename()) + "\"\r\n"
                    + "Content-Type: " + (file.getContentType() == null ? "application/octet-stream" : file.getContentType()) + "\r\n\r\n")
                    .getBytes(StandardCharsets.UTF_8));
            parts.add(file.getBytes());
            parts.add("\r\n".getBytes(StandardCharsets.UTF_8));
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.ASSET_FILE_CONTENT_INVALID, "Uploaded image content could not be read");
        }
    }

    private String resolveModel(CreativeGenerationProperties.OpenAi openAi) {
        if (StringUtils.hasText(directProperties.getDefaultImageModel())) {
            return directProperties.getDefaultImageModel().trim();
        }
        return StringUtils.hasText(openAi.getModel()) ? openAi.getModel().trim() : "gpt-image-1.5";
    }

    private URI resolveUri(CreativeGenerationProperties.OpenAi openAi, String defaultPath) {
        String configuredBaseUrl = StringUtils.hasText(directProperties.getBaseUrl()) ? directProperties.getBaseUrl() : openAi.getBaseUrl();
        String baseUrl = configuredBaseUrl.endsWith("/") ? configuredBaseUrl.substring(0, configuredBaseUrl.length() - 1) : configuredBaseUrl;
        return URI.create(baseUrl + defaultPath);
    }

    private String requireApiKey(CreativeGenerationProperties.OpenAi openAi) {
        String configuredCredential = resolveConfiguredProviderCredential();
        if (StringUtils.hasText(configuredCredential)) {
            return configuredCredential.trim();
        }
        if (StringUtils.hasText(directProperties.getApiKey())) {
            return directProperties.getApiKey().trim();
        }
        if (!StringUtils.hasText(openAi.getApiKey())) {
            throw new BusinessException(ErrorCode.GENERATION_PROVIDER_UNAVAILABLE, "OPENAI_API_KEY is not configured on the backend");
        }
        return openAi.getApiKey().trim();
    }

    private String resolveConfiguredProviderCredential() {
        return providerRepository.findByProviderCodeAndDeletedFalse(OPENAI_PROVIDER_CODE)
                .filter(provider -> provider.isEnabled() && provider.getStatus() == ProviderStatus.ACTIVE)
                .flatMap(provider -> credentialRepository
                        .findFirstByProviderIdAndActiveTrueAndDeletedFalseOrderByUpdatedAtDesc(provider.getId()))
                .filter(credential -> credential.getCredentialStatus() == CredentialStatus.CONFIGURED)
                .map(this::decryptConfiguredCredential)
                .filter(StringUtils::hasText)
                .orElse(null);
    }

    private String decryptConfiguredCredential(com.lebhas.ai.domain.AiProviderCredential credential) {
        try {
            return encryptionService.decryptNullable(credential.getEncryptedSecret());
        } catch (BusinessException exception) {
            throw new BusinessException(
                    ErrorCode.GENERATION_PROVIDER_UNAVAILABLE,
                    "Configured OpenAI credential cannot be used. Re-save the OpenAI credential in Master Provider Settings.");
        }
    }

    private java.time.Duration timeout() {
        return java.time.Duration.ofSeconds(Math.max(1, directProperties.getTimeoutSeconds()));
    }

    private void addOptionalHeader(HttpRequest.Builder builder, String name, String value) {
        if (StringUtils.hasText(value)) {
            builder.header(name, value.trim());
        }
    }

    private String sanitize(String fileName) {
        return StringUtils.hasText(fileName) ? fileName.replace("\"", "").replace("\r", "").replace("\n", "") : "image.png";
    }
}
