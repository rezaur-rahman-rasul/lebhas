package com.lebhas.creativesaas.creative;

import com.lebhas.creativesaas.asset.application.AssetUploadStateService;
import com.lebhas.creativesaas.asset.application.PreviewStateService;
import com.lebhas.creativesaas.asset.application.UploadChunkTracker;
import com.lebhas.creativesaas.asset.domain.AssetEntity;
import com.lebhas.creativesaas.asset.domain.AssetType;
import com.lebhas.creativesaas.asset.domain.PreviewStatus;
import com.lebhas.creativesaas.asset.domain.ProcessingStatus;
import com.lebhas.creativesaas.asset.domain.StorageProvider;
import com.lebhas.creativesaas.asset.domain.UploadSessionEntity;
import com.lebhas.creativesaas.asset.infrastructure.persistence.AssetRepository;
import com.lebhas.creativesaas.asset.infrastructure.persistence.UploadSessionRepository;
import com.lebhas.creativesaas.brand.domain.BrandEntity;
import com.lebhas.creativesaas.brand.domain.BrandLanguagePreference;
import com.lebhas.creativesaas.brand.infrastructure.persistence.BrandRepository;
import com.lebhas.creativesaas.campaign.domain.ProjectCampaignEntity;
import com.lebhas.creativesaas.campaign.infrastructure.persistence.ProjectCampaignRepository;
import com.lebhas.creativesaas.common.security.Role;
import com.lebhas.creativesaas.common.security.jwt.JwtAccessTokenService;
import com.lebhas.creativesaas.common.tenant.TenantContext;
import com.lebhas.creativesaas.download.domain.DownloadLogEntity;
import com.lebhas.creativesaas.download.infrastructure.persistence.DownloadLogRepository;
import com.lebhas.creativesaas.identity.domain.UserEntity;
import com.lebhas.creativesaas.identity.domain.UserStatus;
import com.lebhas.creativesaas.identity.domain.WorkspaceMembershipEntity;
import com.lebhas.creativesaas.identity.domain.WorkspaceMembershipStatus;
import com.lebhas.creativesaas.identity.infrastructure.persistence.UserRepository;
import com.lebhas.creativesaas.identity.infrastructure.persistence.WorkspaceMembershipRepository;
import com.lebhas.creativesaas.messaging.kafka.KafkaTopicConstants;
import com.lebhas.creativesaas.product.domain.ProductServiceEntity;
import com.lebhas.creativesaas.product.infrastructure.persistence.ProductServiceRepository;
import com.lebhas.creativesaas.redis.RedisKeyBuilder;
import com.lebhas.creativesaas.redis.RedisLockService;
import com.lebhas.creativesaas.redis.RedisSignedUrlCache;
import com.lebhas.creativesaas.storage.domain.StorageFileEntity;
import com.lebhas.creativesaas.storage.infrastructure.persistence.StorageFileRepository;
import com.lebhas.creativesaas.workspace.domain.WorkspaceEntity;
import com.lebhas.creativesaas.workspace.domain.WorkspaceLanguage;
import com.lebhas.creativesaas.workspace.infrastructure.persistence.WorkspaceRepository;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CreativeAssetApiIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @Container
    static final KafkaContainer KAFKA = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.1"));

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.flyway.url", POSTGRES::getJdbcUrl);
        registry.add("spring.flyway.user", POSTGRES::getUsername);
        registry.add("spring.flyway.password", POSTGRES::getPassword);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", REDIS::getFirstMappedPort);
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
    }

    @LocalServerPort
    int port;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private WorkspaceMembershipRepository workspaceMembershipRepository;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private ProductServiceRepository productServiceRepository;

    @Autowired
    private ProjectCampaignRepository projectCampaignRepository;

    @Autowired
    private AssetRepository assetRepository;

    @Autowired
    private StorageFileRepository storageFileRepository;

    @Autowired
    private UploadSessionRepository uploadSessionRepository;

    @Autowired
    private DownloadLogRepository downloadLogRepository;

    @Autowired
    private JwtAccessTokenService jwtAccessTokenService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RedisKeyBuilder redisKeyBuilder;

    @Autowired
    private AssetUploadStateService assetUploadStateService;

    @Autowired
    private UploadChunkTracker uploadChunkTracker;

    @Autowired
    private PreviewStateService previewStateService;

    @Autowired
    private RedisSignedUrlCache redisSignedUrlCache;

    @Autowired
    private RedisLockService redisLockService;

    private UserEntity adminUser;
    private UserEntity crewUser;
    private UserEntity masterUser;
    private UserEntity workspaceTwoAdmin;
    private WorkspaceEntity workspaceOne;
    private WorkspaceEntity workspaceTwo;
    private BrandEntity brandOne;
    private ProductServiceEntity productServiceOne;
    private ProjectCampaignEntity projectCampaignOne;

    @BeforeEach
    void setUp() {
        downloadLogRepository.deleteAll();
        assetRepository.deleteAll();
        storageFileRepository.deleteAll();
        uploadSessionRepository.deleteAll();
        projectCampaignRepository.deleteAll();
        productServiceRepository.deleteAll();
        brandRepository.deleteAll();
        workspaceMembershipRepository.deleteAll();
        workspaceRepository.deleteAll();
        userRepository.deleteAll();
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
        TenantContext.clear();

        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;

        adminUser = userRepository.save(UserEntity.register(
                "Creative",
                "Admin",
                "asset-admin@example.com",
                null,
                "{noop}unused",
                Role.ADMIN,
                UserStatus.ACTIVE,
                true));
        crewUser = userRepository.save(UserEntity.register(
                "Creative",
                "Crew",
                "asset-crew@example.com",
                null,
                "{noop}unused",
                Role.CREW,
                UserStatus.ACTIVE,
                true));
        masterUser = userRepository.save(UserEntity.register(
                "Master",
                "Support",
                "asset-master@example.com",
                null,
                "{noop}unused",
                Role.MASTER,
                UserStatus.ACTIVE,
                true));
        workspaceTwoAdmin = userRepository.save(UserEntity.register(
                "Other",
                "Admin",
                "asset-other@example.com",
                null,
                "{noop}unused",
                Role.ADMIN,
                UserStatus.ACTIVE,
                true));

        workspaceOne = workspaceRepository.save(WorkspaceEntity.create(
                "Creative Workspace One",
                "asset-workspace-one",
                null,
                null,
                "Media",
                "Asia/Dhaka",
                WorkspaceLanguage.ENGLISH,
                "BDT",
                "BD",
                adminUser.getId()));
        workspaceTwo = workspaceRepository.save(WorkspaceEntity.create(
                "Creative Workspace Two",
                "asset-workspace-two",
                null,
                null,
                "Agency",
                "Asia/Dhaka",
                WorkspaceLanguage.ENGLISH,
                "BDT",
                "BD",
                workspaceTwoAdmin.getId()));

        workspaceMembershipRepository.save(WorkspaceMembershipEntity.create(
                workspaceOne.getId(),
                adminUser.getId(),
                Role.ADMIN,
                WorkspaceMembershipStatus.ACTIVE,
                Set.of(),
                Instant.now(),
                adminUser.getId()));
        workspaceMembershipRepository.save(WorkspaceMembershipEntity.create(
                workspaceOne.getId(),
                crewUser.getId(),
                Role.CREW,
                WorkspaceMembershipStatus.ACTIVE,
                Set.of(),
                false,
                false,
                Instant.now(),
                adminUser.getId()));
        workspaceMembershipRepository.save(WorkspaceMembershipEntity.create(
                workspaceTwo.getId(),
                workspaceTwoAdmin.getId(),
                Role.ADMIN,
                WorkspaceMembershipStatus.ACTIVE,
                Set.of(),
                Instant.now(),
                workspaceTwoAdmin.getId()));

        brandOne = brandRepository.save(BrandEntity.create(
                workspaceOne.getId(),
                adminUser.getId(),
                "Brand One",
                "Agency",
                "Creative",
                "Growth teams",
                "Bold",
                "Start now",
                "#123456",
                "#654321",
                "https://brand-one.example.com",
                null,
                null,
                null,
                null,
                BrandLanguagePreference.BOTH));
        BrandEntity brandTwo = brandRepository.save(BrandEntity.create(
                workspaceTwo.getId(),
                workspaceTwoAdmin.getId(),
                "Brand Two",
                "Service",
                "Consulting",
                "Finance teams",
                "Calm",
                "Learn more",
                "#111111",
                "#222222",
                "https://brand-two.example.com",
                null,
                null,
                null,
                null,
                BrandLanguagePreference.BOTH));

        productServiceOne = productServiceRepository.save(ProductServiceEntity.create(
                workspaceOne.getId(),
                brandOne.getId(),
                "Creative Retainer",
                "Monthly delivery",
                "SERVICE",
                "Growth teams",
                "Fast delivery"));
        ProductServiceEntity productServiceTwo = productServiceRepository.save(ProductServiceEntity.create(
                workspaceTwo.getId(),
                brandTwo.getId(),
                "Consulting Package",
                "Campaign consulting",
                "SERVICE",
                "Finance teams",
                "Operational clarity"));

        projectCampaignOne = projectCampaignRepository.save(ProjectCampaignEntity.create(
                workspaceOne.getId(),
                brandOne.getId(),
                productServiceOne.getId(),
                adminUser.getId(),
                "Spring Launch",
                "Launch campaign foundation",
                "AWARENESS",
                "FACEBOOK",
                "CAMPAIGN"));
        projectCampaignRepository.save(ProjectCampaignEntity.create(
                workspaceTwo.getId(),
                brandTwo.getId(),
                productServiceTwo.getId(),
                workspaceTwoAdmin.getId(),
                "Summer Launch",
                "Cross-workspace boundary",
                "LEADS",
                "INSTAGRAM",
                "CAMPAIGN"));
    }

    @Test
    void shouldRejectOversizedPaginationRequest() {
        String accessToken = tokenFor(adminUser, workspaceOne.getId(), Role.ADMIN);

        given()
                .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                .when()
                .get("/api/v1/workspaces/{workspaceId}/projects/{projectId}/assets?size=101",
                        workspaceOne.getId(), projectCampaignOne.getId())
                .then()
                .statusCode(400)
                .body("success", equalTo(false))
                .body("errors[0].code", equalTo("COMMON-400"));
    }

    @Test
    void shouldUploadAssetPersistRedisStateAndPublishKafkaLifecycleEvents() {
        String accessToken = tokenFor(adminUser, workspaceOne.getId(), Role.ADMIN);
        try (Consumer<String, String> consumer = createConsumer(
                KafkaTopicConstants.ASSET_UPLOAD_STARTED,
                KafkaTopicConstants.ASSET_UPLOAD_COMPLETED)) {
            Response response = uploadAsset(
                    accessToken,
                    workspaceOne.getId(),
                    projectCampaignOne.getId(),
                    "product.png",
                    pngBytes(),
                    "image/png",
                    "PRODUCT_IMAGE");

            response.then()
                    .statusCode(200)
                    .body("success", equalTo(true))
                    .body("data.workspaceId", equalTo(workspaceOne.getId().toString()))
                    .body("data.brandId", equalTo(brandOne.getId().toString()))
                    .body("data.productServiceId", equalTo(productServiceOne.getId().toString()))
                    .body("data.projectCampaignId", equalTo(projectCampaignOne.getId().toString()))
                    .body("data.assetType", equalTo(AssetType.RAW.name()))
                    .body("data.storageFileId", notNullValue())
                    .body("data.uploadSessionId", notNullValue())
                    .body("data.previewStatus", equalTo(PreviewStatus.READY.name()))
                    .body("data.processingStatus", equalTo(ProcessingStatus.READY.name()))
                    .body("data.status", equalTo("READY"));

            UUID assetId = UUID.fromString(response.jsonPath().getString("data.id"));
            UUID uploadSessionId = UUID.fromString(response.jsonPath().getString("data.uploadSessionId"));
            UUID storageFileId = UUID.fromString(response.jsonPath().getString("data.storageFileId"));

            AssetEntity asset = assetRepository.findById(assetId).orElseThrow();
            UploadSessionEntity uploadSession = uploadSessionRepository.findById(uploadSessionId).orElseThrow();
            StorageFileEntity storageFile = storageFileRepository.findById(storageFileId).orElseThrow();

            assertThat(asset.getWorkspaceId()).isEqualTo(workspaceOne.getId());
            assertThat(asset.getBrandId()).isEqualTo(brandOne.getId());
            assertThat(asset.getProductServiceId()).isEqualTo(productServiceOne.getId());
            assertThat(asset.getProjectCampaignId()).isEqualTo(projectCampaignOne.getId());
            assertThat(asset.getStorageFileId()).isEqualTo(storageFile.getId());
            assertThat(uploadSession.getAssetId()).isEqualTo(assetId);
            assertThat(storageFile.getProvider()).isEqualTo(StorageProvider.LOCAL);
            assertThat(storageFile.getObjectKey())
                    .isEqualTo("raw/workspaces/%s/projects/%s/%s".formatted(
                            workspaceOne.getId(),
                            projectCampaignOne.getId(),
                            assetId));

            assertThat(assetUploadStateService.get(uploadSessionId.toString()))
                    .isPresent()
                    .get()
                    .extracting(
                            AssetUploadStateService.UploadStateSnapshot::workspaceId,
                            AssetUploadStateService.UploadStateSnapshot::projectId,
                            AssetUploadStateService.UploadStateSnapshot::assetId,
                            AssetUploadStateService.UploadStateSnapshot::uploadStatus)
                    .containsExactly(workspaceOne.getId(), projectCampaignOne.getId(), assetId, "COMPLETED");
            assertThat(uploadChunkTracker.get(uploadSessionId.toString()))
                    .isPresent()
                    .get()
                    .extracting(
                            UploadChunkTracker.UploadChunkState::chunkCount,
                            UploadChunkTracker.UploadChunkState::completionPercentage,
                            UploadChunkTracker.UploadChunkState::uploadStatus)
                    .containsExactly(1, 100, "COMPLETED");
            assertThat(previewStateService.get(assetId))
                    .isPresent()
                    .get()
                    .extracting(
                            PreviewStateService.PreviewJobState::previewStatus,
                            PreviewStateService.PreviewJobState::thumbnailReady)
                    .containsExactly("READY", true);
            assertThat(assetUploadStateService.findDuplicate(storageFile.getHash())).contains(assetId);

            List<ConsumerRecord<String, String>> records = pollRecords(consumer, 2, Duration.ofSeconds(10));
            assertThat(records).extracting(ConsumerRecord::topic)
                    .contains(KafkaTopicConstants.ASSET_UPLOAD_STARTED, KafkaTopicConstants.ASSET_UPLOAD_COMPLETED);
            assertThat(records).anySatisfy(record -> {
                assertThat(record.value()).contains(workspaceOne.getId().toString());
                assertThat(record.value()).contains(assetId.toString());
            });
        }
    }

    @Test
    void shouldAcceptJpgPngAndMp4Uploads() {
        String accessToken = tokenFor(adminUser, workspaceOne.getId(), Role.ADMIN);

        uploadAsset(accessToken, workspaceOne.getId(), projectCampaignOne.getId(), "photo.jpg", jpgBytes(), "image/jpeg", "PRODUCT_IMAGE")
                .then()
                .statusCode(200)
                .body("data.status", equalTo("READY"));
        uploadAsset(accessToken, workspaceOne.getId(), projectCampaignOne.getId(), "photo.png", pngBytes(), "image/png", "REFERENCE_IMAGE")
                .then()
                .statusCode(200)
                .body("data.status", equalTo("READY"));
        uploadAsset(accessToken, workspaceOne.getId(), projectCampaignOne.getId(), "clip.mp4", mp4Bytes(), "video/mp4", "PRODUCT_VIDEO")
                .then()
                .statusCode(200)
                .body("data.status", equalTo("READY"));
    }

    @Test
    void shouldRejectMaliciousSvgUpload() {
        String accessToken = tokenFor(adminUser, workspaceOne.getId(), Role.ADMIN);

        given()
                .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                .contentType(ContentType.MULTIPART)
                .multiPart(
                        "file",
                        "brand-logo.svg",
                        """
                        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 10 10">
                          <script>alert('xss')</script>
                        </svg>
                        """.getBytes(StandardCharsets.UTF_8),
                        "image/svg+xml")
                .multiPart("assetCategory", "BRAND_LOGO")
                .when()
                .post("/api/v1/workspaces/{workspaceId}/projects/{projectId}/assets/upload",
                        workspaceOne.getId(), projectCampaignOne.getId())
                .then()
                .statusCode(400)
                .body("success", equalTo(false))
                .body("errors[0].code", equalTo("ASSET-400-05"));
    }

    @Test
    void shouldRejectUnsupportedAndOversizedUploads() {
        String accessToken = tokenFor(adminUser, workspaceOne.getId(), Role.ADMIN);

        uploadAsset(accessToken, workspaceOne.getId(), projectCampaignOne.getId(), "notes.txt",
                "not-an-asset".getBytes(StandardCharsets.UTF_8), "text/plain", "OTHER")
                .then()
                .statusCode(400)
                .body("errors[0].code", equalTo("ASSET-400-02"));

        byte[] oversizedPng = new byte[(10 * 1024 * 1024) + 1];
        oversizedPng[0] = (byte) 0x89;
        oversizedPng[1] = 0x50;
        oversizedPng[2] = 0x4E;
        oversizedPng[3] = 0x47;

        uploadAsset(accessToken, workspaceOne.getId(), projectCampaignOne.getId(), "too-large.png",
                oversizedPng, "image/png", "PRODUCT_IMAGE")
                .then()
                .statusCode(400)
                .body("errors[0].code", equalTo("ASSET-400-03"));
    }

    @Test
    void shouldBlockCrewUploadAndDownloadWithoutPermission() {
        String crewToken = tokenFor(crewUser, workspaceOne.getId(), Role.CREW);
        AssetEntity asset = uploadReadyAsset();

        uploadAsset(crewToken, workspaceOne.getId(), projectCampaignOne.getId(), "blocked.png",
                pngBytes(), "image/png", "REFERENCE_IMAGE")
                .then()
                .statusCode(403)
                .body("success", equalTo(false))
                .body("errors[0].code", equalTo("COMMON-403"));

        given()
                .header(HttpHeaders.AUTHORIZATION, bearer(crewToken))
                .when()
                .get("/api/v1/workspaces/{workspaceId}/assets/{assetId}/download-url", workspaceOne.getId(), asset.getId())
                .then()
                .statusCode(403)
                .body("success", equalTo(false))
                .body("errors[0].code", equalTo("COMMON-403"));
    }

    @Test
    void shouldAllowMasterToAccessAssetAcrossWorkspaceBoundary() {
        AssetEntity asset = uploadReadyAsset();
        String masterToken = tokenFor(masterUser, null, Role.MASTER);

        given()
                .header(HttpHeaders.AUTHORIZATION, bearer(masterToken))
                .when()
                .get("/api/v1/workspaces/{workspaceId}/assets/{assetId}", workspaceOne.getId(), asset.getId())
                .then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("data.id", equalTo(asset.getId().toString()));
    }

    @Test
    void shouldBlockCrossWorkspaceAssetAndSignedUrlAccess() {
        AssetEntity asset = uploadReadyAsset();
        String otherWorkspaceToken = tokenFor(workspaceTwoAdmin, workspaceTwo.getId(), Role.ADMIN);

        given()
                .header(HttpHeaders.AUTHORIZATION, bearer(otherWorkspaceToken))
                .when()
                .get("/api/v1/workspaces/{workspaceId}/assets/{assetId}", workspaceOne.getId(), asset.getId())
                .then()
                .statusCode(403)
                .body("success", equalTo(false))
                .body("errors[0].code", equalTo("TENANT-403"));

        given()
                .header(HttpHeaders.AUTHORIZATION, bearer(otherWorkspaceToken))
                .when()
                .get("/api/v1/workspaces/{workspaceId}/assets/{assetId}/preview-url", workspaceOne.getId(), asset.getId())
                .then()
                .statusCode(403)
                .body("success", equalTo(false))
                .body("errors[0].code", equalTo("TENANT-403"));
    }

    @Test
    void shouldGenerateAndCacheSignedUrlsAndTrackDownload() {
        String accessToken = tokenFor(adminUser, workspaceOne.getId(), Role.ADMIN);
        AssetEntity asset = uploadReadyAsset();
        try (Consumer<String, String> consumer = createConsumer(KafkaTopicConstants.SIGNED_URL_GENERATED, KafkaTopicConstants.ASSET_DOWNLOAD_COMPLETED)) {
            Response previewResponse = given()
                    .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                    .when()
                    .get("/api/v1/workspaces/{workspaceId}/assets/{assetId}/preview-url", workspaceOne.getId(), asset.getId())
                    .then()
                    .statusCode(200)
                    .body("success", equalTo(true))
                    .body("data.type", equalTo("preview"))
                    .body("data.cached", equalTo(false))
                    .extract()
                    .response();

            given()
                    .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                    .when()
                    .get("/api/v1/workspaces/{workspaceId}/assets/{assetId}/preview-url", workspaceOne.getId(), asset.getId())
                    .then()
                    .statusCode(200)
                    .body("success", equalTo(true))
                    .body("data.type", equalTo("preview"))
                    .body("data.cached", equalTo(true));

            Response downloadResponse = given()
                    .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                    .when()
                    .get("/api/v1/workspaces/{workspaceId}/assets/{assetId}/download-url", workspaceOne.getId(), asset.getId())
                    .then()
                    .statusCode(200)
                    .body("success", equalTo(true))
                    .body("data.type", equalTo("download"))
                    .extract()
                    .response();

            assertThat(previewResponse.jsonPath().getString("data.url")).contains("/internal/storage/local/assets/" + asset.getId() + "/preview");
            assertThat(downloadResponse.jsonPath().getString("data.url")).contains("/internal/storage/local/assets/" + asset.getId() + "/download");
            assertThat(redisSignedUrlCache.get(asset.getStorageFileId()))
                    .isPresent()
                    .get()
                    .extracting(
                            RedisSignedUrlCache.SignedUrlSnapshot::type,
                            RedisSignedUrlCache.SignedUrlSnapshot::url)
                    .satisfies(tuple -> {
                        assertThat(tuple.get(0)).isEqualTo("download");
                        assertThat((String) tuple.get(1)).contains("/download");
                    });

            URI downloadUri = URI.create(downloadResponse.jsonPath().getString("data.url"));
            given()
                    .header(HttpHeaders.USER_AGENT, "JUnit Download Client")
                    .when()
                    .get(downloadUri.getRawPath() + "?" + downloadUri.getRawQuery())
                    .then()
                    .statusCode(200);

            List<DownloadLogEntity> logs = downloadLogRepository.findAllByAssetIdAndDeletedFalse(asset.getId());
            assertThat(logs).hasSize(1);
            assertThat(logs.getFirst().getAssetId()).isEqualTo(asset.getId());
            assertThat(logs.getFirst().getDownloadType()).isEqualTo("download");
            assertThat(logs.getFirst().getUserAgent()).isEqualTo("JUnit Download Client");

            List<ConsumerRecord<String, String>> records = pollRecords(consumer, 3, Duration.ofSeconds(10));
            assertThat(records).extracting(ConsumerRecord::topic)
                    .contains(KafkaTopicConstants.SIGNED_URL_GENERATED, KafkaTopicConstants.ASSET_DOWNLOAD_COMPLETED);
            assertThat(records).anySatisfy(record -> {
                assertThat(record.value()).contains(workspaceOne.getId().toString());
                assertThat(record.value()).contains(asset.getId().toString());
            });
        }
    }

    @Test
    void shouldCacheAssetAndListMetadataAndInvalidateAfterMutationAndDelete() {
        String accessToken = tokenFor(adminUser, workspaceOne.getId(), Role.ADMIN);
        AssetEntity asset = uploadReadyAsset();

        given()
                .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                .when()
                .get("/api/v1/workspaces/{workspaceId}/assets/{assetId}", workspaceOne.getId(), asset.getId())
                .then()
                .statusCode(200);

        given()
                .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                .when()
                .get("/api/v1/workspaces/{workspaceId}/projects/{projectId}/assets", workspaceOne.getId(), projectCampaignOne.getId())
                .then()
                .statusCode(200)
                .body("success", equalTo(true));

        String assetKey = redisKeyBuilder.asset(asset.getId());
        String listKey = redisKeyBuilder.assetsList(workspaceOne.getId(), projectCampaignOne.getId(), 0);
        assertThat(redisTemplate.hasKey(assetKey)).isTrue();
        assertThat(redisTemplate.hasKey(listKey)).isTrue();

        given()
                .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "displayName": "Updated Asset",
                          "description": "Updated description",
                          "assetCategory": "REFERENCE_IMAGE",
                          "tags": ["catalog", "approved"],
                          "metadata": {"source": "integration-test"}
                        }
                        """)
                .when()
                .put("/api/v1/workspaces/{workspaceId}/assets/{assetId}", workspaceOne.getId(), asset.getId())
                .then()
                .statusCode(200)
                .body("data.displayName", equalTo("Updated Asset"))
                .body("data.assetCategory", equalTo("REFERENCE_IMAGE"));

        assertThat(redisTemplate.hasKey(listKey)).isFalse();
        assertThat(redisTemplate.hasKey(assetKey)).isTrue();

        given()
                .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                .when()
                .delete("/api/v1/workspaces/{workspaceId}/assets/{assetId}", workspaceOne.getId(), asset.getId())
                .then()
                .statusCode(200)
                .body("success", equalTo(true));

        assertThat(redisTemplate.hasKey(assetKey)).isFalse();
        assertThat(previewStateService.get(asset.getId())).isEmpty();
    }

    @Test
    void shouldPreventDuplicateUploadsAndRespectDistributedLocks() {
        String accessToken = tokenFor(adminUser, workspaceOne.getId(), Role.ADMIN);
        Response firstUpload = uploadAsset(accessToken, workspaceOne.getId(), projectCampaignOne.getId(),
                "dedupe.png", pngBytes(), "image/png", "REFERENCE_IMAGE");
        firstUpload.then().statusCode(200);
        UUID firstAssetId = UUID.fromString(firstUpload.jsonPath().getString("data.id"));

        uploadAsset(accessToken, workspaceOne.getId(), projectCampaignOne.getId(),
                "dedupe-copy.png", pngBytes(), "image/png", "REFERENCE_IMAGE")
                .then()
                .statusCode(200)
                .body("data.id", equalTo(firstAssetId.toString()));

        assertThat(assetRepository.count()).isEqualTo(1);
        assertThat(storageFileRepository.count()).isEqualTo(1);

        String sha256 = storageFileRepository.findAll().getFirst().getHash();
        RedisLockService.RedisLockToken uploadLock = redisLockService.acquire(
                redisKeyBuilder.lockUpload(sha256), Duration.ofSeconds(15)).orElseThrow();
        try {
            uploadAsset(accessToken, workspaceOne.getId(), projectCampaignOne.getId(),
                    "dedupe-locked.png", pngBytes(), "image/png", "REFERENCE_IMAGE")
                    .then()
                    .statusCode(409)
                    .body("success", equalTo(false))
                    .body("errors[0].code", equalTo("ASSET-409-02"));
        } finally {
            redisLockService.release(uploadLock);
        }
    }

    @Test
    void shouldPreventAssetMutationWhenAssetLockExists() {
        String accessToken = tokenFor(adminUser, workspaceOne.getId(), Role.ADMIN);
        AssetEntity asset = uploadReadyAsset();

        RedisLockService.RedisLockToken assetLock = redisLockService.acquire(
                redisKeyBuilder.lockAsset(asset.getId()), Duration.ofSeconds(15)).orElseThrow();
        try {
            given()
                    .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                    .contentType(ContentType.JSON)
                    .body("""
                            {
                              "displayName": "Locked Update",
                              "description": "Should not be saved"
                            }
                            """)
                    .when()
                    .put("/api/v1/workspaces/{workspaceId}/assets/{assetId}", workspaceOne.getId(), asset.getId())
                    .then()
                    .statusCode(409)
                    .body("success", equalTo(false))
                    .body("errors[0].code", equalTo("COMMON-409"));
        } finally {
            redisLockService.release(assetLock);
        }
    }

    private AssetEntity uploadReadyAsset() {
        String accessToken = tokenFor(adminUser, workspaceOne.getId(), Role.ADMIN);
        Response response = uploadAsset(
                accessToken,
                workspaceOne.getId(),
                projectCampaignOne.getId(),
                "seed.png",
                pngBytes(),
                "image/png",
                "REFERENCE_IMAGE");
        response.then().statusCode(200);
        UUID assetId = UUID.fromString(response.jsonPath().getString("data.id"));
        return assetRepository.findById(assetId).orElseThrow();
    }

    private Response uploadAsset(
            String accessToken,
            UUID workspaceId,
            UUID projectId,
            String fileName,
            byte[] content,
            String mimeType,
            String assetCategory
    ) {
        return given()
                .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                .contentType(ContentType.MULTIPART)
                .multiPart("file", fileName, content, mimeType)
                .multiPart("assetCategory", assetCategory)
                .multiPart("displayName", "Display " + fileName)
                .multiPart("description", "Uploaded for integration testing")
                .multiPart("tags", "catalog, hero")
                .multiPart("metadata", "{\"source\":\"integration-test\"}")
                .when()
                .post("/api/v1/workspaces/{workspaceId}/projects/{projectId}/assets/upload", workspaceId, projectId)
                .then()
                .extract()
                .response();
    }

    private Consumer<String, String> createConsumer(String... topics) {
        Map<String, Object> properties = new HashMap<>();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers());
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, "creative-asset-test-" + UUID.randomUUID());
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        Consumer<String, String> consumer = new DefaultKafkaConsumerFactory<String, String>(properties).createConsumer();
        consumer.subscribe(List.of(topics));
        consumer.poll(Duration.ofMillis(250));
        return consumer;
    }

    private List<ConsumerRecord<String, String>> pollRecords(
            Consumer<String, String> consumer,
            int minimumRecords,
            Duration timeout
    ) {
        long deadline = System.nanoTime() + timeout.toNanos();
        List<ConsumerRecord<String, String>> records = new java.util.ArrayList<>();
        while (System.nanoTime() < deadline && records.size() < minimumRecords) {
            ConsumerRecords<String, String> polled = consumer.poll(Duration.ofMillis(500));
            polled.forEach(records::add);
        }
        return records;
    }

    private String tokenFor(UserEntity user, UUID workspaceId, Role role) {
        return jwtAccessTokenService.generate(user, workspaceId, role).token();
    }

    private String bearer(String accessToken) {
        return "Bearer " + accessToken;
    }

    private byte[] pngBytes() {
        return java.util.Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+pC9sAAAAASUVORK5CYII=");
    }

    private byte[] jpgBytes() {
        return new byte[]{
                (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0,
                0x00, 0x10, 0x4A, 0x46, 0x49, 0x46, 0x00, 0x01
        };
    }

    private byte[] mp4Bytes() {
        return new byte[]{
                0x00, 0x00, 0x00, 0x18,
                0x66, 0x74, 0x79, 0x70,
                0x69, 0x73, 0x6F, 0x6D,
                0x00, 0x00, 0x00, 0x00
        };
    }
}
