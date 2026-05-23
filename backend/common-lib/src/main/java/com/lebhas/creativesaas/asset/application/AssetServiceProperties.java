package com.lebhas.creativesaas.asset.application;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "platform.asset")
public class AssetServiceProperties {

    private static final long MEGABYTE = 1024L * 1024L;

    private long maxImageSizeBytes = 10 * MEGABYTE;
    private long maxVideoSizeBytes = 200 * MEGABYTE;
    private long maxLogoSizeBytes = 5 * MEGABYTE;
    private long maxUploadSizeBytes = 200 * MEGABYTE;
    private long maxUploadCountPerWorkspace = 0;
    private long maxWorkspaceStorageBytes = 0;

    public long getMaxImageSizeBytes() {
        return maxImageSizeBytes;
    }

    public void setMaxImageSizeBytes(long maxImageSizeBytes) {
        this.maxImageSizeBytes = maxImageSizeBytes;
    }

    public long getMaxVideoSizeBytes() {
        return maxVideoSizeBytes;
    }

    public void setMaxVideoSizeBytes(long maxVideoSizeBytes) {
        this.maxVideoSizeBytes = maxVideoSizeBytes;
    }

    public long getMaxLogoSizeBytes() {
        return maxLogoSizeBytes;
    }

    public void setMaxLogoSizeBytes(long maxLogoSizeBytes) {
        this.maxLogoSizeBytes = maxLogoSizeBytes;
    }

    public long getMaxUploadSizeBytes() {
        return maxUploadSizeBytes;
    }

    public void setMaxUploadSizeBytes(long maxUploadSizeBytes) {
        this.maxUploadSizeBytes = maxUploadSizeBytes;
    }

    public long getMaxUploadCountPerWorkspace() {
        return maxUploadCountPerWorkspace;
    }

    public void setMaxUploadCountPerWorkspace(long maxUploadCountPerWorkspace) {
        this.maxUploadCountPerWorkspace = maxUploadCountPerWorkspace;
    }

    public long getMaxWorkspaceStorageBytes() {
        return maxWorkspaceStorageBytes;
    }

    public void setMaxWorkspaceStorageBytes(long maxWorkspaceStorageBytes) {
        this.maxWorkspaceStorageBytes = maxWorkspaceStorageBytes;
    }

    public boolean isUploadCountLimited() {
        return maxUploadCountPerWorkspace > 0;
    }

    public boolean isWorkspaceStorageLimited() {
        return maxWorkspaceStorageBytes > 0;
    }
}
