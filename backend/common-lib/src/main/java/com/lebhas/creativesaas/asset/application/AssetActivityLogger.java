package com.lebhas.creativesaas.asset.application;

import com.lebhas.creativesaas.asset.domain.AssetCategory;
import com.lebhas.creativesaas.asset.domain.AssetType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class AssetActivityLogger {

    private static final Logger log = LoggerFactory.getLogger(AssetActivityLogger.class);

    public void logUploadStarted(
            UUID workspaceId,
            UUID assetId,
            UUID actorUserId,
            UUID uploadSessionId,
            AssetCategory category,
            AssetType assetType
    ) {
        log.info("asset_event type=upload_started workspaceId={} assetId={} actorUserId={} uploadSessionId={} category={} assetType={}",
                workspaceId, assetId, actorUserId, uploadSessionId, category, assetType);
    }

    public void logUploadCompleted(UUID workspaceId, UUID assetId, UUID actorUserId, String storageKey) {
        log.info("asset_event type=upload_completed workspaceId={} assetId={} actorUserId={} storageKey={}",
                workspaceId, assetId, actorUserId, storageKey);
    }

    public void logAssetUploaded(UUID workspaceId, UUID assetId, UUID actorUserId, AssetCategory category, String storageKey) {
        logUploadCompleted(workspaceId, assetId, actorUserId, storageKey);
    }

    public void logUploadFailed(UUID workspaceId, UUID assetId, UUID actorUserId, String reason) {
        log.warn("asset_event type=upload_failed workspaceId={} assetId={} actorUserId={} reason={}",
                workspaceId, assetId, actorUserId, reason);
    }

    public void logDuplicateUploadAttempt(UUID workspaceId, UUID actorUserId, String hash, UUID existingAssetId) {
        log.info("asset_event type=duplicate_upload_detected workspaceId={} actorUserId={} hash={} existingAssetId={}",
                workspaceId, actorUserId, hash, existingAssetId);
    }

    public void logAssetUpdated(UUID workspaceId, UUID assetId, UUID actorUserId) {
        log.info("asset_event type=asset_updated workspaceId={} assetId={} actorUserId={}",
                workspaceId, assetId, actorUserId);
    }

    public void logAssetDeleted(UUID workspaceId, UUID assetId, UUID actorUserId) {
        log.info("asset_event type=asset_deleted workspaceId={} assetId={} actorUserId={}",
                workspaceId, assetId, actorUserId);
    }

    public void logFolderCreated(UUID workspaceId, UUID folderId, UUID actorUserId, String name) {
        log.info("asset_event type=asset_folder_created workspaceId={} folderId={} actorUserId={} name={}",
                workspaceId, folderId, actorUserId, name);
    }

    public void logFolderUpdated(UUID workspaceId, UUID folderId, UUID actorUserId, String name) {
        log.info("asset_event type=asset_folder_updated workspaceId={} folderId={} actorUserId={} name={}",
                workspaceId, folderId, actorUserId, name);
    }

    public void logFolderDeleted(UUID workspaceId, UUID folderId, UUID actorUserId) {
        log.info("asset_event type=asset_folder_deleted workspaceId={} folderId={} actorUserId={}",
                workspaceId, folderId, actorUserId);
    }

    public void logSignedUrlGenerated(UUID workspaceId, UUID assetId, UUID actorUserId, String mode) {
        log.info("asset_event type=signed_url_generated workspaceId={} assetId={} actorUserId={} mode={}",
                workspaceId, assetId, actorUserId, mode);
    }

    public void logPreviewState(UUID workspaceId, UUID assetId, String status) {
        log.info("asset_event type=preview_state workspaceId={} assetId={} status={}",
                workspaceId, assetId, status);
    }

    public void logDownloadRequested(UUID workspaceId, UUID assetId, UUID actorUserId, String mode) {
        log.info("asset_event type=download_requested workspaceId={} assetId={} actorUserId={} mode={}",
                workspaceId, assetId, actorUserId, mode);
    }

    public void logDownloadCompleted(UUID workspaceId, UUID assetId, UUID actorUserId, String mode) {
        log.info("asset_event type=download_completed workspaceId={} assetId={} actorUserId={} mode={}",
                workspaceId, assetId, actorUserId, mode);
    }

    public void logAccessDenied(UUID workspaceId, UUID actorUserId, UUID assetId, String reason) {
        log.warn("asset_event type=access_denied workspaceId={} actorUserId={} assetId={} reason={}",
                workspaceId, actorUserId, assetId, reason);
    }

    public void logValidationFailure(UUID workspaceId, UUID actorUserId, String reason) {
        log.warn("asset_event type=validation_failure workspaceId={} actorUserId={} reason={}",
                workspaceId, actorUserId, reason);
    }

    public void logRedisFailure(String key, UUID workspaceId, UUID assetId, String reason) {
        log.warn("asset_event type=redis_failure workspaceId={} assetId={} key={} reason={}",
                workspaceId, assetId, key, reason);
    }

    public void logKafkaFailure(String topic, UUID workspaceId, UUID assetId, String reason) {
        log.warn("asset_event type=kafka_failure workspaceId={} assetId={} topic={} reason={}",
                workspaceId, assetId, topic, reason);
    }
}
