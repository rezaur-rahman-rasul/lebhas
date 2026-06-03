package com.lebhas.creativesaas.texttool.domain;

public enum CreativeTextToolType {
    POST("TEXT_POST_GENERATOR"),
    CAPTION("TEXT_CAPTION_GENERATOR"),
    ADS_COPY("TEXT_ADS_COPY_GENERATOR"),
    HASHTAGS("TEXT_HASHTAG_GENERATOR");

    private final String toolCode;

    CreativeTextToolType(String toolCode) {
        this.toolCode = toolCode;
    }

    public String toolCode() {
        return toolCode;
    }
}
