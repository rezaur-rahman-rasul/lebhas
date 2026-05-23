package com.lebhas.creativesaas.generation.domain;

public enum CreativeOutputFormat {
    PNG("png", "image/png", true),
    JPG("jpg", "image/jpeg", true),
    WEBP("webp", "image/webp", true),
    MP4("mp4", "video/mp4", false),
    MOV("mov", "video/quicktime", false);

    private final String extension;
    private final String mimeType;
    private final boolean image;

    CreativeOutputFormat(String extension, String mimeType, boolean image) {
        this.extension = extension;
        this.mimeType = mimeType;
        this.image = image;
    }

    public String extension() {
        return extension;
    }

    public String mimeType() {
        return mimeType;
    }

    public boolean isImage() {
        return image;
    }

    public boolean isVideo() {
        return !image;
    }
}
