package com.lebhas.creativesaas.sharing.application;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@Service
public class SecureTokenService {

    private static final int TOKEN_SIZE_BYTES = 32;

    private final SecureRandom secureRandom = new SecureRandom();
    private final PasswordEncoder passwordEncoder;

    public SecureTokenService(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    public String generatePublicToken() {
        byte[] value = new byte[TOKEN_SIZE_BYTES];
        secureRandom.nextBytes(value);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    public String hashToken(String rawToken) {
        if (!StringUtils.hasText(rawToken)) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.trim().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 token hashing is unavailable", exception);
        }
    }

    public String encodePassword(String rawPassword) {
        if (!StringUtils.hasText(rawPassword)) {
            return null;
        }
        return passwordEncoder.encode(rawPassword.trim());
    }

    public boolean matchesPassword(String rawPassword, String encodedPassword) {
        if (!StringUtils.hasText(encodedPassword)) {
            return !StringUtils.hasText(rawPassword);
        }
        return StringUtils.hasText(rawPassword) && passwordEncoder.matches(rawPassword.trim(), encodedPassword);
    }

    public boolean hasPassword(String rawPassword) {
        return StringUtils.hasText(rawPassword);
    }
}
