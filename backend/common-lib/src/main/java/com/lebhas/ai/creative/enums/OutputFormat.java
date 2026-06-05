package com.lebhas.ai.creative.enums;

public enum OutputFormat {
    png("image/png", "png"),
    jpeg("image/jpeg", "jpg"),
    webp("image/webp", "webp");

    private final String contentType;
    private final String extension;

    OutputFormat(String contentType, String extension) {
        this.contentType = contentType;
        this.extension = extension;
    }

    public String contentType() {
        return contentType;
    }

    public String extension() {
        return extension;
    }
}
