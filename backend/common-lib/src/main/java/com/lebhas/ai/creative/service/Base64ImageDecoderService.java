package com.lebhas.ai.creative.service;

import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;

@Service
public class Base64ImageDecoderService {

    private static final int MAX_IMAGE_BYTES = 30 * 1024 * 1024;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public byte[] decodeImage(String b64Json) {
        return decodeImage(b64Json, null);
    }

    public byte[] decodeImage(String b64Json, String imageUrl) {
        byte[] bytes;
        if (StringUtils.hasText(b64Json)) {
            bytes = decodeBase64(b64Json);
        } else if (StringUtils.hasText(imageUrl)) {
            bytes = downloadImage(imageUrl);
        } else {
            throw new BusinessException(ErrorCode.GENERATION_PROVIDER_RESPONSE_INVALID, "OpenAI image response did not include b64_json or url");
        }
        validateSupportedImage(bytes);
        return bytes;
    }

    private byte[] decodeBase64(String b64Json) {
        try {
            return Base64.getDecoder().decode(b64Json);
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.GENERATION_PROVIDER_RESPONSE_INVALID, "OpenAI image b64_json is invalid");
        }
    }

    private byte[] downloadImage(String imageUrl) {
        HttpRequest request;
        try {
            request = HttpRequest.newBuilder(URI.create(imageUrl))
                    .timeout(Duration.ofSeconds(20))
                    .GET()
                    .build();
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.GENERATION_PROVIDER_RESPONSE_INVALID, "OpenAI image URL is invalid");
        }

        try {
            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BusinessException(ErrorCode.GENERATION_PROVIDER_RESPONSE_INVALID, "OpenAI image URL returned HTTP " + response.statusCode());
            }
            try (InputStream body = response.body()) {
                return readBounded(body);
            }
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.GENERATION_PROVIDER_RESPONSE_INVALID, "Failed to download OpenAI image");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.GENERATION_PROVIDER_RESPONSE_INVALID, "Interrupted while downloading OpenAI image");
        }
    }

    private byte[] readBounded(InputStream inputStream) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int total = 0;
        int read;
        while ((read = inputStream.read(buffer)) != -1) {
            total += read;
            if (total > MAX_IMAGE_BYTES) {
                throw new BusinessException(ErrorCode.GENERATION_PROVIDER_RESPONSE_INVALID, "OpenAI image exceeds maximum supported size");
            }
            outputStream.write(buffer, 0, read);
        }
        return outputStream.toByteArray();
    }

    private void validateSupportedImage(byte[] bytes) {
        if (!isPng(bytes) && !isJpeg(bytes) && !isWebp(bytes)) {
            throw new BusinessException(ErrorCode.GENERATION_PROVIDER_RESPONSE_INVALID, "Decoded image format is not supported");
        }
    }

    private boolean isPng(byte[] bytes) {
        byte[] sig = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        return startsWith(bytes, sig);
    }

    private boolean isJpeg(byte[] bytes) {
        return bytes.length >= 3 && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF;
    }

    private boolean isWebp(byte[] bytes) {
        return bytes.length >= 12
                && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P';
    }

    private boolean startsWith(byte[] bytes, byte[] signature) {
        if (bytes.length < signature.length) {
            return false;
        }
        for (int i = 0; i < signature.length; i++) {
            if (bytes[i] != signature[i]) {
                return false;
            }
        }
        return true;
    }
}
