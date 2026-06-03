package com.lebhas.creativesaas.identity.application;

import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.common.api.ApiError;
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
import com.lebhas.creativesaas.messaging.kafka.BaseDomainEvent;
import com.lebhas.creativesaas.messaging.kafka.DomainEventPublisher;
import com.lebhas.creativesaas.messaging.kafka.KafkaTopicConstants;
import com.lebhas.creativesaas.identity.application.dto.AuthSessionView;
import com.lebhas.creativesaas.identity.application.dto.LoginCommand;
import com.lebhas.creativesaas.identity.application.dto.LogoutCommand;
import com.lebhas.creativesaas.identity.application.dto.RefreshSessionCommand;
import com.lebhas.creativesaas.identity.application.dto.RegisterUserCommand;
import com.lebhas.creativesaas.identity.application.dto.UserView;
import com.lebhas.creativesaas.identity.domain.UserEntity;
import com.lebhas.creativesaas.identity.domain.UserStatus;
import com.lebhas.creativesaas.identity.domain.WorkspaceMembershipEntity;
import com.lebhas.creativesaas.identity.domain.WorkspaceMembershipStatus;
import com.lebhas.creativesaas.identity.infrastructure.persistence.UserRepository;
import com.lebhas.creativesaas.identity.infrastructure.persistence.WorkspaceMembershipRepository;
import com.lebhas.creativesaas.profile.application.UserAccountSettingsProvisioningService;
import com.lebhas.creativesaas.profile.application.UserProfileProvisioningService;
import com.lebhas.creativesaas.redis.RedisLockService;
import com.lebhas.creativesaas.redis.RedisRealtimeStateService;
import com.lebhas.creativesaas.redis.RedisSessionService;
import com.lebhas.creativesaas.workspace.application.WorkspacePermissionPolicy;
import com.lebhas.creativesaas.workspace.application.WorkspaceProvisioningService;
import com.lebhas.creativesaas.workspace.domain.WorkspaceLanguage;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final WorkspaceMembershipRepository workspaceMembershipRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtAccessTokenService jwtAccessTokenService;
    private final RefreshTokenService refreshTokenService;
    private final InvitationService invitationService;
    private final CurrentUserContext currentUserContext;
    private final WorkspaceAuthorizationService workspaceAuthorizationService;
    private final IdentityViewMapper identityViewMapper;
    private final RolePermissionRegistry rolePermissionRegistry;
    private final WorkspacePermissionPolicy workspacePermissionPolicy;
    private final WorkspaceProvisioningService workspaceProvisioningService;
    private final AccessTokenRevocationStore accessTokenRevocationStore;
    private final SecurityAuditLogger securityAuditLogger;
    private final AuthenticationThrottleService authenticationThrottleService;
    private final Clock clock;
    private final RedisSessionService redisSessionService;
    private final RedisLockService redisLockService;
    private final RedisRealtimeStateService redisRealtimeStateService;
    private final DomainEventPublisher domainEventPublisher;
    private final SessionProperties sessionProperties;
    private final UserProfileProvisioningService userProfileProvisioningService;
    private final UserAccountSettingsProvisioningService userAccountSettingsProvisioningService;

    public AuthenticationService(
            AuthenticationManager authenticationManager,
            UserRepository userRepository,
            WorkspaceMembershipRepository workspaceMembershipRepository,
            PasswordEncoder passwordEncoder,
            JwtAccessTokenService jwtAccessTokenService,
            RefreshTokenService refreshTokenService,
            InvitationService invitationService,
            CurrentUserContext currentUserContext,
            WorkspaceAuthorizationService workspaceAuthorizationService,
            IdentityViewMapper identityViewMapper,
            RolePermissionRegistry rolePermissionRegistry,
            WorkspacePermissionPolicy workspacePermissionPolicy,
            WorkspaceProvisioningService workspaceProvisioningService,
            AccessTokenRevocationStore accessTokenRevocationStore,
            SecurityAuditLogger securityAuditLogger,
            AuthenticationThrottleService authenticationThrottleService,
            Clock clock,
            RedisSessionService redisSessionService,
            RedisLockService redisLockService,
            RedisRealtimeStateService redisRealtimeStateService,
            DomainEventPublisher domainEventPublisher,
            SessionProperties sessionProperties,
            UserProfileProvisioningService userProfileProvisioningService,
            UserAccountSettingsProvisioningService userAccountSettingsProvisioningService
    ) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.workspaceMembershipRepository = workspaceMembershipRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtAccessTokenService = jwtAccessTokenService;
        this.refreshTokenService = refreshTokenService;
        this.invitationService = invitationService;
        this.currentUserContext = currentUserContext;
        this.workspaceAuthorizationService = workspaceAuthorizationService;
        this.identityViewMapper = identityViewMapper;
        this.rolePermissionRegistry = rolePermissionRegistry;
        this.workspacePermissionPolicy = workspacePermissionPolicy;
        this.workspaceProvisioningService = workspaceProvisioningService;
        this.accessTokenRevocationStore = accessTokenRevocationStore;
        this.securityAuditLogger = securityAuditLogger;
        this.authenticationThrottleService = authenticationThrottleService;
        this.clock = clock;
        this.redisSessionService = redisSessionService;
        this.redisLockService = redisLockService;
        this.redisRealtimeStateService = redisRealtimeStateService;
        this.domainEventPublisher = domainEventPublisher;
        this.sessionProperties = sessionProperties;
        this.userProfileProvisioningService = userProfileProvisioningService;
        this.userAccountSettingsProvisioningService = userAccountSettingsProvisioningService;
    }

    @Transactional
    public AuthSessionView register(RegisterUserCommand command, String clientIp, String userAgent) {
        validatePasswordConfirmation(command);
        String normalizedEmail = normalizeEmail(command.email());
        if (userRepository.existsByEmailIgnoreCaseAndDeletedFalse(normalizedEmail)) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        Role role = Role.ADMIN;
        UUID workspaceId = command.workspaceId();
        if (command.invitationToken() != null && !command.invitationToken().isBlank()) {
            InvitationService.PendingInvitation pendingInvitation = invitationService.resolvePendingInvitation(command.invitationToken(), normalizedEmail);
            workspaceId = pendingInvitation.invitation().getWorkspaceId();
            role = pendingInvitation.invitation().getRole();
            UserEntity invitedUser = createUser(command, normalizedEmail, role);
            userRepository.save(invitedUser);
            provisionProfileDefaults(invitedUser);
            workspaceMembershipRepository.save(WorkspaceMembershipEntity.create(
                    workspaceId,
                    invitedUser.getId(),
                    role,
                    WorkspaceMembershipStatus.ACTIVE,
                    pendingInvitation.invitation().getPermissions(),
                    clock.instant(),
                    pendingInvitation.invitation().getInvitedByUserId()));
            invitedUser.markLastLogin(clock.instant());
            userRepository.save(invitedUser);
            invitationService.markAccepted(pendingInvitation.invitation());
            return issueSession(invitedUser, workspaceId, role, defaultDeviceId(), clientIp, userAgent);
        }

        UserEntity user = createUser(command, normalizedEmail, role);
        userRepository.save(user);
        provisionProfileDefaults(user);
        WorkspaceProvisioningService.ProvisionedWorkspace provisionedWorkspace = workspaceProvisioningService.provisionOwnedWorkspace(
                user.getId(),
                new WorkspaceProvisioningService.WorkspaceSeed(
                        defaultWorkspaceName(command),
                        null,
                        null,
                        null,
                        null,
                        "Asia/Dhaka",
                        WorkspaceLanguage.ENGLISH,
                        "BDT",
                        "BD"));
        workspaceId = provisionedWorkspace.workspace().getId();
        user.markLastLogin(clock.instant());
        userRepository.save(user);
        return issueSession(user, workspaceId, role, defaultDeviceId(), clientIp, userAgent);
    }

    @Transactional
    public AuthSessionView login(LoginCommand command) {
        String normalizedEmail = normalizeEmail(command.email());
        UserEntity user = userRepository.findByEmailIgnoreCaseAndDeletedFalse(normalizedEmail).orElse(null);
        securityAuditLogger.logLoginAttempt(normalizedEmail, command.workspaceId(), command.clientIp());
        authenticationThrottleService.assertLoginAllowed(normalizedEmail, command.clientIp(), user);
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(normalizedEmail, command.password()));
        } catch (LockedException exception) {
            securityAuditLogger.logLoginFailure(normalizedEmail, command.workspaceId(), "account_locked");
            throw new BusinessException(ErrorCode.AUTH_RATE_LIMITED, "Account is temporarily locked due to repeated failed login attempts");
        } catch (DisabledException exception) {
            securityAuditLogger.logLoginFailure(normalizedEmail, command.workspaceId(), "account_disabled");
            throw new BusinessException(ErrorCode.USER_INACTIVE);
        } catch (BadCredentialsException exception) {
            AuthenticationThrottleService.LoginFailureState failureState =
                    authenticationThrottleService.recordLoginFailure(normalizedEmail, command.clientIp());
            if (user != null) {
                user.recordFailedLogin(clock.instant(), failureState.identityAttempts(), failureState.lockedUntil());
                userRepository.save(user);
                if (failureState.lockedUntil() != null) {
                    securityAuditLogger.logAccountLocked(user.getId(), normalizedEmail, failureState.lockedUntil());
                }
            }
            securityAuditLogger.logLoginFailure(normalizedEmail, command.workspaceId(), "bad_credentials");
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        } catch (AuthenticationException exception) {
            securityAuditLogger.logLoginFailure(normalizedEmail, command.workspaceId(), "authentication_failed");
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        user = userRepository.findByEmailIgnoreCaseAndDeletedFalse(normalizedEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));
        if (!user.isActive()) {
            securityAuditLogger.logLoginFailure(normalizedEmail, command.workspaceId(), "user_inactive");
            throw new BusinessException(ErrorCode.USER_INACTIVE);
        }
        Role effectiveRole;
        UUID workspaceId = command.workspaceId();
        if (user.getRole().isMaster()) {
            effectiveRole = Role.MASTER;
        } else {
            WorkspaceMembershipEntity membership = resolveLoginMembership(user.getId(), workspaceId);
            workspaceId = membership.getWorkspaceId();
            effectiveRole = membership.getRole();
        }
        authenticationThrottleService.recordLoginSuccess(normalizedEmail, command.clientIp());
        user.clearFailedLoginState();
        user.markLastLogin(clock.instant());
        userRepository.save(user);
        String deviceId = resolveDeviceId(command.deviceId());
        securityAuditLogger.logLoginSuccess(user.getId(), workspaceId, deviceId);
        AuthSessionView sessionView = issueSession(user, workspaceId, effectiveRole, deviceId, command.clientIp(), command.userAgent());
        publishSafely(
                KafkaTopicConstants.AUTH_LOGIN_SUCCESS,
                new BaseDomainEvent(
                        KafkaTopicConstants.AUTH_LOGIN_SUCCESS,
                        workspaceId,
                        user.getId(),
                        clock.instant(),
                        Map.of(
                                "userId", user.getId().toString(),
                                "deviceId", deviceId,
                                "email", user.getEmail())));
        return sessionView;
    }

    @Transactional
    public AuthSessionView refresh(RefreshSessionCommand command) {
        authenticationThrottleService.assertRefreshAllowed(command.refreshToken(), command.clientIp());
        RefreshTokenService.ValidatedRefreshToken validatedRefreshToken =
                refreshTokenService.validate(command.refreshToken(), command.clientIp(), command.userAgent());
        UserEntity user = userRepository.findByIdAndDeletedFalse(validatedRefreshToken.refreshToken().getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID));
        if (!user.isActive()) {
            throw new BusinessException(ErrorCode.USER_INACTIVE);
        }
        RedisLockService.RedisLockToken authLock = redisLockService.acquire(
                        "lock:auth:" + user.getId(),
                        Duration.ofSeconds(10))
                .orElseThrow(() -> new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Refresh token rotation is already in progress"));
        UUID workspaceId = validatedRefreshToken.refreshToken().getWorkspaceId();
        try {
            Role effectiveRole = workspaceId == null
                    ? user.getRole()
                    : workspaceAuthorizationService.resolveEffectiveRole(user.getId(), user.getRole(), workspaceId);
            RefreshTokenService.IssuedRefreshToken rotatedToken = refreshTokenService.rotate(
                    validatedRefreshToken,
                    user,
                    command.clientIp(),
                    command.userAgent());
            String deviceId = rotatedToken.deviceId();
            IssuedAccessToken accessToken = jwtAccessTokenService.generate(user, workspaceId, effectiveRole, deviceId);
            redisSessionService.storeUserSession(
                    user.getId(),
                    deviceId,
                    new RedisSessionService.UserSession(
                            user.getId(),
                            deviceId,
                            workspaceId,
                            user.getEmail(),
                            effectiveRole.name(),
                            command.clientIp(),
                            command.userAgent(),
                            clock.instant(),
                            clock.instant(),
                            false,
                            rotatedToken.expiresAt()),
                    Duration.between(clock.instant(), rotatedToken.expiresAt()));
            if (workspaceId != null) {
                redisRealtimeStateService.markWorkspaceSessionActive(
                        workspaceId,
                        user.getId(),
                        deviceId,
                        sessionProperties.getActiveStateTtl());
            }
            securityAuditLogger.logTokenRefresh(user.getId(), workspaceId, deviceId);
            publishSafely(
                    KafkaTopicConstants.AUTH_REFRESH_COMPLETED,
                    new BaseDomainEvent(
                            KafkaTopicConstants.AUTH_REFRESH_COMPLETED,
                            workspaceId,
                            user.getId(),
                            clock.instant(),
                            Map.of(
                                    "userId", user.getId().toString(),
                                    "deviceId", deviceId,
                                    "refreshTokenId", rotatedToken.tokenId().toString())));
            return new AuthSessionView(
                    accessToken.token(),
                    accessToken.expiresAt(),
                    rotatedToken.token(),
                    rotatedToken.expiresAt(),
                    deviceId,
                    toUserView(user, workspaceId, effectiveRole));
        } finally {
            redisLockService.release(authLock);
        }
    }

    @Transactional
    public void logout(LogoutCommand command) {
        CurrentUser currentUser = currentUserContext.requireCurrentUser();
        String deviceId = currentUser.deviceId() == null ? defaultDeviceId() : currentUser.deviceId();
        if (command.logoutAllDevices()) {
            refreshTokenService.revokeAllForUser(currentUser.userId())
                    .forEach(revokedDeviceId -> redisSessionService.deleteUserSession(currentUser.userId(), revokedDeviceId));
            redisSessionService.removeRefreshFamily(currentUser.userId());
        } else if (command.refreshToken() != null && !command.refreshToken().isBlank()) {
            refreshTokenService.revokeSilently(command.refreshToken(), currentUser.userId());
            redisSessionService.deleteUserSession(currentUser.userId(), deviceId);
        } else {
            refreshTokenService.revokeDeviceSessions(currentUser.userId(), deviceId)
                    .forEach(revokedDeviceId -> redisSessionService.deleteUserSession(currentUser.userId(), revokedDeviceId));
        }
        if (currentUser.tokenId() != null && currentUser.accessTokenExpiresAt() != null) {
            accessTokenRevocationStore.revoke(currentUser.tokenId(), currentUser.accessTokenExpiresAt());
        }
        if (currentUser.workspaceId() != null) {
            redisRealtimeStateService.clearWorkspaceSession(currentUser.workspaceId(), currentUser.userId(), deviceId);
        }
        securityAuditLogger.logLogout(currentUser.userId(), currentUser.workspaceId(), deviceId, command.logoutAllDevices());
        publishSafely(
                KafkaTopicConstants.AUTH_LOGOUT_COMPLETED,
                new BaseDomainEvent(
                        KafkaTopicConstants.AUTH_LOGOUT_COMPLETED,
                        currentUser.workspaceId(),
                        currentUser.userId(),
                        clock.instant(),
                        Map.of(
                                "userId", currentUser.userId().toString(),
                                "deviceId", deviceId,
                                "logoutAllDevices", command.logoutAllDevices())));
    }

    @Transactional(readOnly = true)
    public UserView currentUser() {
        CurrentUser currentUser = currentUserContext.requireCurrentUser();
        UserEntity user = userRepository.findByIdAndDeletedFalse(currentUser.userId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        Role role = currentUser.roles().stream().findFirst().orElse(user.getRole());
        return identityViewMapper.toUserView(user, currentUser.workspaceId(), role, currentUser.permissions());
    }

    private UserEntity createUser(RegisterUserCommand command, String normalizedEmail, Role role) {
        return UserEntity.register(
                command.firstName(),
                command.lastName(),
                normalizedEmail,
                command.phone(),
                passwordEncoder.encode(command.password()),
                role,
                UserStatus.ACTIVE,
                false);
    }

    private void provisionProfileDefaults(UserEntity user) {
        userProfileProvisioningService.provisionIfMissing(user);
        userAccountSettingsProvisioningService.provisionIfMissing(user);
    }

    private WorkspaceMembershipEntity resolveLoginMembership(UUID userId, UUID requestedWorkspaceId) {
        List<WorkspaceMembershipEntity> activeMemberships = workspaceMembershipRepository
                .findAllByUserIdAndStatusAndDeletedFalse(userId, WorkspaceMembershipStatus.ACTIVE);
        if (requestedWorkspaceId != null) {
            return activeMemberships.stream()
                    .filter(membership -> membership.getWorkspaceId().equals(requestedWorkspaceId))
                    .findFirst()
                    .orElseThrow(() -> new BusinessException(ErrorCode.WORKSPACE_ACCESS_DENIED));
        }
        if (activeMemberships.isEmpty()) {
            throw new BusinessException(ErrorCode.WORKSPACE_ACCESS_DENIED);
        }
        if (activeMemberships.size() > 1) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "workspaceId is required when the user belongs to multiple workspaces");
        }
        return activeMemberships.getFirst();
    }

    private AuthSessionView issueSession(
            UserEntity user,
            UUID workspaceId,
            Role role,
            String deviceId,
            String clientIp,
            String userAgent
    ) {
        String resolvedDeviceId = resolveDeviceId(deviceId);
        IssuedAccessToken accessToken = jwtAccessTokenService.generate(user, workspaceId, role, resolvedDeviceId);
        RefreshTokenService.IssuedRefreshToken refreshToken =
                refreshTokenService.issue(user, workspaceId, resolvedDeviceId, clientIp, userAgent);
        redisSessionService.storeUserSession(
                user.getId(),
                resolvedDeviceId,
                new RedisSessionService.UserSession(
                        user.getId(),
                        resolvedDeviceId,
                        workspaceId,
                        user.getEmail(),
                        role.name(),
                        clientIp,
                        userAgent,
                        clock.instant(),
                        clock.instant(),
                        false,
                        refreshToken.expiresAt()),
                java.time.Duration.between(clock.instant(), refreshToken.expiresAt()));
        if (workspaceId != null) {
            redisRealtimeStateService.markWorkspaceSessionActive(
                    workspaceId,
                    user.getId(),
                    resolvedDeviceId,
                    sessionProperties.getActiveStateTtl());
        }
        return new AuthSessionView(
                accessToken.token(),
                accessToken.expiresAt(),
                refreshToken.token(),
                refreshToken.expiresAt(),
                resolvedDeviceId,
                toUserView(user, workspaceId, role));
    }

    private UserView toUserView(UserEntity user, UUID workspaceId, Role role) {
        java.util.Set<Permission> permissions = workspaceId == null
                ? rolePermissionRegistry.resolve(Set.of(role))
                : workspaceAuthorizationService.resolveEffectivePermissions(user.getId(), role, workspaceId);
        return identityViewMapper.toUserView(
                user,
                workspaceId,
                role,
                permissions);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String resolveDeviceId(String deviceId) {
        return deviceId == null || deviceId.isBlank() ? defaultDeviceId() : deviceId.trim();
    }

    private String defaultDeviceId() {
        return "device-" + UUID.randomUUID();
    }

    private void validatePasswordConfirmation(RegisterUserCommand command) {
        if (command.confirmPassword() == null || !command.password().equals(command.confirmPassword())) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_FAILED,
                    ErrorCode.VALIDATION_FAILED.defaultMessage(),
                    List.of(ApiError.of(
                            ErrorCode.VALIDATION_FAILED.code(),
                            "confirmPassword",
                            "Password and confirm password do not match.")));
        }
    }

    private String defaultWorkspaceName(RegisterUserCommand command) {
        if (command.workspaceName() != null && !command.workspaceName().isBlank()) {
            return command.workspaceName().trim();
        }
        String firstName = command.firstName();
        String lastName = command.lastName();
        String normalizedFirstName = firstName == null ? "" : firstName.trim();
        String normalizedLastName = lastName == null ? "" : lastName.trim();
        String fullName = (normalizedFirstName + " " + normalizedLastName).trim();
        return fullName.isBlank() ? "Workspace" : fullName + " Workspace";
    }

    private void publishSafely(String topic, BaseDomainEvent event) {
        try {
            domainEventPublisher.publish(topic, event);
        } catch (RuntimeException exception) {
            securityAuditLogger.logKafkaPublishFailure(event.getEventType(), event.getWorkspaceId(), exception.getMessage());
        }
    }
}
