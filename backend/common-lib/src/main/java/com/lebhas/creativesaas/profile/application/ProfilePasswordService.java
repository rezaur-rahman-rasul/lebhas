package com.lebhas.creativesaas.profile.application;

import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.common.security.context.CurrentUser;
import com.lebhas.creativesaas.common.security.context.CurrentUserContext;
import com.lebhas.creativesaas.identity.domain.UserEntity;
import com.lebhas.creativesaas.identity.infrastructure.persistence.UserRepository;
import com.lebhas.creativesaas.profile.application.dto.ChangePasswordRequest;
import com.lebhas.creativesaas.profile.domain.UserSecurityActivityType;
import com.lebhas.creativesaas.profile.cache.ProfileLockService;
import com.lebhas.creativesaas.profile.cache.ProfileRateLimitService;
import com.lebhas.creativesaas.profile.event.ProfileEventProducer;
import com.lebhas.creativesaas.redis.RedisLockService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

@Service
public class ProfilePasswordService {

    private static final Logger log = LoggerFactory.getLogger(ProfilePasswordService.class);

    private final CurrentUserContext currentUserContext;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordStrengthPolicyService passwordStrengthPolicyService;
    private final UserPasswordHistoryService userPasswordHistoryService;
    private final UserSecurityActivityService userSecurityActivityService;
    private final SessionRevocationService sessionRevocationService;
    private final ProfileRateLimitService profileRateLimitService;
    private final ProfileLockService profileLockService;
    private final Clock clock;
    private final ProfileEventProducer profileEventProducer;
    private ProfileNotificationActivityAuditIntegration profileIntegration;

    public ProfilePasswordService(
            CurrentUserContext currentUserContext,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            PasswordStrengthPolicyService passwordStrengthPolicyService,
            UserPasswordHistoryService userPasswordHistoryService,
            UserSecurityActivityService userSecurityActivityService,
            SessionRevocationService sessionRevocationService,
            ProfileRateLimitService profileRateLimitService,
            ProfileLockService profileLockService,
            Clock clock,
            ProfileEventProducer profileEventProducer
    ) {
        this.currentUserContext = currentUserContext;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordStrengthPolicyService = passwordStrengthPolicyService;
        this.userPasswordHistoryService = userPasswordHistoryService;
        this.userSecurityActivityService = userSecurityActivityService;
        this.sessionRevocationService = sessionRevocationService;
        this.profileRateLimitService = profileRateLimitService;
        this.profileLockService = profileLockService;
        this.clock = clock;
        this.profileEventProducer = profileEventProducer;
    }

    @Autowired(required = false)
    void setProfileIntegration(ProfileNotificationActivityAuditIntegration profileIntegration) {
        this.profileIntegration = profileIntegration;
    }

    @Transactional
    public void changeOwnPassword(ChangePasswordRequest request) {
        changeOwnPassword(request, null, null);
    }

