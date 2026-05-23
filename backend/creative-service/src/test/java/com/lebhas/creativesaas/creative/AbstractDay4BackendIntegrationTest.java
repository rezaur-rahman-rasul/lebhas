package com.lebhas.creativesaas.creative;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lebhas.asset.event.AssetKafkaTopicNames;
import com.lebhas.creativesaas.asset.cache.AssetCacheKeys;
import com.lebhas.creativesaas.asset.cache.AssetHotRedisCacheService;
import com.lebhas.creativesaas.asset.domain.AssetEntity;
import com.lebhas.creativesaas.asset.domain.StorageProvider;
import com.lebhas.creativesaas.asset.infrastructure.persistence.AssetRepository;
import com.lebhas.creativesaas.asset.storage.LocalAssetAccessMode;
import com.lebhas.creativesaas.asset.storage.StorageProperties;
import com.lebhas.creativesaas.asset.infrastructure.persistence.UploadSessionRepository;
import com.lebhas.creativesaas.brand.domain.BrandEntity;
import com.lebhas.creativesaas.brand.domain.BrandLanguagePreference;
import com.lebhas.creativesaas.brand.infrastructure.persistence.BrandRepository;
import com.lebhas.creativesaas.campaign.domain.ProjectCampaignEntity;
import com.lebhas.creativesaas.campaign.infrastructure.persistence.ProjectCampaignRepository;
import com.lebhas.creativesaas.common.security.Role;
import com.lebhas.creativesaas.common.security.jwt.JwtAccessTokenService;
import com.lebhas.creativesaas.common.tenant.TenantContext;
import com.lebhas.creativesaas.creativerequest.domain.CreativeRequestEntity;
import com.lebhas.creativesaas.creativerequest.infrastructure.persistence.CreativeRequestRepository;
import com.lebhas.creativesaas.download.domain.DownloadLogEntity;
import com.lebhas.creativesaas.download.infrastructure.persistence.DownloadLogRepository;
import com.lebhas.creativesaas.generatedversion.domain.GeneratedVersionEntity;
import com.lebhas.creativesaas.generatedversion.domain.GenerationStatus;
import com.lebhas.creativesaas.generatedversion.infrastructure.persistence.GeneratedVersionRepository;
import com.lebhas.creativesaas.identity.domain.UserEntity;
import com.lebhas.creativesaas.identity.domain.UserStatus;
import com.lebhas.creativesaas.identity.domain.WorkspaceMembershipEntity;
import com.lebhas.creativesaas.identity.domain.WorkspaceMembershipStatus;
import com.lebhas.creativesaas.identity.infrastructure.persistence.UserRepository;
import com.lebhas.creativesaas.identity.infrastructure.persistence.WorkspaceMembershipRepository;
import com.lebhas.creativesaas.product.domain.ProductServiceEntity;
import com.lebhas.creativesaas.product.infrastructure.persistence.ProductServiceRepository;
import com.lebhas.creativesaas.sharing.infrastructure.persistence.PublicShareLinkRepository;
import com.lebhas.creativesaas.storage.domain.StorageClass;
import com.lebhas.creativesaas.storage.domain.StorageFileEntity;
import com.lebhas.creativesaas.storage.domain.StorageFilePurpose;
import com.lebhas.creativesaas.storage.infrastructure.persistence.StorageFileRepository;
import com.lebhas.creativesaas.storage.infrastructure.persistence.StorageUsageRepository;
import com.lebhas.creativesaas.workspace.domain.WorkspaceEntity;
import com.lebhas.creativesaas.workspace.domain.WorkspaceLanguage;
import com.lebhas.creativesaas.workspace.infrastructure.persistence.WorkspaceRepository;
import com.lebhas.pricing.PricingPlanRepository;
import com.lebhas.pricing.WorkspaceSubscription;
import com.lebhas.pricing.WorkspaceSubscriptionRepository;
import com.lebhas.pricing.WorkspaceSubscriptionStatus;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
abstract class AbstractDay4BackendIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @Container
    static final KafkaContainer KAFKA = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.1"));

    static final Path STORAGE_ROOT = Path.of(
            System.getProperty("java.io.tmpdir"),
            "lebhas-day4-backend-tests-" + UUID.randomUUID());

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
        registry.add("platform.storage.provider", () -> "LOCAL");
        registry.add("platform.storage.local.root-path", () -> STORAGE_ROOT.toString());
        registry.add("platform.storage.local.base-url", () -> "http://localhost");
        registry.add("platform.asset.kafka.consumer-enabled", () -> "true");
    }

    protected MockMvc mockMvc;

    @Autowired
    protected WebApplicationContext webApplicationContext;

    @Autowired
    protected FilterChainProxy springSecurityFilterChain;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected WorkspaceRepository workspaceRepository;

    @Autowired
    protected WorkspaceMembershipRepository workspaceMembershipRepository;

    @Autowired
    protected BrandRepository brandRepository;

    @Autowired
    protected ProductServiceRepository productServiceRepository;

    @Autowired
    protected ProjectCampaignRepository projectCampaignRepository;

    @Autowired
    protected AssetRepository assetRepository;

    @Autowired
    protected StorageFileRepository storageFileRepository;

    @Autowired
    protected StorageUsageRepository storageUsageRepository;

    @Autowired
    protected UploadSessionRepository uploadSessionRepository;

    @Autowired
    protected DownloadLogRepository downloadLogRepository;

    @Autowired
    protected CreativeRequestRepository creativeRequestRepository;

    @Autowired
    protected GeneratedVersionRepository generatedVersionRepository;

    @Autowired
    protected PublicShareLinkRepository publicShareLinkRepository;

    @Autowired
    protected PricingPlanRepository pricingPlanRepository;

    @Autowired
    protected WorkspaceSubscriptionRepository workspaceSubscriptionRepository;

    @Autowired
    protected JwtAccessTokenService jwtAccessTokenService;

    @Autowired
    protected StringRedisTemplate redisTemplate;

    @Autowired
    protected StorageProperties storageProperties;

    @Autowired
    protected AssetHotRedisCacheService assetHotRedisCacheService;

    @Autowired
    protected AssetKafkaTopicNames assetKafkaTopicNames;

    protected UserEntity adminUser;
    protected UserEntity crewUser;
    protected UserEntity workspaceTwoAdmin;
    protected WorkspaceEntity workspaceOne;
    protected WorkspaceEntity workspaceTwo;
    protected BrandEntity brandOne;
    protected ProductServiceEntity productServiceOne;
    protected ProjectCampaignEntity projectCampaignOne;

    @BeforeEach
    void setUp() throws Exception {
        downloadLogRepository.deleteAll();
        publicShareLinkRepository.deleteAll();
        generatedVersionRepository.deleteAll();
        creativeRequestRepository.deleteAll();
        assetRepository.deleteAll();
        storageFileRepository.deleteAll();
        storageUsageRepository.deleteAll();
        uploadSessionRepository.deleteAll();
        projectCampaignRepository.deleteAll();
        productServiceRepository.deleteAll();
        brandRepository.deleteAll();
        workspaceSubscriptionRepository.deleteAll();
        workspaceMembershipRepository.deleteAll();
        workspaceRepository.deleteAll();
        userRepository.deleteAll();
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
        resetStorageRoot();
        TenantContext.clear();
        SecurityContextHolder.clearContext();
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .addFilters(springSecurityFilterChain)
                .build();

        adminUser = userRepository.save(UserEntity.register(
                "Creative",
                "Admin",
                "day4-admin@example.com",
                null,
                "{noop}unused",
                Role.ADMIN,
                UserStatus.ACTIVE,
                true));
        crewUser = userRepository.save(UserEntity.register(
                "Creative",
                "Crew",
                "day4-crew@example.com",
                null,
                "{noop}unused",
                Role.CREW,
                UserStatus.ACTIVE,
                true));
        workspaceTwoAdmin = userRepository.save(UserEntity.register(
                "Other",
                "Admin",
                "day4-other@example.com",
                null,
                "{noop}unused",
                Role.ADMIN,
                UserStatus.ACTIVE,
                true));

        workspaceOne = workspaceRepository.save(WorkspaceEntity.create(
                "Creative Workspace One",
                "day4-workspace-one",
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
                "day4-workspace-two",
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
        productServiceOne = productServiceRepository.save(ProductServiceEntity.create(
                workspaceOne.getId(),
                brandOne.getId(),
                "Creative Retainer",
                "Monthly delivery",
                "SERVICE",
                "Growth teams",
                "Fast delivery"));
        projectCampaignOne = projectCampaignRepository.save(ProjectCampaignEntity.create(
                workspaceOne.getId(),
                brandOne.getId(),
                productServiceOne.getId(),
                adminUser.getId(),
                "Spring Launch",
                "Day 4 backend test project",
                "AWARENESS",
                "FACEBOOK",
                "CAMPAIGN"));
        workspaceSubscriptionRepository.save(WorkspaceSubscription.create(
                workspaceOne.getId(),
                pricingPlanRepository.findByCodeIgnoreCaseAndDeletedFalse("BASIC").orElseThrow().getId(),
                WorkspaceSubscriptionStatus.ACTIVE,
                Instant.now(),
                Instant.now().plusSeconds(86400),
                null,
                true));
    }

    protected String tokenFor(UserEntity user, UUID workspaceId, Role role) {
        return jwtAccessTokenService.generate(user, workspaceId, role).token();
    }

    protected String bearer(String accessToken) {
        return "Bearer " + accessToken;
    }

    protected MvcResult uploadAsset(
            String accessToken,
            String fileName,
            byte[] content,
            String mimeType,
            String assetCategory
    ) throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", fileName, mimeType, content);
        return mockMvc.perform(multipart("/api/v1/assets/upload")
                        .file(file)
                        .param("projectId", projectCampaignOne.getId().toString())
                        .param("assetCategory", assetCategory)
                        .param("displayName", "Display " + fileName)
                        .param("description", "Uploaded for Day 4 backend tests")
                        .param("tags", "catalog,hero")
                        .param("metadata", "{\"source\":\"day4-backend-test\"}")
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andReturn();
    }

    protected AssetEntity uploadReadyAsset() throws Exception {
        MvcResult upload = uploadAsset(
                tokenFor(adminUser, workspaceOne.getId(), Role.ADMIN),
                "seed.png",
                pngBytes(),
                "image/png",
                "REFERENCE_IMAGE");
        assertThat(upload.getResponse().getStatus()).isEqualTo(200);
        UUID assetId = uuidAt(upload, "/data/id");
        return assetRepository.findById(assetId).orElseThrow();
    }

    protected GeneratedVersionEntity createShareableGeneratedVersion() {
        StorageFileEntity storageFile = storageFileRepository.save(StorageFileEntity.create(
                workspaceOne.getId(),
                projectCampaignOne.getId(),
                StorageProvider.LOCAL,
                "creative-saas-assets",
                "generated/workspaces/%s/projects/%s/%s".formatted(
                        workspaceOne.getId(),
                        projectCampaignOne.getId(),
                        UUID.randomUUID()),
                "http://localhost/generated/creative.png",
                "image/png",
                "png",
                2048L,
                "generated-hash-" + UUID.randomUUID(),
                1080,
                1080,
                null,
                StorageClass.STANDARD,
                StorageFilePurpose.GENERATED));
        CreativeRequestEntity creativeRequest = creativeRequestRepository.save(CreativeRequestEntity.create(
                workspaceOne.getId(),
                projectCampaignOne.getId(),
                adminUser.getId(),
                "Day 4 Share Request",
                "Create a launch-ready visual",
                null,
                "Awareness",
                "Instagram",
                "Square",
                java.util.List.of(),
                null));
        GeneratedVersionEntity generatedVersion = GeneratedVersionEntity.create(
                workspaceOne.getId(),
                creativeRequest.getId(),
                projectCampaignOne.getId(),
                1,
                "Launch Creative v1",
                storageFile.getId(),
                null,
                "test-provider",
                "test-model",
                adminUser.getId());
        ReflectionTestUtils.setField(generatedVersion, "generationStatus", GenerationStatus.COMPLETED);
        return generatedVersionRepository.save(generatedVersion);
    }

    protected JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    protected UUID uuidAt(MvcResult result, String pointer) throws Exception {
        return UUID.fromString(json(result).at(pointer).asText());
    }

    protected String textAt(MvcResult result, String pointer) throws Exception {
        return json(result).at(pointer).asText();
    }

    protected Consumer<String, String> createConsumer(String... topics) {
        Map<String, Object> properties = new HashMap<>();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers());
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, "day4-backend-test-" + UUID.randomUUID());
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        Consumer<String, String> consumer = new DefaultKafkaConsumerFactory<String, String>(properties).createConsumer();
        consumer.subscribe(List.of(topics));
        consumer.poll(Duration.ofMillis(250));
        return consumer;
    }

    protected List<ConsumerRecord<String, String>> pollRecords(
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

    protected String localSignature(UUID assetId, LocalAssetAccessMode mode, long expiresAtEpochSeconds) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(
                    storageProperties.getLocal().getSigningSecret().getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"));
            byte[] digest = mac.doFinal((assetId + "|" + mode.name() + "|" + expiresAtEpochSeconds)
                    .getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not sign local asset URL for testing", exception);
        }
    }

    protected byte[] pngBytes() {
        return java.util.Base64.getDecoder().decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+pC9sAAAAASUVORK5CYII=");
    }

    protected byte[] oversizedPngBytes() {
        byte[] oversized = new byte[(10 * 1024 * 1024) + 1];
        oversized[0] = (byte) 0x89;
        oversized[1] = 0x50;
        oversized[2] = 0x4E;
        oversized[3] = 0x47;
        return oversized;
    }

    protected List<DownloadLogEntity> downloadLogsFor(UUID assetId) {
        return downloadLogRepository.findAllByAssetIdAndDeletedFalse(assetId);
    }

    protected void assertAssetCachePresent(UUID assetId) {
        assertThat(redisTemplate.hasKey(AssetCacheKeys.asset(assetId))).isEqualTo(Boolean.TRUE);
    }

    protected void assertProjectListCachePresent() {
        assertThat(redisTemplate.hasKey(AssetCacheKeys.assetListProject(projectCampaignOne.getId(), 0)))
                .isEqualTo(Boolean.TRUE);
    }

    protected void assertSignedUrlCachePresent(UUID assetId, String type) {
        assertThat(redisTemplate.hasKey(AssetCacheKeys.signedUrl(assetId, type))).isEqualTo(Boolean.TRUE);
    }

    private void resetStorageRoot() throws IOException {
        if (Files.exists(STORAGE_ROOT)) {
            try (var paths = Files.walk(STORAGE_ROOT)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                    Files.deleteIfExists(path);
                }
            }
        }
        Files.createDirectories(STORAGE_ROOT);
    }
}
