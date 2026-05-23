package com.lebhas.creativesaas.download.application.dto;

public record DownloadRequestContext(
        String downloadSource,
        String ipAddress,
        String userAgent
) {
}
