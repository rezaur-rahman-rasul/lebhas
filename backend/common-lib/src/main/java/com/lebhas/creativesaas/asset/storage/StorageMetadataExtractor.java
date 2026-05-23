package com.lebhas.creativesaas.asset.storage;

import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;

@Component("assetStorageMetadataExtractor")
public class StorageMetadataExtractor {

    public String calculateSha256(StorageObjectRequest.ContentStreamSupplier contentStreamSupplier) {
        try (InputStream inputStream = contentStreamSupplier.openStream()) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = inputStream.read(buffer)) >= 0) {
                if (read > 0) {
                    digest.update(buffer, 0, read);
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (IOException | NoSuchAlgorithmException exception) {
            throw new BusinessException(ErrorCode.STORAGE_FILE_HASH_FAILED, "Storage file hash could not be calculated");
        }
    }

    public String resolveFileExtension(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return null;
        }
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot < 0 || lastDot == fileName.length() - 1) {
            return null;
        }
        return fileName.substring(lastDot + 1).trim().toLowerCase(Locale.ROOT);
    }
}
