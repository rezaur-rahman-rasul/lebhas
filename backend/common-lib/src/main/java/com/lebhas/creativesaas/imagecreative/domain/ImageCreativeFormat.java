package com.lebhas.creativesaas.imagecreative.domain;

import com.lebhas.creativesaas.prompt.domain.PromptPlatform;

public enum ImageCreativeFormat {
    FACEBOOK_SQUARE(PromptPlatform.FACEBOOK, 1080, 1080),
    FACEBOOK_BANNER(PromptPlatform.FACEBOOK, 1200, 628),
    INSTAGRAM_POST(PromptPlatform.INSTAGRAM, 1080, 1080),
    INSTAGRAM_STORY(PromptPlatform.INSTAGRAM, 1080, 1920),
    TIKTOK_VERTICAL(PromptPlatform.TIKTOK, 1080, 1920),
    TIKTOK_PRODUCT_AD(PromptPlatform.TIKTOK, 1080, 1080),
    LINKEDIN_POST(PromptPlatform.LINKEDIN, 1200, 627),
    LINKEDIN_BANNER(PromptPlatform.LINKEDIN, 1200, 627),
    CUSTOM(null, 1024, 1024);

    private final PromptPlatform platform;
    private final int width;
    private final int height;

    ImageCreativeFormat(PromptPlatform platform, int width, int height) {
        this.platform = platform;
        this.width = width;
        this.height = height;
    }

    public boolean supports(PromptPlatform requestedPlatform) {
        return platform == null || platform == requestedPlatform;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }
}
