package com.lebhas.creativesaas.foundation;

import com.lebhas.creativesaas.brand.application.BrandService;
import com.lebhas.creativesaas.brand.application.BrandViewMapper;
import com.lebhas.creativesaas.brand.domain.BrandEntity;
import com.lebhas.creativesaas.brand.domain.BrandLanguagePreference;
import com.lebhas.creativesaas.brand.infrastructure.persistence.BrandRepository;
import com.lebhas.creativesaas.campaign.application.ProjectCampaignService;
import com.lebhas.creativesaas.campaign.application.ProjectCampaignViewMapper;
import com.lebhas.creativesaas.campaign.domain.ProjectCampaignEntity;
import com.lebhas.creativesaas.campaign.infrastructure.persistence.ProjectCampaignRepository;
import com.lebhas.creativesaas.common.security.Permission;
import com.lebhas.creativesaas.common.security.Role;
import com.lebhas.creativesaas.common.security.SecurityAuditLogger;
import com.lebhas.creativesaas.common.security.authorization.RolePermissionRegistry;
import com.lebhas.creativesaas.common.security.context.CurrentUser;
import com.lebhas.creativesaas.common.security.context.CurrentUserContext;
import com.lebhas.creativesaas.common.security.jwt.IssuedAccessToken;
import com.lebhas.creativesaas.common.security.jwt.JwtAccessTokenService;
import com.lebhas.creativesaas.common.security.rate.AuthenticationThrottleService;
import com.lebhas.creativesaas.common.security.session.AccessTokenRevocationStore;
import com.lebhas.creativesaas.identity.application.AuthenticationService;
import com.lebhas.creativesaas.identity.application.IdentityViewMapper;
import com.lebhas.creativesaas.identity.application.InvitationService;
import com.lebhas.creativesaas.identity.application.MasterSupportModeService;
import com.lebhas.creativesaas.identity.application.RefreshTokenService;
import com.lebhas.creativesaas.identity.application.SessionProperties;
import com.lebhas.creativesaas.identity.application.WorkspaceAuthorizationService;
import com.lebhas.creativesaas.identity.application.dto.AuthSessionView;
import com.lebhas.creativesaas.identity.application.dto.LoginCommand;
import com.lebhas.creativesaas.identity.domain.RefreshTokenEntity;
import com.lebhas.creativesaas.identity.domain.UserEntity;
import com.lebhas.creativesaas.identity.domain.UserStatus;
import com.lebhas.creativesaas.identity.domain.WorkspaceMembershipEntity;
import com.lebhas.creativesaas.identity.infrastructure.persistence.UserRepository;
import com.lebhas.creativesaas.identity.infrastructure.persistence.WorkspaceMembershipRepository;
import com.lebhas.creativesaas.messaging.kafka.DomainEventPublisher;
import com.lebhas.creativesaas.messaging.kafka.KafkaTopicConstants;
import com.lebhas.creativesaas.product.application.ProductServiceCatalogService;
import com.lebhas.creativesaas.product.application.ProductServiceViewMapper;
import com.lebhas.creativesaas.product.domain.ProductServiceEntity;
import com.lebhas.creativesaas.product.infrastructure.persistence.ProductServiceRepository;
import com.lebhas.creativesaas.project.infrastructure.persistence.ProjectRepository;
import com.lebhas.creativesaas.redis.RedisCacheService;
import com.lebhas.creativesaas.redis.RedisKeyBuilder;
import com.lebhas.creativesaas.redis.RedisLockService;
import com.lebhas.creativesaas.redis.RedisPermissionCache;
import com.lebhas.creativesaas.redis.RedisPermissionVersionService;
import com.lebhas.creativesaas.redis.RedisRealtimeStateService;
import com.lebhas.creativesaas.redis.RedisSessionService;
import com.lebhas.creativesaas.redis.RedisWorkspaceContextCache;
import com.lebhas.creativesaas.workspace.application.WorkspaceActivityLogger;
import com.lebhas.creativesaas.workspace.application.WorkspacePermissionPolicy;
import com.lebhas.creativesaas.workspace.application.WorkspaceProvisioningService;
import com.lebhas.creativesaas.workspace.application.dto.SupportModeView;
import com.lebhas.creativesaas.workspace.domain.WorkspaceEntity;
import com.lebhas.creativesaas.workspace.domain.WorkspaceLanguage;
import com.lebhas.creativesaas.workspace.infrastructure.persistence.WorkspaceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EnterpriseDay2FoundationUnitTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-05-12T00:00:00Z"), ZoneOffset.UTC);

    @Test
    void shouldPublishLoginSuccessEventAndTrackDeviceSession() {
        AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
        UserRepository userRepository = mock(UserRepository.class);
        WorkspaceMembershipRepository workspaceMembershipRepository = mock(WorkspaceMembershipRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        JwtAccessTokenService jwtAccessTokenService = mock(JwtAccessTokenService.class);
        RefreshTokenService refreshTokenService = mock(RefreshTokenService.class);
        InvitationService invitationService = mock(InvitationService.class);
        CurrentUserContext currentUserContext = mock(CurrentUserContext.class);
        WorkspaceAuthorizationService workspaceAuthorizationService = mock(WorkspaceAuthorizationService.class);
        IdentityViewMapper identityViewMapper = new IdentityViewMapper();
        RolePermissionRegistry rolePermissionRegistry = new RolePermissionRegistry();
        WorkspacePermissionPolicy workspacePermissionPolicy = new WorkspacePermissionPolicy(rolePermissionRegistry);
        WorkspaceProvisioningService workspaceProvisioningService = mock(WorkspaceProvisioningService.class);
        AccessTokenRevocationStore accessTokenRevocationStore = mock(AccessTokenRevocationStore.class);
        SecurityAuditLogger securityAuditLogger = mock(SecurityAuditLogger.class);
        AuthenticationThrottleService authenticationThrottleService = mock(AuthenticationThrottleService.class);
        RedisSessionService redisSessionService = mock(RedisSessionService.class);
        RedisLockService redisLockService = mock(RedisLockService.class);
        RedisRealtimeStateService redisRealtimeStateService = mock(RedisRealtimeStateService.class);
        DomainEventPublisher domainEventPublisher = mock(DomainEventPublisher.class);
        SessionProperties sessionProperties = new SessionProperties();

        AuthenticationService authenticationService = new AuthenticationService(
                authenticationManager,
                userRepository,
                workspaceMembershipRepository,
                passwordEncoder,
                jwtAccessTokenService,
                refreshTokenService,
                invitationService,
                currentUserContext,
                workspaceAuthorizationService,
                identityViewMapper,
                rolePermissionRegistry,
                workspacePermissionPolicy,
                workspaceProvisioningService,
                accessTokenRevocationStore,
                securityAuditLogger,
                authenticationThrottleService,
                clock,
                redisSessionService,
                redisLockService,
                redisRealtimeStateService,
                domainEventPublisher,
                sessionProperties);

        UserEntity masterUser = UserEntity.register(
                "Master",
                "User",
                "master@example.com",
                null,
                "{bcrypt}hash",
                Role.MASTER,
                UserStatus.ACTIVE,
                true);
        ReflectionTestUtils.setField(masterUser, "id", UUID.randomUUID());

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(new UsernamePasswordAuthenticationToken("master@example.com", "ignored"));
        when(userRepository.findByEmailIgnoreCaseAndDeletedFalse("master@example.com"))
                .thenReturn(Optional.of(masterUser), Optional.of(masterUser));
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtAccessTokenService.generate(eq(masterUser), eq(null), eq(Role.MASTER), eq("browser-1")))
                .thenReturn(new IssuedAccessToken(
                        "access-token",
                        "access-token-id",
                        Instant.parse("2026-05-12T00:15:00Z"),
                        Set.of(Role.MASTER),
                        rolePermissionRegistry.resolve(Role.MASTER)));
        when(refreshTokenService.issue(eq(masterUser), eq(null), eq("browser-1"), eq("127.0.0.1"), eq("JUnit")))
                .thenReturn(new RefreshTokenService.IssuedRefreshToken(
                        "refresh-token",
                        UUID.randomUUID(),
                        Instant.parse("2026-06-11T00:00:00Z"),
                        null,
                        "browser-1",
                        UUID.randomUUID()));

        AuthSessionView sessionView = authenticationService.login(new LoginCommand(
                "master@example.com",
                "CorrectPassword!1",
                null,
                "browser-1",
                "127.0.0.1",
                "JUnit"));

        assertThat(sessionView.deviceId()).isEqualTo("browser-1");
        verify(redisSessionService).storeUserSession(eq(masterUser.getId()), eq("browser-1"), any(), any());
        verify(domainEventPublisher).publish(eq(KafkaTopicConstants.AUTH_LOGIN_SUCCESS), any());
    }

    @Test
    void shouldEnterSupportModeAndInvalidatePermissionCaches() {
        CurrentUserContext currentUserContext = mock(CurrentUserContext.class);
        WorkspaceRepository workspaceRepository = mock(WorkspaceRepository.class);
        RedisSessionService redisSessionService = mock(RedisSessionService.class);
        RedisLockService redisLockService = mock(RedisLockService.class);
        RedisWorkspaceContextCache redisWorkspaceContextCache = mock(RedisWorkspaceContextCache.class);
        RedisPermissionCache redisPermissionCache = mock(RedisPermissionCache.class);
        RedisPermissionVersionService redisPermissionVersionService = mock(RedisPermissionVersionService.class);
        DomainEventPublisher domainEventPublisher = mock(DomainEventPublisher.class);
        SecurityAuditLogger securityAuditLogger = mock(SecurityAuditLogger.class);
        SessionProperties sessionProperties = new SessionProperties();
        MasterSupportModeService masterSupportModeService = new MasterSupportModeService(
                currentUserContext,
                workspaceRepository,
                redisSessionService,
                redisLockService,
                redisWorkspaceContextCache,
                redisPermissionCache,
                redisPermissionVersionService,
                domainEventPublisher,
                securityAuditLogger,
                sessionProperties,
                clock);

        UUID masterUserId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();
        CurrentUser currentUser = new CurrentUser(
                masterUserId,
                null,
                "browser-1",
                "master@example.com",
                Set.of(Role.MASTER),
                Set.of(Permission.SUPPORT_WORKSPACE_ACCESS),
                "jwt-id",
                Instant.parse("2026-05-12T00:15:00Z"));
        WorkspaceEntity workspace = WorkspaceEntity.create(
                "Support Workspace",
                "support-workspace",
                null,
                null,
                "Agency",
                "Asia/Dhaka",
                WorkspaceLanguage.ENGLISH,
                "BDT",
                "BD",
                masterUserId);
        ReflectionTestUtils.setField(workspace, "id", workspaceId);

        when(currentUserContext.requireCurrentUser()).thenReturn(currentUser);
        when(workspaceRepository.findByIdAndDeletedFalse(workspaceId)).thenReturn(Optional.of(workspace));
        when(redisLockService.acquire(eq("lock:support:" + masterUserId), any()))
                .thenReturn(Optional.of(new RedisLockService.RedisLockToken("lock:support:" + masterUserId, "token", Instant.now(clock).plusSeconds(10))));

        SupportModeView view = masterSupportModeService.enterSupportMode(workspaceId);

        assertThat(view.active()).isTrue();
        assertThat(view.workspaceId()).isEqualTo(workspaceId);
        verify(redisSessionService).storeSupportSession(eq(masterUserId), any(), any());
        verify(redisWorkspaceContextCache).invalidate(workspaceId, masterUserId);
        verify(redisPermissionCache).invalidate(workspaceId, masterUserId);
        verify(redisPermissionVersionService).increment(workspaceId);
        verify(domainEventPublisher).publish(eq(KafkaTopicConstants.MASTER_SUPPORT_ENTERED), any());
    }

    @Test
    void shouldPublishBrandProductAndProjectCreationEvents() {
        WorkspaceAuthorizationService workspaceAuthorizationService = mock(WorkspaceAuthorizationService.class);
        BrandRepository brandRepository = mock(BrandRepository.class);
        RedisCacheService redisCacheService = mock(RedisCacheService.class);
        RedisKeyBuilder redisKeyBuilder = new RedisKeyBuilder();
        RedisLockService redisLockService = mock(RedisLockService.class);
        DomainEventPublisher domainEventPublisher = mock(DomainEventPublisher.class);
        WorkspaceActivityLogger workspaceActivityLogger = mock(WorkspaceActivityLogger.class);
        SessionProperties sessionProperties = new SessionProperties();
        BrandService brandService = new BrandService(
                workspaceAuthorizationService,
                brandRepository,
                new BrandViewMapper(),
                redisCacheService,
                redisKeyBuilder,
                redisLockService,
                domainEventPublisher,
                workspaceActivityLogger,
                sessionProperties);

        ProductServiceRepository productServiceRepository = mock(ProductServiceRepository.class);
        ProductServiceCatalogService productServiceCatalogService = new ProductServiceCatalogService(
                workspaceAuthorizationService,
                productServiceRepository,
                new ProductServiceViewMapper(),
                brandService,
                redisCacheService,
                redisKeyBuilder,
                redisLockService,
                domainEventPublisher,
                workspaceActivityLogger,
                sessionProperties);

        ProjectCampaignRepository projectCampaignRepository = mock(ProjectCampaignRepository.class);
        ProjectRepository projectRepository = mock(ProjectRepository.class);
        ProjectCampaignService projectCampaignService = new ProjectCampaignService(
                workspaceAuthorizationService,
                projectCampaignRepository,
                new ProjectCampaignViewMapper(),
                projectRepository,
                productServiceCatalogService,
                redisCacheService,
                redisKeyBuilder,
                redisLockService,
                domainEventPublisher,
                workspaceActivityLogger,
                sessionProperties);

        UUID workspaceId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();
        UUID brandId = UUID.randomUUID();
        CurrentUser currentUser = new CurrentUser(
                actorUserId,
                workspaceId,
                "browser-1",
                "admin@example.com",
                Set.of(Role.ADMIN),
                Set.of(Permission.BRAND_MANAGE, Permission.PRODUCT_MANAGE, Permission.PROJECT_CREATE),
                "jwt-id",
                Instant.parse("2026-05-12T00:15:00Z"));
        WorkspaceEntity workspace = WorkspaceEntity.create(
                "Workspace",
                "workspace",
                null,
                null,
                "Agency",
                "Asia/Dhaka",
                WorkspaceLanguage.ENGLISH,
                "BDT",
                "BD",
                actorUserId);
        WorkspaceAuthorizationService.WorkspaceAccess access = new WorkspaceAuthorizationService.WorkspaceAccess(
                workspace,
                currentUser,
                (WorkspaceMembershipEntity) null,
                Role.ADMIN,
                Set.of(Permission.BRAND_MANAGE, Permission.PRODUCT_MANAGE, Permission.PROJECT_CREATE));

        when(workspaceAuthorizationService.requirePermission(workspaceId, Permission.BRAND_MANAGE)).thenReturn(access);
        when(workspaceAuthorizationService.requirePermission(workspaceId, Permission.PRODUCT_MANAGE)).thenReturn(access);
        when(workspaceAuthorizationService.requirePermission(workspaceId, Permission.PROJECT_CREATE)).thenReturn(access);
        when(redisLockService.acquire(anyString(), any()))
                .thenReturn(Optional.of(new RedisLockService.RedisLockToken("lock", "token", Instant.now(clock).plusSeconds(10))));
        when(brandRepository.save(any(BrandEntity.class))).thenAnswer(invocation -> {
            BrandEntity entity = invocation.getArgument(0);
            if (entity.getId() == null) {
                ReflectionTestUtils.setField(entity, "id", UUID.randomUUID());
            }
            return entity;
        });
        when(productServiceRepository.save(any(ProductServiceEntity.class))).thenAnswer(invocation -> {
            ProductServiceEntity entity = invocation.getArgument(0);
            if (entity.getId() == null) {
                ReflectionTestUtils.setField(entity, "id", UUID.randomUUID());
            }
            return entity;
        });
        when(projectCampaignRepository.save(any(ProjectCampaignEntity.class))).thenAnswer(invocation -> {
            ProjectCampaignEntity entity = invocation.getArgument(0);
            if (entity.getId() == null) {
                ReflectionTestUtils.setField(entity, "id", UUID.randomUUID());
            }
            return entity;
        });

        BrandEntity savedBrand = BrandEntity.create(
                workspaceId,
                actorUserId,
                "Brand",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                BrandLanguagePreference.BOTH);
        ReflectionTestUtils.setField(savedBrand, "id", brandId);
        when(brandRepository.findByIdAndWorkspaceIdAndDeletedFalse(brandId, workspaceId)).thenReturn(Optional.of(savedBrand));

        brandService.createBrand(workspaceId, "Brand", null, null, null, null, null, null, null, null, null, null, null, null, BrandLanguagePreference.BOTH);
        verify(domainEventPublisher).publish(eq(KafkaTopicConstants.BRAND_CREATED), any());

        when(brandRepository.findByIdAndWorkspaceIdAndDeletedFalse(brandId, workspaceId))
                .thenReturn(Optional.of(BrandEntity.create(
                        workspaceId,
                        actorUserId,
                        "Brand",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        BrandLanguagePreference.BOTH)));
        ProductServiceEntity productService = ProductServiceEntity.create(
                workspaceId,
                brandId,
                "Service",
                null,
                null,
                null,
                null);
        UUID productServiceId = UUID.randomUUID();
        ReflectionTestUtils.setField(productService, "id", productServiceId);
        when(productServiceRepository.findByIdAndWorkspaceIdAndDeletedFalse(productServiceId, workspaceId))
                .thenReturn(Optional.of(productService));
        productServiceCatalogService.createProductService(workspaceId, brandId, "Service", null, null, null, null);
        verify(domainEventPublisher).publish(eq(KafkaTopicConstants.PRODUCT_SERVICE_CREATED), any());

        when(productServiceRepository.findByIdAndWorkspaceIdAndDeletedFalse(productServiceId, workspaceId))
                .thenReturn(Optional.of(productService));
        projectCampaignService.createProjectCampaign(workspaceId, productServiceId, "Project", null, null, null, null);
        verify(domainEventPublisher).publish(eq(KafkaTopicConstants.PROJECT_CAMPAIGN_CREATED), any());
    }
}
