package com.lebhas.creativesaas.identity.application;

import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.ai.application.MasterProviderSettingsService;
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
import com.lebhas.creativesaas.brand.domain.BrandEntity;
import com.lebhas.creativesaas.brand.infrastructure.persistence.BrandRepository;
import com.lebhas.creativesaas.campaign.domain.ProjectCampaignEntity;
import com.lebhas.creativesaas.campaign.infrastructure.persistence.ProjectCampaignRepository;
import com.lebhas.creativesaas.messaging.kafka.BaseDomainEvent;
import com.lebhas.creativesaas.messaging.kafka.DomainEventPublisher;
import com.lebhas.creativesaas.messaging.kafka.KafkaTopicConstants;
import com.lebhas.creativesaas.identity.application.dto.AuthSessionView;
import com.lebhas.creativesaas.identity.application.dto.LoginCommand;
import com.lebhas.creativesaas.identity.application.dto.LogoutCommand;
import com.lebhas.creativesaas.identity.application.dto.MobileOtpStartCommand;
import com.lebhas.creativesaas.identity.application.dto.MobileOtpStartView;
import com.lebhas.creativesaas.identity.application.dto.MobileOtpVerifyCommand;
import com.lebhas.creativesaas.identity.application.dto.MobileOtpVerifyView;
import com.lebhas.creativesaas.identity.application.dto.RefreshSessionCommand;
import com.lebhas.creativesaas.identity.application.dto.RegistrationBrandCommand;
import com.lebhas.creativesaas.identity.application.dto.RegistrationEmailStartCommand;
import com.lebhas.creativesaas.identity.application.dto.RegistrationEmailVerifyCommand;
import com.lebhas.creativesaas.identity.application.dto.RegistrationPasswordCommand;
import com.lebhas.creativesaas.identity.application.dto.RegistrationProductServiceCommand;
import com.lebhas.creativesaas.identity.application.dto.RegistrationProjectCampaignCommand;
import com.lebhas.creativesaas.identity.application.dto.RegistrationStepView;
import com.lebhas.creativesaas.identity.application.dto.RegisterUserCommand;
import com.lebhas.creativesaas.identity.application.dto.UserView;
import com.lebhas.creativesaas.identity.domain.AuthOtpChallenge;
import com.lebhas.creativesaas.identity.domain.OnboardingRewardPolicy;
import com.lebhas.creativesaas.identity.domain.RegistrationSession;
import com.lebhas.creativesaas.identity.domain.UserEntity;
import com.lebhas.creativesaas.identity.domain.UserStatus;
import com.lebhas.creativesaas.identity.domain.WorkspaceMembershipEntity;
import com.lebhas.creativesaas.identity.domain.WorkspaceMembershipStatus;
import com.lebhas.creativesaas.identity.infrastructure.persistence.AuthOtpChallengeRepository;
import com.lebhas.creativesaas.identity.infrastructure.persistence.RegistrationSessionRepository;
import com.lebhas.creativesaas.identity.infrastructure.persistence.UserRepository;
import com.lebhas.creativesaas.identity.infrastructure.persistence.WorkspaceMembershipRepository;
import com.lebhas.creativesaas.profile.application.ProfileRewardService;
import com.lebhas.creativesaas.profile.application.UserAccountSettingsProvisioningService;
import com.lebhas.creativesaas.profile.application.UserProfileProvisioningService;
import com.lebhas.creativesaas.product.domain.ProductServiceEntity;
import com.lebhas.creativesaas.product.infrastructure.persistence.ProductServiceRepository;
import com.lebhas.creativesaas.redis.RedisLockService;
import com.lebhas.creativesaas.redis.RedisRealtimeStateService;
import com.lebhas.creativesaas.redis.RedisSessionService;
import com.lebhas.creativesaas.usage.application.AdminFreeCreditAllocationService;
import com.lebhas.creativesaas.workspace.application.WorkspacePermissionPolicy;
import com.lebhas.creativesaas.workspace.application.WorkspaceProvisioningService;
import com.lebhas.creativesaas.workspace.domain.WorkspaceLanguage;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final AuthOtpChallengeRepository authOtpChallengeRepository;
    private final RegistrationSessionRepository registrationSessionRepository;
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
    private final OnboardingRewardPolicyService onboardingRewardPolicyService;
    private final ProfileRewardService profileRewardService;
    private final BrandRepository brandRepository;
    private final ProductServiceRepository productServiceRepository;
    private final ProjectCampaignRepository projectCampaignRepository;
    private final MasterProviderSettingsService providerSettingsService;
    private final SecureRandom secureRandom = new SecureRandom();
    private AdminFreeCreditAllocationService adminFreeCreditAllocationService;

    public AuthenticationService(
            AuthenticationManager authenticationManager,
            UserRepository userRepository,
            AuthOtpChallengeRepository authOtpChallengeRepository,
            RegistrationSessionRepository registrationSessionRepository,
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
            UserAccountSettingsProvisioningService userAccountSettingsProvisioningService,
            OnboardingRewardPolicyService onboardingRewardPolicyService,
            ProfileRewardService profileRewardService,
            BrandRepository brandRepository,
            ProductServiceRepository productServiceRepository,
            ProjectCampaignRepository projectCampaignRepository,
            MasterProviderSettingsService providerSettingsService
    ) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.authOtpChallengeRepository = authOtpChallengeRepository;
        this.registrationSessionRepository = registrationSessionRepository;
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
        this.onboardingRewardPolicyService = onboardingRewardPolicyService;
        this.profileRewardService = profileRewardService;
        this.brandRepository = brandRepository;
        this.productServiceRepository = productServiceRepository;
        this.projectCampaignRepository = projectCampaignRepository;
        this.providerSettingsService = providerSettingsService;
    }

    @Autowired(required = false)
    public void setAdminFreeCreditAllocationService(AdminFreeCreditAllocationService adminFreeCreditAllocationService) {
        this.adminFreeCreditAllocationService = adminFreeCreditAllocationService;
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
    public MobileOtpStartView startMobileOtp(MobileOtpStartCommand command) {
        OnboardingRewardPolicy policy = onboardingRewardPolicyService.requireActivePolicy();
        if (!policy.isEnableMobileOtpLogin()) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Mobile OTP login is disabled");
        }
        String mobileNumber = normalizeBangladeshMobile(command.mobileNumber());
        UserEntity user = userRepository.findFirstByPhoneAndDeletedFalseOrderByCreatedAtAsc(mobileNumber).orElse(null);
        authenticationThrottleService.assertLoginAllowed(mobileNumber, command.clientIp(), user);
        authOtpChallengeRepository.findFirstByMobileNumberAndVerifiedAtIsNullAndDeletedFalseOrderByCreatedAtDesc(mobileNumber)
                .filter(challenge -> challenge.getResendAvailableAt().isAfter(clock.instant()))
                .ifPresent(challenge -> {
                    throw new BusinessException(ErrorCode.AUTH_RATE_LIMITED, "Please wait before requesting another OTP");
                });
        boolean isNewUser = false;
        if (user == null) {
            isNewUser = true;
            user = UserEntity.register(
                    "Lebhas",
                    "User",
                    syntheticMobileEmail(mobileNumber),
                    mobileNumber,
                    passwordEncoder.encode(UUID.randomUUID().toString()),
                    Role.ADMIN,
                    UserStatus.OTP_PENDING,
                    false);
            userRepository.save(user);
            provisionProfileDefaults(user);
            workspaceProvisioningService.provisionOwnedWorkspace(
                    user.getId(),
                    new WorkspaceProvisioningService.WorkspaceSeed(
                            "Lebhas Workspace",
                            null,
                            null,
                            null,
                            null,
                            "Asia/Dhaka",
                            WorkspaceLanguage.ENGLISH,
                            "BDT",
                            "BD"));
        }

        String otpToken = UUID.randomUUID().toString() + "." + UUID.randomUUID();
        String otp = nextOtp();
        AuthOtpChallenge challenge = AuthOtpChallenge.create(
                sha256(otpToken),
                mobileNumber,
                user.getId(),
                passwordEncoder.encode(otp),
                isNewUser,
                policy.getMaxOtpAttempts(),
                clock.instant().plus(Duration.ofMinutes(policy.getOtpExpiryMinutes())),
                clock.instant().plus(Duration.ofSeconds(policy.getOtpResendCooldownSeconds())));
        authOtpChallengeRepository.save(challenge);
        providerSettingsService.sendOtp(mobileNumber, otp);
        // Development-friendly mock delivery: do not log OTP in production.
        if ("dev".equalsIgnoreCase(System.getProperty("spring.profiles.active", ""))) {
            securityAuditLogger.logLoginAttempt("mock-otp:" + otp, null, command.clientIp());
        }
        return new MobileOtpStartView(
                otpToken,
                maskMobile(mobileNumber),
                isNewUser,
                policy.getOtpResendCooldownSeconds(),
                6,
                false,
                null);
    }

    @Transactional
    public MobileOtpVerifyView verifyMobileOtp(MobileOtpVerifyCommand command) {
        if (command.otpToken() == null || command.otpToken().isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "OTP token is required");
        }
        AuthOtpChallenge challenge = authOtpChallengeRepository.findByOtpTokenHashAndDeletedFalse(sha256(command.otpToken()))
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS, "OTP token is invalid or expired"));
        if (challenge.isVerified() || challenge.getExpiresAt().isBefore(clock.instant())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS, "OTP is expired");
        }
        if (challenge.getAttempts() >= challenge.getMaxAttempts()) {
            throw new BusinessException(ErrorCode.AUTH_RATE_LIMITED, "Maximum OTP attempts reached");
        }
        UserEntity user = userRepository.findByIdAndDeletedFalse(challenge.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        challenge.recordAttempt();
        if (command.otp() == null || !passwordEncoder.matches(command.otp().trim(), challenge.getOtpHash())) {
            authOtpChallengeRepository.save(challenge);
            authenticationThrottleService.recordLoginFailure(challenge.getMobileNumber(), command.clientIp());
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS, "OTP is invalid");
        }
        challenge.markVerified(clock.instant());
        authOtpChallengeRepository.save(challenge);

        authenticationThrottleService.recordLoginSuccess(challenge.getMobileNumber(), command.clientIp());
        user.markMobileVerified(clock.instant());
        user.markLastLogin(clock.instant());
        userRepository.save(user);
        WorkspaceMembershipEntity membership = resolveLoginMembership(user.getId(), null);
        BigDecimal granted = BigDecimal.ZERO.setScale(4);
        if (challenge.isNewUser()) {
            BigDecimal configuredCredits = onboardingRewardPolicyService.requireActivePolicy().getSignupFreeCredits();
            ProfileRewardService.ProfileRewardResult rewardResult =
                    profileRewardService.grantSignupReward(membership.getWorkspaceId(), user.getId(), configuredCredits);
            granted = rewardResult.granted() ? rewardResult.creditsGranted() : BigDecimal.ZERO.setScale(4);
        }
        AuthSessionView session = issueSession(
                user,
                membership.getWorkspaceId(),
                membership.getRole(),
                command.deviceId(),
                command.clientIp(),
                command.userAgent());
        return new MobileOtpVerifyView(
                session.accessToken(),
                session.accessTokenExpiresAt(),
                session.refreshToken(),
                session.refreshTokenExpiresAt(),
                session.deviceId(),
                session.user(),
                membership.getWorkspaceId(),
                challenge.isNewUser(),
                granted);
    }

    @Transactional
    public RegistrationStepView startRegistrationMobile(MobileOtpStartCommand command) {
        OnboardingRewardPolicy policy = onboardingRewardPolicyService.requireActivePolicy();
        if (!policy.isEnableMobileOtpLogin()) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Mobile OTP registration is disabled");
        }
        String mobileNumber = normalizeBangladeshMobile(command.mobileNumber());
        UserEntity user = userRepository.findFirstByPhoneAndDeletedFalseOrderByCreatedAtAsc(mobileNumber).orElse(null);
        authenticationThrottleService.assertLoginAllowed(mobileNumber, command.clientIp(), user);
        authOtpChallengeRepository.findFirstByMobileNumberAndVerifiedAtIsNullAndDeletedFalseOrderByCreatedAtDesc(mobileNumber)
                .filter(challenge -> challenge.getResendAvailableAt().isAfter(clock.instant()))
                .ifPresent(challenge -> {
                    throw new BusinessException(ErrorCode.AUTH_RATE_LIMITED, "Please wait before requesting another OTP");
                });

        boolean isNewUser = false;
        UUID workspaceId;
        if (user == null) {
            isNewUser = true;
            user = UserEntity.register(
                    "Lebhas",
                    "User",
                    syntheticMobileEmail(mobileNumber),
                    mobileNumber,
                    passwordEncoder.encode(UUID.randomUUID().toString()),
                    Role.ADMIN,
                    UserStatus.OTP_PENDING,
                    false);
            userRepository.save(user);
            provisionProfileDefaults(user);
            WorkspaceProvisioningService.ProvisionedWorkspace provisionedWorkspace = workspaceProvisioningService.provisionOwnedWorkspace(
                    user.getId(),
                    new WorkspaceProvisioningService.WorkspaceSeed(
                            "Lebhas Workspace",
                            null,
                            null,
                            null,
                            null,
                            "Asia/Dhaka",
                            WorkspaceLanguage.ENGLISH,
                            "BDT",
                            "BD"));
            workspaceId = provisionedWorkspace.workspace().getId();
        } else {
            workspaceId = resolveLoginMembership(user.getId(), null).getWorkspaceId();
        }

        String registrationSessionToken = secureToken();
        RegistrationSession session = RegistrationSession.create(
                sha256(registrationSessionToken),
                user.getId(),
                workspaceId,
                mobileNumber,
                isNewUser,
                clock.instant().plus(Duration.ofMinutes(Math.max(30, policy.getOtpExpiryMinutes()))));
        registrationSessionRepository.save(session);

        String otp = nextOtp();
        authOtpChallengeRepository.save(AuthOtpChallenge.create(
                sha256(registrationSessionToken),
                mobileNumber,
                user.getId(),
                passwordEncoder.encode(otp),
                isNewUser,
                policy.getMaxOtpAttempts(),
                clock.instant().plus(Duration.ofMinutes(policy.getOtpExpiryMinutes())),
                clock.instant().plus(Duration.ofSeconds(policy.getOtpResendCooldownSeconds()))));
        providerSettingsService.sendOtp(mobileNumber, otp);

        return new RegistrationStepView(
                registrationSessionToken,
                "MOBILE_OTP",
                maskMobile(mobileNumber),
                null,
                isNewUser,
                policy.getOtpResendCooldownSeconds(),
                6,
                BigDecimal.ZERO.setScale(4),
                workspaceId,
                null,
                null,
                null);
    }

    @Transactional
    public RegistrationStepView verifyRegistrationMobile(MobileOtpVerifyCommand command) {
        RegistrationSession session = requireRegistrationSession(command.otpToken(), "MOBILE_OTP");
        AuthOtpChallenge challenge = requireOtpChallenge(command.otpToken(), "MOBILE");
        verifyOtpChallenge(challenge, command.otp(), session.getMobileNumber(), command.clientIp());

        UserEntity user = userRepository.findByIdAndDeletedFalse(session.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        user.markMobileVerified(clock.instant());
        userRepository.save(user);

        BigDecimal granted = BigDecimal.ZERO.setScale(4);
        if (session.isNewUser()) {
            BigDecimal configuredCredits = onboardingRewardPolicyService.requireActivePolicy().getSignupFreeCredits();
            ProfileRewardService.ProfileRewardResult rewardResult =
                    profileRewardService.grantSignupReward(session.getWorkspaceId(), user.getId(), configuredCredits);
            granted = rewardResult.granted() ? rewardResult.creditsGranted() : BigDecimal.ZERO.setScale(4);
        }
        session.advanceTo("EMAIL_OPTIONAL");
        registrationSessionRepository.save(session);
        return new RegistrationStepView(
                command.otpToken(),
                "EMAIL_OPTIONAL",
                maskMobile(session.getMobileNumber()),
                null,
                session.isNewUser(),
                0,
                6,
                granted,
                session.getWorkspaceId(),
                session.getSelectedBrandId(),
                session.getSelectedProductServiceId(),
                session.getSelectedProjectCampaignId());
    }

    @Transactional
    public RegistrationStepView skipRegistrationEmail(String registrationSessionToken) {
        RegistrationSession session = requireRegistrationSession(registrationSessionToken, "EMAIL_OPTIONAL");
        session.clearPendingEmail();
        session.advanceTo("BRAND_NAME");
        registrationSessionRepository.save(session);
        return registrationStep(registrationSessionToken, session, "BRAND_NAME", null, BigDecimal.ZERO.setScale(4));
    }

    @Transactional
    public RegistrationStepView startRegistrationEmail(RegistrationEmailStartCommand command) {
        RegistrationSession session = requireRegistrationSession(command.registrationSessionToken(), Set.of("EMAIL_OPTIONAL", "EMAIL_OTP"));
        String normalizedEmail = normalizeEmail(command.email());
        UserEntity user = userRepository.findByIdAndDeletedFalse(session.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        userRepository.findByEmailIgnoreCaseAndDeletedFalse(normalizedEmail)
                .filter(existing -> !existing.getId().equals(user.getId()))
                .ifPresent(existing -> {
                    throw new BusinessException(
                            ErrorCode.EMAIL_ALREADY_EXISTS,
                            "This email is already connected to another account. Please use a new email.");
                });
        if (!isSyntheticMobileEmail(user.getEmail()) && !user.getEmail().equalsIgnoreCase(normalizedEmail)) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "This mobile account already has an email.");
        }
        OnboardingRewardPolicy policy = onboardingRewardPolicyService.requireActivePolicy();
        authOtpChallengeRepository.findFirstByEmailAndVerifiedAtIsNullAndDeletedFalseOrderByCreatedAtDesc(normalizedEmail)
                .filter(challenge -> challenge.getResendAvailableAt().isAfter(clock.instant()))
                .ifPresent(challenge -> {
                    throw new BusinessException(ErrorCode.AUTH_RATE_LIMITED, "Please wait before requesting another OTP");
                });
        String otp = nextOtp();
        authOtpChallengeRepository.save(AuthOtpChallenge.createEmail(
                sha256(emailOtpToken(command.registrationSessionToken())),
                normalizedEmail,
                user.getId(),
                passwordEncoder.encode(otp),
                policy.getMaxOtpAttempts(),
                clock.instant().plus(Duration.ofMinutes(policy.getOtpExpiryMinutes())),
                clock.instant().plus(Duration.ofSeconds(policy.getOtpResendCooldownSeconds()))));
        session.attachPendingEmail(normalizedEmail);
        registrationSessionRepository.save(session);
        return registrationStep(
                command.registrationSessionToken(),
                session,
                "EMAIL_OTP",
                maskEmail(normalizedEmail),
                BigDecimal.ZERO.setScale(4));
    }

    @Transactional
    public RegistrationStepView verifyRegistrationEmail(RegistrationEmailVerifyCommand command) {
        RegistrationSession session = requireRegistrationSession(command.registrationSessionToken(), "EMAIL_OTP");
        AuthOtpChallenge challenge = requireOtpChallenge(command.registrationSessionToken(), "EMAIL");
        verifyOtpChallenge(challenge, command.otp(), session.getPendingEmail(), null);
        UserEntity user = userRepository.findByIdAndDeletedFalse(session.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        user.changeEmail(session.getPendingEmail(), true);
        userRepository.save(user);
        BigDecimal configuredCredits = onboardingRewardPolicyService.requireActivePolicy().getEmailRewardCredits();
        ProfileRewardService.ProfileRewardResult rewardResult =
                profileRewardService.grantEmailReward(session.getWorkspaceId(), user.getId(), configuredCredits);
        BigDecimal granted = rewardResult.granted() ? rewardResult.creditsGranted() : BigDecimal.ZERO.setScale(4);
        session.advanceTo("PASSWORD_SETUP");
        registrationSessionRepository.save(session);
        return registrationStep(
                command.registrationSessionToken(),
                session,
                "PASSWORD_SETUP",
                maskEmail(user.getEmail()),
                granted);
    }

    @Transactional
    public RegistrationStepView setRegistrationPassword(RegistrationPasswordCommand command) {
        RegistrationSession session = requireRegistrationSession(command.registrationSessionToken(), "PASSWORD_SETUP");
        validatePasswordStrength(command.password(), command.confirmPassword());
        UserEntity user = userRepository.findByIdAndDeletedFalse(session.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        user.updatePasswordHash(passwordEncoder.encode(command.password()));
        userRepository.save(user);
        session.advanceTo("BRAND_NAME");
        registrationSessionRepository.save(session);
        return registrationStep(command.registrationSessionToken(), session, "BRAND_NAME", null, BigDecimal.ZERO.setScale(4));
    }

    @Transactional
    public RegistrationStepView completeRegistrationBrand(RegistrationBrandCommand command) {
        RegistrationSession session = requireRegistrationSession(command.registrationSessionToken(), "BRAND_NAME");
        String brandName = requireBrandName(command.brandName());
        UserEntity user = userRepository.findByIdAndDeletedFalse(session.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        BrandEntity brand = brandRepository
                .findFirstByWorkspaceIdAndNameIgnoreCaseAndDeletedFalse(session.getWorkspaceId(), brandName)
                .orElseGet(() -> brandRepository.save(BrandEntity.create(
                        session.getWorkspaceId(),
                        user.getId(),
                        brandName,
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
                        null)));
        session.selectBrand(brand.getId());
        registrationSessionRepository.save(session);
        return registrationStep(command.registrationSessionToken(), session, "PRODUCT_SERVICE_NAME", null, BigDecimal.ZERO.setScale(4));
    }

    @Transactional
    public RegistrationStepView completeRegistrationProductService(RegistrationProductServiceCommand command) {
        RegistrationSession session = requireRegistrationSession(command.registrationSessionToken(), "PRODUCT_SERVICE_NAME");
        if (session.getSelectedBrandId() == null) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Brand must be selected before product/service.");
        }
        String productServiceName = requireHierarchyName(command.productServiceName(), "Product/service name");
        ProductServiceEntity productService = productServiceRepository
                .findFirstByWorkspaceIdAndBrandIdAndNameIgnoreCaseAndDeletedFalse(
                        session.getWorkspaceId(),
                        session.getSelectedBrandId(),
                        productServiceName)
                .orElseGet(() -> productServiceRepository.save(ProductServiceEntity.create(
                        session.getWorkspaceId(),
                        session.getSelectedBrandId(),
                        productServiceName,
                        null,
                        null,
                        null,
                        null)));
        session.selectProductService(productService.getId());
        registrationSessionRepository.save(session);
        return registrationStep(command.registrationSessionToken(), session, "PROJECT_CAMPAIGN_NAME", null, BigDecimal.ZERO.setScale(4));
    }

    @Transactional
    public AuthSessionView completeRegistrationProjectCampaign(RegistrationProjectCampaignCommand command) {
        RegistrationSession session = requireRegistrationSession(command.registrationSessionToken(), "PROJECT_CAMPAIGN_NAME");
        if (session.getSelectedBrandId() == null || session.getSelectedProductServiceId() == null) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Brand and product/service must be selected before project/campaign.");
        }
        String projectCampaignName = requireHierarchyName(command.projectCampaignName(), "Project/campaign name");
        UserEntity user = userRepository.findByIdAndDeletedFalse(session.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        ProjectCampaignEntity projectCampaign = projectCampaignRepository
                .findFirstByWorkspaceIdAndBrandIdAndProductServiceIdAndNameIgnoreCaseAndDeletedFalse(
                        session.getWorkspaceId(),
                        session.getSelectedBrandId(),
                        session.getSelectedProductServiceId(),
                        projectCampaignName)
                .orElseGet(() -> projectCampaignRepository.save(ProjectCampaignEntity.create(
                        session.getWorkspaceId(),
                        session.getSelectedBrandId(),
                        session.getSelectedProductServiceId(),
                        user.getId(),
                        projectCampaignName,
                        null,
                        null,
                        null,
                        null)));
        user.markLastLogin(clock.instant());
        userRepository.save(user);
        session.selectProjectCampaign(projectCampaign.getId());
        session.complete(clock.instant());
        registrationSessionRepository.save(session);
        AuthSessionView sessionView = issueSession(user, session.getWorkspaceId(), Role.ADMIN, command.deviceId(), command.clientIp(), command.userAgent());
        return new AuthSessionView(
                sessionView.accessToken(),
                sessionView.accessTokenExpiresAt(),
                sessionView.refreshToken(),
                sessionView.refreshTokenExpiresAt(),
                sessionView.deviceId(),
                sessionView.user(),
                session.getWorkspaceId(),
                session.getSelectedBrandId(),
                session.getSelectedProductServiceId(),
                session.getSelectedProjectCampaignId(),
                "CREATIVE_GENERATOR");
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
                    toUserView(user, workspaceId, effectiveRole),
                    workspaceId,
                    null,
                    null,
                    null,
                    null);
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

    private void grantFreeSignupCredits(UUID workspaceId, UUID userId) {
        if (adminFreeCreditAllocationService != null) {
            adminFreeCreditAllocationService.grantForNewWorkspace(workspaceId, userId);
        }
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
                toUserView(user, workspaceId, role),
                workspaceId,
                null,
                null,
                null,
                null);
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
        if (email == null || email.isBlank() || !email.contains("@")) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Valid email is required");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private RegistrationSession requireRegistrationSession(String registrationSessionToken, String expectedStep) {
        return requireRegistrationSession(registrationSessionToken, Set.of(expectedStep));
    }

    private RegistrationSession requireRegistrationSession(String registrationSessionToken, Set<String> expectedSteps) {
        if (registrationSessionToken == null || registrationSessionToken.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Registration session token is required");
        }
        RegistrationSession session = registrationSessionRepository.findBySessionTokenHashAndDeletedFalse(sha256(registrationSessionToken))
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS, "Registration session is invalid or expired"));
        if (session.isCompleted() || session.isExpired(clock.instant())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS, "Registration session is expired");
        }
        if (!expectedSteps.contains(session.getCurrentStep())) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Registration step cannot be skipped");
        }
        return session;
    }

    private AuthOtpChallenge requireOtpChallenge(String token, String challengeType) {
        String lookupToken = "EMAIL".equals(challengeType) ? emailOtpToken(token) : token;
        AuthOtpChallenge challenge = authOtpChallengeRepository.findByOtpTokenHashAndDeletedFalse(sha256(lookupToken))
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS, "OTP token is invalid or expired"));
        boolean typeMatches = "MOBILE".equals(challengeType) ? challenge.isMobileChallenge() : challenge.isEmailChallenge();
        if (!typeMatches) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS, "OTP token is invalid for this step");
        }
        return challenge;
    }

    private void verifyOtpChallenge(AuthOtpChallenge challenge, String otp, String identity, String clientIp) {
        if (challenge.isVerified() || challenge.getExpiresAt().isBefore(clock.instant())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS, "OTP is expired");
        }
        if (challenge.getAttempts() >= challenge.getMaxAttempts()) {
            throw new BusinessException(ErrorCode.AUTH_RATE_LIMITED, "Maximum OTP attempts reached");
        }
        challenge.recordAttempt();
        if (otp == null || !passwordEncoder.matches(otp.trim(), challenge.getOtpHash())) {
            authOtpChallengeRepository.save(challenge);
            if (clientIp != null) {
                authenticationThrottleService.recordLoginFailure(identity, clientIp);
            }
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS, "OTP is invalid");
        }
        challenge.markVerified(clock.instant());
        authOtpChallengeRepository.save(challenge);
        if (clientIp != null) {
            authenticationThrottleService.recordLoginSuccess(identity, clientIp);
        }
    }

    private RegistrationStepView registrationStep(
            String registrationSessionToken,
            RegistrationSession session,
            String nextStep,
            String emailMasked,
            BigDecimal creditsGranted
    ) {
        return new RegistrationStepView(
                registrationSessionToken,
                nextStep,
                maskMobile(session.getMobileNumber()),
                emailMasked,
                session.isNewUser(),
                0,
                6,
                creditsGranted,
                session.getWorkspaceId(),
                session.getSelectedBrandId(),
                session.getSelectedProductServiceId(),
                session.getSelectedProjectCampaignId());
    }

    private void validatePasswordStrength(String password, String confirmPassword) {
        if (password == null || confirmPassword == null || !password.equals(confirmPassword)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Password and confirm password do not match.");
        }
        if (password.length() < 8
                || !password.matches(".*[A-Z].*")
                || !password.matches(".*[a-z].*")
                || !password.matches(".*[0-9].*")) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Password must be at least 8 characters and include uppercase, lowercase, and a number.");
        }
    }

    private String requireBrandName(String brandName) {
        if (brandName == null || brandName.isBlank() || brandName.trim().length() < 2 || brandName.trim().length() > 120) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Brand name must be between 2 and 120 characters.");
        }
        return brandName.trim();
    }

    private String requireHierarchyName(String value, String label) {
        if (value == null || value.isBlank() || value.trim().length() < 2 || value.trim().length() > 140) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, label + " must be between 2 and 140 characters.");
        }
        return value.trim();
    }

    private boolean isSyntheticMobileEmail(String email) {
        return email != null && email.endsWith("@mobile.lebhas.local");
    }

    private String secureToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String emailOtpToken(String registrationSessionToken) {
        return registrationSessionToken + ":EMAIL";
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

    private String normalizeBangladeshMobile(String mobileNumber) {
        if (mobileNumber == null || mobileNumber.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Mobile number is required");
        }
        String digits = mobileNumber.replaceAll("[^0-9]", "");
        if (digits.startsWith("880")) {
            digits = "0" + digits.substring(3);
        }
        if (!digits.matches("^01[3-9][0-9]{8}$")) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Bangladesh mobile number format is invalid");
        }
        return digits;
    }

    private String syntheticMobileEmail(String mobileNumber) {
        return "mobile-" + mobileNumber + "@mobile.lebhas.local";
    }

    private String maskMobile(String mobileNumber) {
        return mobileNumber.substring(0, 3) + "****" + mobileNumber.substring(mobileNumber.length() - 3);
    }

    private String maskEmail(String email) {
        int at = email.indexOf('@');
        if (at <= 1) {
            return "***" + email.substring(Math.max(0, at));
        }
        return email.charAt(0) + "***" + email.substring(at - 1);
    }

    private String nextOtp() {
        return String.format("%06d", secureRandom.nextInt(1_000_000));
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return Base64.getEncoder().encodeToString(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private void publishSafely(String topic, BaseDomainEvent event) {
        try {
            domainEventPublisher.publish(topic, event);
        } catch (RuntimeException exception) {
            securityAuditLogger.logKafkaPublishFailure(event.getEventType(), event.getWorkspaceId(), exception.getMessage());
        }
    }
}