    @Transactional
    public void changeOwnPassword(ChangePasswordRequest request, String ipAddress, String userAgent) {
        CurrentUser currentUser = currentUserContext.requireCurrentUser();
        enforceRateLimit(currentUser.userId());
        RedisLockService.RedisLockToken lockToken = profileLockService.acquirePasswordLock(currentUser.userId())
                .orElseThrow(() -> new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Password change is already in progress"));
        try {
            validateRequestShape(request);
            UserEntity user = userRepository.findByIdAndDeletedFalse(currentUser.userId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "User not found"));
            if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
                recordSecurityActivity(currentUser, ipAddress, userAgent, false, "current_password_invalid");
                integratePasswordChangeFailed(currentUser, "current_password_invalid", ipAddress, userAgent);
                throw new BusinessException(ErrorCode.INVALID_CREDENTIALS, "Current password is invalid");
            }
            passwordStrengthPolicyService.validate(request.newPassword());
            userPasswordHistoryService.assertNotReused(currentUser.userId(), request.newPassword(), user.getPassword());
            String encodedPassword = passwordEncoder.encode(request.newPassword());
            user.updatePasswordHash(encodedPassword);
            userRepository.save(user);
            userPasswordHistoryService.record(currentUser.userId(), encodedPassword);
            SessionRevocationService.RevocationResult revocationResult = request.revokeOtherSessions()
                    ? sessionRevocationService.revokeOtherSessions(currentUser)
                    : new SessionRevocationService.RevocationResult(0, java.util.Set.of());
            recordSecurityActivity(currentUser, ipAddress, userAgent, true, null);
            publishPasswordChanged(currentUser, revocationResult);
            integratePasswordChanged(currentUser, revocationResult, ipAddress, userAgent);
        } catch (BusinessException exception) {
            if (exception.getErrorCode() != ErrorCode.INVALID_CREDENTIALS) {
                String failureReason = sanitizeFailureReason(exception);
                recordSecurityActivity(currentUser, ipAddress, userAgent, false, failureReason);
                integratePasswordChangeFailed(currentUser, failureReason, ipAddress, userAgent);
            }
            throw exception;
        } finally {
            profileLockService.releaseQuietly(lockToken);
        }
    }

    private void validateRequestShape(ChangePasswordRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Password change request is required");
        }
        if (request.currentPassword() == null || request.currentPassword().isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Current password is required");
        }
        if (request.newPassword() == null || request.newPassword().isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "New password is required");
        }
        if (request.confirmPassword() == null || request.confirmPassword().isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Confirm password is required");
        }
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "New password confirmation does not match");
        }
    }

    private void enforceRateLimit(UUID userId) {
        ProfileRateLimitService.RateLimitDecision decision = profileRateLimitService.incrementPasswordChange(userId);
        if (!decision.allowed()) {
            throw new BusinessException(ErrorCode.AUTH_RATE_LIMITED, "Too many password change attempts. Try again later.");
        }
    }

    private void recordSecurityActivity(
            CurrentUser currentUser,
            String ipAddress,
            String userAgent,
            boolean success,
            String failureReason
    ) {
        try {
            userSecurityActivityService.record(
                    currentUser.userId(),
                    UserSecurityActivityType.PASSWORD_CHANGED,
                    ipAddress,
                    userAgent,
                    null,
                    success,
                    failureReason);
        } catch (RuntimeException exception) {
            log.warn("password_security_activity_hook_failed userId={} reason={}", currentUser.userId(), reason(exception));
        }
    }

    private void publishPasswordChanged(CurrentUser currentUser, SessionRevocationService.RevocationResult revocationResult) {
        profileEventProducer.profilePasswordChanged(
                currentUser.workspaceId(),
                currentUser.userId(),
                currentUser.userId(),
                !revocationResult.revokedDeviceIds().isEmpty(),
                revocationResult.revokedDeviceIds().size());
    }

    private void integratePasswordChanged(
            CurrentUser currentUser,
            SessionRevocationService.RevocationResult revocationResult,
            String ipAddress,
            String userAgent
    ) {
        if (profileIntegration != null) {
            profileIntegration.passwordChanged(currentUser, revocationResult, ipAddress, userAgent);
        }
    }

    private void integratePasswordChangeFailed(
            CurrentUser currentUser,
            String failureReason,
            String ipAddress,
            String userAgent
    ) {
        if (profileIntegration != null) {
            profileIntegration.passwordChangeFailed(currentUser, failureReason, ipAddress, userAgent);
        }
    }

    private static String sanitizeFailureReason(BusinessException exception) {
        if (exception.getErrorCode() == ErrorCode.AUTH_RATE_LIMITED) {
            return "password_rate_limited";
        }
        if (exception.getErrorCode() == ErrorCode.BUSINESS_RULE_VIOLATION) {
            return "password_reuse_blocked";
        }
        return "password_validation_failed";
    }

    private static String reason(RuntimeException exception) {
        return exception.getMessage() == null || exception.getMessage().isBlank()
                ? exception.getClass().getSimpleName()
                : exception.getMessage().replaceAll("\\s+", " ").trim();
    }
}
