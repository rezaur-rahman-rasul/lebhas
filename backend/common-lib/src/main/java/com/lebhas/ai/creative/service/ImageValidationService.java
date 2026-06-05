package com.lebhas.ai.creative.service;

import com.lebhas.ai.creative.enums.OutputFormat;
import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

@Service
public class ImageValidationService {

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of("image/png", "image/jpeg", "image/jpg", "image/webp");
    private static final long MAX_UPLOAD_BYTES = 10L * 1024L * 1024L;

    public void validateImage(MultipartFile file, String field) {
        if (file == null || file.isEmpty()) {
            return;
        }
        if (file.getSize() > MAX_UPLOAD_BYTES) {
            throw new BusinessException(ErrorCode.ASSET_FILE_SIZE_EXCEEDED, field + " exceeds 10MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
            throw new BusinessException(ErrorCode.ASSET_FILE_TYPE_INVALID, field + " must be PNG, JPEG, or WEBP");
        }
    }

    public void validateOutput(String size, OutputFormat outputFormat, String background) {
        if (size != null && !Set.of("1024x1024", "1024x1536", "1536x1024").contains(size)) {
            throw new BusinessException(ErrorCode.GENERATION_VALIDATION_FAILED, "Creative size is not supported");
        }
        if ("transparent".equalsIgnoreCase(background) && outputFormat != OutputFormat.png && outputFormat != OutputFormat.webp) {
            throw new BusinessException(ErrorCode.GENERATION_VALIDATION_FAILED, "Transparent background requires PNG or WEBP output");
        }
    }
}
