package com.lebhas.creativesaas.profile.application;

import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.common.security.context.CurrentUser;
import com.lebhas.creativesaas.common.security.context.CurrentUserContext;
import com.lebhas.creativesaas.identity.application.OnboardingRewardPolicyService;
import com.lebhas.creativesaas.identity.domain.OnboardingRewardPolicy;
import com.lebhas.creativesaas.identity.domain.UserEntity;
import com.lebhas.creativesaas.identity.infrastructure.persistence.UserRepository;
import com.lebhas.creativesaas.profile.domain.ProfileRewardClaim;
import com.lebhas.creativesaas.profile.domain.ProfileRewardType;
import com.lebhas.creativesaas.profile.domain.ProfileSocialConnection;
import com.lebhas.creativesaas.profile.infrastructure.persistence.ProfileRewardClaimRepository;
import com.lebhas.creativesaas.profile.infrastructure.persistence.ProfileSocialConnectionRepository;
import com.lebhas.creativesaas.usage.application.CreditBalanceService;
import com.lebhas.creativesaas.usage.application.CreditLedgerService;
import com.lebhas.creativesaas.usage.domain.CreditLedger;
import com.lebhas.creativesaas.usage.domain.CreditLedgerTransactionType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;

@Service
public class ProfileRewardService {

    public static final String PROFILE_REWARD_REFERENCE_TYPE = "PROFILE_REWARD";

    private final CurrentUserContext currentUserContext;
    private final UserRepository userRepository;
    private final OnboardingRewardPolicyService policyService;
    private final ProfileRewardClaimRepository rewardClaimRepository;
    private final ProfileSocialConnectionRepository socialConnectionRepository;
    private final CreditBalanceService creditBalanceService;
    private final CreditLedgerService creditLedgerService;

    public ProfileRewardService(
            CurrentUserContext currentUserContext,
            UserRepository userRepository,
            OnboardingRewardPolicyService policyService,
            ProfileRewardClaimRepository rewardClaimRepository,
            ProfileSocialConnectionRepository socialConnectionRepository,
            CreditBalanceService creditBalanceService,
            CreditLedgerService creditLedgerService
    ) {
        this.currentUserContext = currentUserContext;
        this.userRepository = userRepository;
        this.policyService = policyService;
        this.rewardClaimRepository = rewardClaimRepository;
        this.socialConnectionRepository = socialConnectionRepository;
        this.creditBalanceService = creditBalanceService;
        this.creditLedgerService = creditLedgerService;
    }

