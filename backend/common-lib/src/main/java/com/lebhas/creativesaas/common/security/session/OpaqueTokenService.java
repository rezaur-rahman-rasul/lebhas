package com.lebhas.creativesaas.common.security.session;

import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Component
public class OpaqueTokenService {

    private static final int SECRET_SIZE = 32;

    private final SecureRandom secureRandom = new SecureRandom();

    public IssuedOpaqueToken issue() {
        UUID tokenId = UUID.randomUUID();
        byte[] secret = new byte[SECRET_SIZE];
        secureRandom.nextBytes(secret);
        String rawSecret = Base64.getUrlEncoder().withoutPadding().encodeToString(secret);
        return new IssuedOpaqueToken(tokenId, tokenId + "." + rawSecret, hash(rawSecret));
    }

    public ParsedOpaqueToken parse(String rawToken) {
        return parse(rawToken, ErrorCode.TOKEN_INVALID);
    }

    public ParsedOpaqueToken parse(String rawToken, ErrorCode errorCode) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new BusinessException(errorCode);
        }
        String[] parts = rawToken.split("\\.", 2);
        if (parts.length != 2) {
            throw new BusinessException(errorCode);
        }
        try {
            UUID tokenId = UUID.fromString(parts[0]);
            return new ParsedOpaqueToken(tokenId, hash(parts[1]));
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(errorCode);
        }
    }

    public boolean matches(String expectedHash, String rawSecretHash) {
        return MessageDigest.isEqual(
                expectedHash.getBytes(StandardCharsets.UTF_8),
                rawSecretHash.getBytes(StandardCharsets.UTF_8));
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm is required", exception);
        }
    }

    public record IssuedOpaqueToken(UUID tokenId, String value, String hashedSecret) {
    }

    public record ParsedOpaqueToken(UUID tokenId, String hashedSecret) {
    }
}
