package com.lebhas.creativesaas.generation.domain;

public enum CreativeType {
    STATIC_IMAGE,
    CAROUSEL_IMAGE,
    SHORT_VIDEO,
    PRODUCT_PROMO_VIDEO,
    STORY_CREATIVE,
    MOTION_GRAPHIC;

    public boolean isVideo() {
        return this == SHORT_VIDEO || this == PRODUCT_PROMO_VIDEO || this == MOTION_GRAPHIC;
    }

    public boolean isImage() {
        return this == STATIC_IMAGE || this == CAROUSEL_IMAGE || this == STORY_CREATIVE;
    }
}
