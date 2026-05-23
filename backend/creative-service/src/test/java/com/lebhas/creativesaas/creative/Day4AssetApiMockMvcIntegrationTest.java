package com.lebhas.creativesaas.creative;

import com.lebhas.creativesaas.asset.cache.AssetCacheKeys;
import com.lebhas.creativesaas.asset.cache.dto.AssetHotCacheEntry;
import com.lebhas.creativesaas.asset.domain.AssetEntity;
import com.lebhas.creativesaas.asset.storage.LocalAssetAccessMode;
import com.lebhas.creativesaas.common.constants.CommonHeaders;
import com.lebhas.creativesaas.common.security.Role;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class Day4AssetApiMockMvcIntegrationTest extends AbstractDay4BackendIntegrationTest {

    @Test
    void assetUploadWorks() throws Exception {
        String accessToken = tokenFor(adminUser, workspaceOne.getId(), Role.ADMIN);

        var upload = uploadAsset(accessToken, "product.png", pngBytes(), "image/png", "PRODUCT_IMAGE");

        assertThat(upload.getResponse().getStatus()).isEqualTo(200);
        assertThat(textAt(upload, "/data/status")).isEqualTo("READY");
        assertThat(textAt(upload, "/data/workspaceId")).isEqualTo(workspaceOne.getId().toString());
        assertThat(textAt(upload, "/data/projectId")).isEqualTo(projectCampaignOne.getId().toString());
    }

    @Test
    void invalidMimeRejected() throws Exception {
        String accessToken = tokenFor(adminUser, workspaceOne.getId(), Role.ADMIN);

        var upload = uploadAsset(
                accessToken,
                "notes.txt",
                "not-an-asset".getBytes(),
                "text/plain",
                "OTHER");

        assertThat(upload.getResponse().getStatus()).isEqualTo(400);
        assertThat(textAt(upload, "/errors/0/code")).isEqualTo("ASSET-400-02");
    }

    @Test
    void oversizedUploadRejected() throws Exception {
        String accessToken = tokenFor(adminUser, workspaceOne.getId(), Role.ADMIN);

        var upload = uploadAsset(
                accessToken,
                "too-large.png",
                oversizedPngBytes(),
                "image/png",
                "PRODUCT_IMAGE");

        assertThat(upload.getResponse().getStatus()).isEqualTo(400);
        assertThat(textAt(upload, "/errors/0/code")).isEqualTo("ASSET-400-03");
    }

    @Test
    void assetRelationshipMappingWorks() throws Exception {
        AssetEntity asset = uploadReadyAsset();

        assertThat(asset.getWorkspaceId()).isEqualTo(workspaceOne.getId());
        assertThat(asset.getBrandId()).isEqualTo(brandOne.getId());
        assertThat(asset.getProductServiceId()).isEqualTo(productServiceOne.getId());
        assertThat(asset.getProjectId()).isEqualTo(projectCampaignOne.getId());
        assertThat(asset.getUploadedBy()).isEqualTo(adminUser.getId());
        assertThat(asset.getStorageFileId()).isNotNull();
    }

    @Test
    void assetRetrievalWorks() throws Exception {
        String accessToken = tokenFor(adminUser, workspaceOne.getId(), Role.ADMIN);
        AssetEntity asset = uploadReadyAsset();

        mockMvc.perform(get("/api/v1/assets/{assetId}", asset.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(asset.getId().toString()))
                .andExpect(jsonPath("$.data.projectId").value(projectCampaignOne.getId().toString()));

        mockMvc.perform(get("/api/v1/assets/project/{projectId}", projectCampaignOne.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].id").value(asset.getId().toString()));
    }

    @Test
    void assetMetadataCachingWorks() throws Exception {
        String accessToken = tokenFor(adminUser, workspaceOne.getId(), Role.ADMIN);
        AssetEntity asset = uploadReadyAsset();

        mockMvc.perform(get("/api/v1/assets/{assetId}", asset.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/assets/project/{projectId}", projectCampaignOne.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isOk());

        assertAssetCachePresent(asset.getId());
        assertProjectListCachePresent();
    }

    @Test
    void signedUrlCachingWorks() throws Exception {
        String accessToken = tokenFor(adminUser, workspaceOne.getId(), Role.ADMIN);
        AssetEntity asset = uploadReadyAsset();

        mockMvc.perform(get("/api/v1/downloads/{assetId}", asset.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.type").value("download"))
                .andExpect(jsonPath("$.data.cached").value(false));

        mockMvc.perform(get("/api/v1/downloads/{assetId}", asset.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.type").value("download"))
                .andExpect(jsonPath("$.data.cached").value(true));

        assertSignedUrlCachePresent(asset.getId(), "download");
    }

    @Test
    void cacheInvalidationWorks() throws Exception {
        String accessToken = tokenFor(adminUser, workspaceOne.getId(), Role.ADMIN);
        AssetEntity asset = uploadReadyAsset();

        mockMvc.perform(get("/api/v1/assets/{assetId}", asset.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/assets/project/{projectId}", projectCampaignOne.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/downloads/{assetId}", asset.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/v1/assets/{assetId}", asset.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        assertThat(redisTemplate.hasKey(AssetCacheKeys.asset(asset.getId()))).isEqualTo(Boolean.FALSE);
        assertThat(redisTemplate.hasKey(AssetCacheKeys.assetListProject(projectCampaignOne.getId(), 0)))
                .isEqualTo(Boolean.FALSE);
        assertThat(redisTemplate.hasKey(AssetCacheKeys.signedUrl(asset.getId(), "download")))
                .isEqualTo(Boolean.FALSE);
    }

    @Test
    void assetUploadedEventPublished() throws Exception {
        String accessToken = tokenFor(adminUser, workspaceOne.getId(), Role.ADMIN);

        try (Consumer<String, String> consumer = createConsumer(assetKafkaTopicNames.assetUploaded())) {
            var upload = uploadAsset(accessToken, "event.png", pngBytes(), "image/png", "REFERENCE_IMAGE");
            UUID assetId = uuidAt(upload, "/data/id");

            List<ConsumerRecord<String, String>> records = pollRecords(consumer, 1, Duration.ofSeconds(10));

            assertThat(records).hasSizeGreaterThanOrEqualTo(1);
            assertThat(records.getFirst().topic()).isEqualTo(assetKafkaTopicNames.assetUploaded());
            assertThat(records.getFirst().value()).contains(assetId.toString(), workspaceOne.getId().toString());
        }
    }

    @Test
    void assetProcessConsumerWorks() throws Exception {
        String accessToken = tokenFor(adminUser, workspaceOne.getId(), Role.ADMIN);

        try (Consumer<String, String> consumer = createConsumer(assetKafkaTopicNames.assetVariantGenerate())) {
            var upload = uploadAsset(accessToken, "consumer.png", pngBytes(), "image/png", "REFERENCE_IMAGE");
            UUID assetId = uuidAt(upload, "/data/id");

            List<ConsumerRecord<String, String>> records = pollRecords(consumer, 1, Duration.ofSeconds(10));

            assertThat(records).hasSizeGreaterThanOrEqualTo(1);
            assertThat(records.getFirst().topic()).isEqualTo(assetKafkaTopicNames.assetVariantGenerate());
            assertThat(records.getFirst().value()).contains(assetId.toString(), "THUMBNAIL");
        }
    }

    @Test
    void assetDeletedEventPublished() throws Exception {
        String accessToken = tokenFor(adminUser, workspaceOne.getId(), Role.ADMIN);
        AssetEntity asset = uploadReadyAsset();

        try (Consumer<String, String> consumer = createConsumer(assetKafkaTopicNames.assetDeleted())) {
            mockMvc.perform(delete("/api/v1/assets/{assetId}", asset.getId())
                            .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                    .andExpect(status().isOk());

            List<ConsumerRecord<String, String>> records = pollRecords(consumer, 1, Duration.ofSeconds(10));

            assertThat(records).hasSizeGreaterThanOrEqualTo(1);
            assertThat(records.getFirst().topic()).isEqualTo(assetKafkaTopicNames.assetDeleted());
            assertThat(records.getFirst().value()).contains(asset.getId().toString(), workspaceOne.getId().toString());
        }
    }

    @Test
    void workspaceIsolationEnforced() throws Exception {
        AssetEntity asset = uploadReadyAsset();
        String otherWorkspaceToken = tokenFor(workspaceTwoAdmin, workspaceTwo.getId(), Role.ADMIN);

        mockMvc.perform(get("/api/v1/assets/{assetId}", asset.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(otherWorkspaceToken))
                        .header(CommonHeaders.WORKSPACE_ID, workspaceOne.getId().toString()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errors[0].code").value("TENANT-403"));
    }

    @Test
    void unauthorizedAssetAccessBlocked() throws Exception {
        AssetEntity asset = uploadReadyAsset();

        mockMvc.perform(get("/api/v1/assets/{assetId}", asset.getId()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errors[0].code").value("COMMON-401"));
    }

    @Test
    void signedUrlsExpireCorrectly() throws Exception {
        AssetEntity asset = uploadReadyAsset();
        long expiredAt = Instant.now().minusSeconds(5).getEpochSecond();
        String signature = localSignature(asset.getId(), LocalAssetAccessMode.DOWNLOAD, expiredAt);

        mockMvc.perform(get("/internal/storage/local/assets/{assetId}/download", asset.getId())
                        .param("expiresAt", Long.toString(expiredAt))
                        .param("signature", signature))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errors[0].code").value("ASSET-403-02"));
    }

    @Test
    void downloadHistoryTracked() throws Exception {
        String accessToken = tokenFor(adminUser, workspaceOne.getId(), Role.ADMIN);
        AssetEntity asset = uploadReadyAsset();

        mockMvc.perform(get("/api/v1/downloads/{assetId}", asset.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .header(HttpHeaders.USER_AGENT, "Day4 Download Client"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.assetId").value(asset.getId().toString()));

        List<com.lebhas.creativesaas.download.domain.DownloadLogEntity> logs = downloadLogsFor(asset.getId());
        assertThat(logs).hasSize(1);
        assertThat(logs.getFirst().getAssetId()).isEqualTo(asset.getId());
        assertThat(logs.getFirst().getDownloadedBy()).isEqualTo(adminUser.getId());
        assertThat(logs.getFirst().getDownloadType()).isEqualTo("download");
        assertThat(logs.getFirst().getUserAgent()).isEqualTo("Day4 Download Client");
    }

    @Test
    void accessCountUpdated() throws Exception {
        String accessToken = tokenFor(adminUser, workspaceOne.getId(), Role.ADMIN);
        AssetEntity asset = uploadReadyAsset();

        mockMvc.perform(get("/api/v1/downloads/{assetId}", asset.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/downloads/{assetId}", asset.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isOk());

        AssetHotCacheEntry hotCacheEntry = assetHotRedisCacheService.get(workspaceOne.getId(), asset.getId()).orElseThrow();
        assertThat(hotCacheEntry.downloadCount()).isEqualTo(2L);
        assertThat(hotCacheEntry.lastDownloadType()).isEqualTo("download");
    }

    @Test
    void downloadPermissionValidationWorks() throws Exception {
        String crewToken = tokenFor(crewUser, workspaceOne.getId(), Role.CREW);
        AssetEntity asset = uploadReadyAsset();

        mockMvc.perform(get("/api/v1/downloads/{assetId}", asset.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(crewToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errors[0].code").value("COMMON-403"));
    }
}
