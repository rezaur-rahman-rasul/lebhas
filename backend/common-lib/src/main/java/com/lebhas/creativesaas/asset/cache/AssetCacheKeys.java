package com.lebhas.creativesaas.asset.cache;

import java.util.UUID;

public final class AssetCacheKeys {

    public static final String ASSET = "asset:%s";
    public static final String ASSET_LIST_PROJECT_PAGE = "asset:list:project:%s:page:%d";
    public static final String ASSET_LIST_PROJECT_PATTERN = "asset:list:project:%s:page:*";
    public static final String SIGNED_URL = "signed-url:%s:%s";
    public static final String WORKSPACE_STORAGE = "workspace:storage:%s";
    public static final String UPLOAD_SESSION = "upload:session:%s";
    public static final String UPLOAD_STATE = "upload:%s";
    public static final String UPLOAD_PROGRESS = "upload:progress:%s";
    public static final String ASSET_HOT = "asset:hot:%s";
    public static final String ASSET_PROCESSING = "asset:processing:%s";
    public static final String ASSET_RATE_LIMIT = "asset:rate:%s:%s:%s";
    public static final String ASSET_ASYNC_JOB = "asset:job:%s";
    public static final String ASSET_ASYNC_COORDINATION = "asset:job:asset:%s:%s";

    private AssetCacheKeys() {
    }

    public static String asset(UUID assetId) {
        return ASSET.formatted(required(assetId, "assetId"));
    }

    public static String assetListProject(UUID projectId, int page) {
        return ASSET_LIST_PROJECT_PAGE.formatted(required(projectId, "projectId"), Math.max(page, 0));
    }

    public static String assetListProjectPattern(UUID projectId) {
        return ASSET_LIST_PROJECT_PATTERN.formatted(required(projectId, "projectId"));
    }

    public static String signedUrl(UUID assetId, String type) {
        return SIGNED_URL.formatted(required(assetId, "assetId"), normalize(type, "type"));
    }

    public static String workspaceStorage(UUID workspaceId) {
        return WORKSPACE_STORAGE.formatted(required(workspaceId, "workspaceId"));
    }

    public static String uploadSession(UUID uploadId) {
        return uploadSession(required(uploadId, "uploadId").toString());
    }

    public static String uploadSession(String uploadId) {
        return UPLOAD_SESSION.formatted(normalize(uploadId, "uploadId"));
    }

    public static String uploadState(String uploadId) {
        return UPLOAD_STATE.formatted(normalize(uploadId, "uploadId"));
    }

    public static String uploadProgress(String uploadId) {
        return UPLOAD_PROGRESS.formatted(normalize(uploadId, "uploadId"));
    }

    public static String assetHot(UUID assetId) {
        return ASSET_HOT.formatted(required(assetId, "assetId"));
    }

    public static String assetProcessing(UUID assetId) {
        return ASSET_PROCESSING.formatted(required(assetId, "assetId"));
    }

    public static String rateLimit(String scope, String subject, String action) {
        return ASSET_RATE_LIMIT.formatted(
                normalize(scope, "scope"),
                normalize(subject, "subject"),
                normalize(action, "action"));
    }

    public static String asyncJob(String jobId) {
        return ASSET_ASYNC_JOB.formatted(normalize(jobId, "jobId"));
    }

    public static String asyncCoordination(UUID assetId, String jobType) {
        return ASSET_ASYNC_COORDINATION.formatted(required(assetId, "assetId"), normalize(jobType, "jobType"));
    }

    private static UUID required(UUID value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " must not be null");
        }
        return value;
    }

    private static String normalize(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim()
                .replaceAll("[\\s\\r\\n]+", "-");
    }
}