    @Transactional
    public ProfileRewardResult updateEmail(String email) {
        CurrentUser currentUser = requireWorkspaceUser();
        String normalizedEmail = normalizeEmail(email);
        UserEntity user = userRepository.findByIdAndDeletedFalse(currentUser.userId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        if (!user.getEmail().equalsIgnoreCase(normalizedEmail)
                && userRepository.existsByEmailIgnoreCaseAndDeletedFalse(normalizedEmail)) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
        user.updateProfile(user.getFirstName(), user.getLastName(), normalizedEmail, user.getPhone());
        user.markEmailVerified();
        userRepository.save(user);
        OnboardingRewardPolicy policy = policyService.requireActivePolicy();
        return grant(currentUser, ProfileRewardType.EMAIL, policy.getEmailRewardCredits(), "Reward for adding email");
    }

    @Transactional
    public ProfileRewardResult connectSocial(String provider, String profileUrl) {
        CurrentUser currentUser = requireWorkspaceUser();
        String normalizedProvider = provider.trim().toUpperCase();
        String normalizedUrl = requireUrl(profileUrl);
        ProfileSocialConnection connection = socialConnectionRepository
                .findByUserIdAndProviderAndDeletedFalse(currentUser.userId(), normalizedProvider)
                .orElseGet(() -> ProfileSocialConnection.create(currentUser.userId(), normalizedProvider, normalizedUrl));
        connection.updateProfileUrl(normalizedUrl);
        socialConnectionRepository.save(connection);
        ProfileRewardType rewardType = switch (normalizedProvider) {
            case "FACEBOOK" -> ProfileRewardType.FACEBOOK;
            case "INSTAGRAM" -> ProfileRewardType.INSTAGRAM;
            default -> throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Unsupported social provider");
        };
        OnboardingRewardPolicy policy = policyService.requireActivePolicy();
        BigDecimal amount = rewardType == ProfileRewardType.FACEBOOK
                ? policy.getFacebookRewardCredits()
                : policy.getInstagramRewardCredits();
        return grant(currentUser, rewardType, amount, "Reward for connecting " + normalizedProvider.charAt(0) + normalizedProvider.substring(1).toLowerCase());
    }

    @Transactional(readOnly = true)
    public Map<String, Boolean> rewardStatus() {
        CurrentUser currentUser = requireWorkspaceUser();
        return Map.of(
                "email", claimed(currentUser, ProfileRewardType.EMAIL),
                "facebook", claimed(currentUser, ProfileRewardType.FACEBOOK),
                "instagram", claimed(currentUser, ProfileRewardType.INSTAGRAM));
    }

    public ProfileRewardResult grantSignupReward(java.util.UUID workspaceId, java.util.UUID userId, BigDecimal amount) {
        CurrentUser synthetic = new CurrentUser(userId, workspaceId, null, null, java.util.Set.of(), java.util.Set.of(), null, null);
        return grant(synthetic, ProfileRewardType.SIGNUP, amount, "One-time signup free credits");
    }

    public ProfileRewardResult grantEmailReward(java.util.UUID workspaceId, java.util.UUID userId, BigDecimal amount) {
        CurrentUser synthetic = new CurrentUser(userId, workspaceId, null, null, java.util.Set.of(), java.util.Set.of(), null, null);
        return grant(synthetic, ProfileRewardType.EMAIL, amount, "Reward for verifying email");
    }

    private ProfileRewardResult grant(CurrentUser currentUser, ProfileRewardType rewardType, BigDecimal amount, String description) {
        OnboardingRewardPolicy policy = policyService.requireActivePolicy();
        if (!rewardEnabled(policy, rewardType)) {
            return new ProfileRewardResult(rewardType.name(), false, BigDecimal.ZERO, true);
        }
        if (amount == null || amount.signum() <= 0 || claimed(currentUser, rewardType)) {
            return new ProfileRewardResult(rewardType.name(), false, BigDecimal.ZERO, true);
        }
        CreditBalanceService.BalanceMovement movement = creditBalanceService.purchase(currentUser.workspaceId(), amount);
        CreditLedger ledger = creditLedgerService.append(
                currentUser.workspaceId(),
                null,
                null,
                null,
                ledgerTransactionType(rewardType),
                amount,
                movement.balanceBefore(),
                movement.balanceAfter(),
                rewardType == ProfileRewardType.SIGNUP ? "FREE_SIGNUP_GRANT" : PROFILE_REWARD_REFERENCE_TYPE,
                currentUser.userId(),
                description,
                currentUser.userId());
        rewardClaimRepository.save(ProfileRewardClaim.create(currentUser.workspaceId(), currentUser.userId(), rewardType, amount, ledger.getId()));
        return new ProfileRewardResult(rewardType.name(), true, amount, true);
    }

    private CreditLedgerTransactionType ledgerTransactionType(ProfileRewardType rewardType) {
        return switch (rewardType) {
            case SIGNUP -> CreditLedgerTransactionType.FREE_SIGNUP_GRANT;
            case EMAIL -> CreditLedgerTransactionType.PROFILE_REWARD_EMAIL;
            case FACEBOOK -> CreditLedgerTransactionType.PROFILE_REWARD_FACEBOOK;
            case INSTAGRAM -> CreditLedgerTransactionType.PROFILE_REWARD_INSTAGRAM;
        };
    }

    private boolean rewardEnabled(OnboardingRewardPolicy policy, ProfileRewardType rewardType) {
        return switch (rewardType) {
            case SIGNUP -> policy.isEnableSignupFreeCredits();
            case EMAIL -> policy.isEnableEmailReward();
            case FACEBOOK -> policy.isEnableProfileRewards() && policy.isEnableFacebookReward();
            case INSTAGRAM -> policy.isEnableProfileRewards() && policy.isEnableInstagramReward();
        };
    }

    private boolean claimed(CurrentUser currentUser, ProfileRewardType rewardType) {
        return rewardClaimRepository.existsByWorkspaceIdAndUserIdAndRewardType(currentUser.workspaceId(), currentUser.userId(), rewardType);
    }

    private CurrentUser requireWorkspaceUser() {
        CurrentUser currentUser = currentUserContext.requireCurrentUser();
        if (currentUser.workspaceId() == null) {
            throw new BusinessException(ErrorCode.WORKSPACE_CONTEXT_REQUIRED);
        }
        return currentUser;
    }

    private static String normalizeEmail(String email) {
        if (email == null || email.isBlank() || !email.contains("@")) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Valid email is required");
        }
        return email.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private static String requireUrl(String value) {
        if (value == null || value.isBlank() || value.length() > 500) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Profile URL is required");
        }
        return value.trim();
    }

    public record ProfileRewardResult(String rewardType, boolean granted, BigDecimal creditsGranted, boolean alreadyClaimedOrCompleted) {
    }
}
