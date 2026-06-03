package com.lebhas.ai.application;

import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import org.springframework.core.env.Environment;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

public class AiCredentialEncryptionService {

    private static final String KEY_PROPERTY = "ai.credentials.encryption-key";
    private static final String KEY_ENV = "AI_CREDENTIAL_ENCRYPTION_KEY";
    private static final String PREFIX = "enc:v1:";
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;

    private final Environment environment;
    private final SecureRandom secureRandom = new SecureRandom();

    public AiCredentialEncryptionService(Environment environment) {
        this.environment = environment;
    }

    public String encryptNullable(String rawValue, String existingEncryptedValue) {
        if (rawValue == null) {
            return existingEncryptedValue;
        }
        if (rawValue.isBlank()) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_BYTES];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(encryptionKey(), "AES"), new GCMParameterSpec(TAG_BITS, iv));
            byte[] cipherText = cipher.doFinal(rawValue.trim().getBytes(StandardCharsets.UTF_8));
            return PREFIX + Base64.getEncoder().encodeToString(iv) + ":" + Base64.getEncoder().encodeToString(cipherText);
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "AI provider credential encryption failed");
        }
    }

    public String maskNullable(String rawValue, String existingMaskedValue) {
        if (rawValue == null) {
            return existingMaskedValue;
        }
        if (rawValue.isBlank()) {
            return null;
        }
        String trimmed = rawValue.trim();
        if (trimmed.length() <= 6) {
            return "****";
        }
        return trimmed.substring(0, 3) + "****" + trimmed.substring(trimmed.length() - 3);
    }

    public String decryptNullable(String encryptedValue) {
        if (encryptedValue == null || encryptedValue.isBlank()) {
            return null;
        }
        if (!encryptedValue.startsWith(PREFIX)) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "AI provider credential format is invalid");
        }
        try {
            String payload = encryptedValue.substring(PREFIX.length());
            String[] parts = payload.split(":", 2);
            if (parts.length != 2) {
                throw new IllegalArgumentException();
            }
            byte[] iv = Base64.getDecoder().decode(parts[0]);
            byte[] cipherText = Base64.getDecoder().decode(parts[1]);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(encryptionKey(), "AES"), new GCMParameterSpec(TAG_BITS, iv));
            return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "AI provider credential decryption failed");
        }
    }

    private byte[] encryptionKey() {
        String configured = environment.getProperty(KEY_PROPERTY);
        if (configured == null || configured.isBlank()) {
            configured = environment.getProperty(KEY_ENV);
        }
        if (configured == null || configured.isBlank()) {
            configured = System.getenv(KEY_ENV);
        }
        if (configured == null || configured.isBlank()) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "AI credential encryption key is not configured");
        }
        byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(configured.trim());
        } catch (IllegalArgumentException ignored) {
            decoded = configured.trim().getBytes(StandardCharsets.UTF_8);
        }
        if (decoded.length != 16 && decoded.length != 24 && decoded.length != 32) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "AI credential encryption key must be 16, 24, or 32 bytes");
        }
        return decoded;
    }
}
