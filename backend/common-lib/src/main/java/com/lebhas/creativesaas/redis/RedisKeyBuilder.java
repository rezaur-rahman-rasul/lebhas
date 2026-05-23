package com.lebhas.creativesaas.redis;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class RedisKeyBuilder {

    public String authRefresh(String tokenId) {
        return "auth:refresh:" + tokenId;
    }

    public String authBlacklist(String jwtId) {
        return "auth:blacklist:" + jwtId;
    }

    public String authSession(UUID userId, String deviceId) {
        return "auth:session:" + userId + ":" + normalizeSegment(deviceId);
    }

    public String refreshFamily(UUID userId) {
        return "refresh:family:" + userId;
    }

    public String loginAttempt(String email) {
        return "login:attempt:" + normalizeSegment(email == null ? "unknown" : email.trim().toLowerCase(java.util.Locale.ROOT));
    }

    public String workspaceContext(UUID workspaceId, UUID userId) {
        return "workspace:context:" + workspaceId + ":" + userId;
    }

    public String workspaceActive(UUID workspaceId) {
        return "workspace:active:" + workspaceId;
    }

    public String permissions(UUID workspaceId, UUID userId) {
        return "permissions:" + workspaceId + ":" + userId;
    }

    public String permissionVersion(UUID workspaceId) {
        return "permission:version:" + workspaceId;
    }

    public String supportSession(UUID masterUserId) {
        return "support:session:" + masterUserId;
    }

    public String workspaceBrands(UUID workspaceId) {
        return "workspace:" + workspaceId + ":brands";
    }

    public String brand(UUID brandId) {
        return "brand:" + brandId;
    }

    public String workspaceProductServices(UUID workspaceId) {
        return "workspace:" + workspaceId + ":product-services";
    }

    public String productService(UUID productServiceId) {
        return "product-service:" + productServiceId;
    }

    public String workspaceProjects(UUID workspaceId) {
        return "workspace:" + workspaceId + ":projects";
    }

    public String project(UUID projectId) {
        return "project:" + projectId;
    }

    public String dashboard(UUID workspaceId) {
        return "dashboard:" + workspaceId;
    }

    public String wallet(UUID workspaceId) {
        return "wallet:" + workspaceId;
    }

    public String promptHash(String sha256) {
        return "prompt:hash:" + sha256;
    }

    public String generationDeduplication(String requestHash) {
        return "generation:lock:" + requestHash;
    }

    public String lockWallet(UUID workspaceId) {
        return "lock:wallet:" + workspaceId;
    }

    public String lockGeneration(UUID requestId) {
        return "lock:generation:" + requestId;
    }

    public String lockCreativeRequest(UUID creativeRequestId) {
        return "lock:creative-request:" + creativeRequestId;
    }

    public String lockAuth(UUID userId) {
        return "lock:auth:" + userId;
    }

    public String lockWorkspace(UUID workspaceId) {
        return "lock:workspace:" + workspaceId;
    }

    public String lockProject(UUID projectId) {
        return "lock:project:" + projectId;
    }

    public String lockSupport(UUID masterUserId) {
        return "lock:support:" + masterUserId;
    }

    public String rateUser(UUID userId) {
        return "rate:user:" + userId;
    }

    public String rateWorkspace(UUID workspaceId) {
        return "rate:workspace:" + workspaceId;
    }

    public String rateIp(String ipAddress) {
        return "rate:ip:" + ipAddress;
    }

    public String signedUrl(UUID storageFileId) {
        return "signed:url:" + storageFileId;
    }

    public String asset(UUID assetId) {
        return "asset:" + assetId;
    }

    public String assetsList(UUID workspaceId, UUID projectId, int page) {
        return "assets:list:" + workspaceId + ":" + projectId + ":" + page;
    }

    public String assetsListPattern(UUID workspaceId, UUID projectId) {
        return "assets:list:" + workspaceId + ":" + projectId + ":";
    }

    public String upload(String uploadId) {
        return "upload:" + uploadId;
    }

    public String uploadChunk(String uploadId) {
        return "upload:chunk:" + uploadId;
    }

    public String uploadProgress(String uploadId) {
        return "upload:progress:" + uploadId;
    }

    public String uploadQuotaLock(UUID workspaceId) {
        return "upload:quota-lock:" + workspaceId;
    }

    public String uploadDedupe(String sha256) {
        return "upload:dedupe:" + normalizeSegment(sha256 == null ? "unknown" : sha256.trim().toLowerCase(java.util.Locale.ROOT));
    }

    public String previewJob(UUID assetId) {
        return "preview:job:" + assetId;
    }

    public String lockUpload(String sha256) {
        return "lock:upload:" + normalizeSegment(sha256 == null ? "unknown" : sha256.trim().toLowerCase(java.util.Locale.ROOT));
    }

    public String lockAsset(UUID assetId) {
        return "lock:asset:" + assetId;
    }

    public String lockStorage(String objectKey) {
        return "lock:storage:" + normalizeSegment(objectKey);
    }

    public String jobStatus(String jobId) {
        return "job:" + jobId + ":status";
    }

    private String normalizeSegment(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        return value.trim();
    }
}
