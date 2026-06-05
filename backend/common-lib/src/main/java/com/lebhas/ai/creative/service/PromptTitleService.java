package com.lebhas.ai.creative.service;

import com.lebhas.ai.creative.enums.CreativePlatform;
import com.lebhas.ai.creative.enums.CreativeType;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class PromptTitleService {

    private static final DateTimeFormatter FALLBACK_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmm");

    public String createTitle(
            String brandName,
            String productServiceName,
            String campaignName,
            CreativeType creativeType,
            CreativePlatform platform
    ) {
        String brand = safe(brandName, "Brand");
        String type = label(creativeType == null ? CreativeType.SQUARE_POST : creativeType);
        String platformLabel = label(platform == null ? CreativePlatform.OTHER : platform);
        if (hasText(productServiceName) && hasText(campaignName)) {
            return "%s - %s - %s - %s - %s".formatted(brand, productServiceName.trim(), campaignName.trim(), type, platformLabel);
        }
        if (hasText(campaignName)) {
            return "%s - %s - %s - %s".formatted(brand, campaignName.trim(), type, platformLabel);
        }
        return "%s - %s - %s - %s".formatted(brand, type, platformLabel, LocalDateTime.now().format(FALLBACK_TIMESTAMP));
    }

    private String label(Enum<?> value) {
        String lower = value.name().toLowerCase().replace('_', ' ');
        String[] parts = lower.split(" ");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return builder.toString();
    }

    private String safe(String value, String fallback) {
        return hasText(value) ? value.trim() : fallback;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
