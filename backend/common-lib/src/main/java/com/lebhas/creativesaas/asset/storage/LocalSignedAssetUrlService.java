package com.lebhas.creativesaas.asset.storage;

import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

@Component
public class LocalSignedAssetUrlService {

    private final StorageProperties storageProperties;
    private final Clock clock;

    public LocalSignedAssetUrlService(StorageProperties storageProperties, Clock clock) {
        this.storageProperties = storageProperties;
        this.clock = clock;
    }

    public StorageService.SignedAssetUrl createUrl(UUID assetId, LocalAssetAccessMode mode) {
        Instant expiresAt = clock.instant().plus(storageProperties.getSignedUrlTtl());
        String signature = sign(assetId, mode, expiresAt.getEpochSecond());
        String url = storageProperties.getLocal().getBaseUrl()
                .resolve("/internal/storage/local/assets/" + assetId + "/" + mode.name().toLowerCase()
                        + "?expiresAt=" + expiresAt.getEpochSecond()
                        + "&signature=" + signature)
                .toString();
        return new StorageService.SignedAssetUrl(url, expiresAt);
    }

    public void verify(UUID assetId, LocalAssetAccessMode mode, long expiresAtEpochSeconds, String signature) {
        Instant expiresAt = Instant.ofEpochSecond(expiresAtEpochSeconds);
        if (expiresAt.isBefore(clock.instant())) {
            throw new BusinessException(ErrorCode.ASSET_URL_EXPIRED);
        }
        String expected = sign(assetId, mode, expiresAtEpochSeconds);
        if (signature == null || !MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                signature.getBytes(StandardCharsets.UTF_8))) {
            throw new BusinessException(ErrorCode.ASSET_URL_INVALID);
        }
    }

    private String sign(UUID assetId, LocalAssetAccessMode mode, long expiresAtEpochSeconds) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(
                    storageProperties.getLocal().getSigningSecret().getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"));
            byte[] digest = mac.doFinal((assetId + "|" + mode.name() + "|" + expiresAtEpochSeconds).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.ASSET_STORAGE_FAILURE, "Asset URL signer could not be initialized");
        }
    }
}
