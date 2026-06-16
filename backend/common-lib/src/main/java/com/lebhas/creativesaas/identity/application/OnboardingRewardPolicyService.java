package com.lebhas.creativesaas.identity.application;

import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.common.security.Role;
import com.lebhas.creativesaas.common.security.context.CurrentUserContext;
import com.lebhas.creativesaas.identity.application.dto.OnboardingRewardPolicyCommand;
import com.lebhas.creativesaas.identity.application.dto.OnboardingRewardPolicyView;
import com.lebhas.creativesaas.identity.domain.OnboardingRewardPolicy;
import com.lebhas.creativesaas.identity.infrastructure.persistence.OnboardingRewardPolicyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OnboardingRewardPolicyService {

    private final OnboardingRewardPolicyRepository repository;
    private final CurrentUserContext currentUserContext;

    public OnboardingRewardPolicyService(OnboardingRewardPolicyRepository repository, CurrentUserContext currentUserContext) {
        this.repository = repository;
        this.currentUserContext = currentUserContext;
    }

    @Transactional(readOnly = true)
    public OnboardingRewardPolicy requireActivePolicy() {
        return repository.findFirstByActiveTrueAndDeletedFalseOrderByUpdatedAtDesc()
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Onboarding reward policy is not configured"));
    }

    @Transactional(readOnly = true)
    public OnboardingRewardPolicyView getActivePolicyView() {
        requireMaster();
        return repository.findFirstByDeletedFalseOrderByUpdatedAtDesc()
                .map(this::toView)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Onboarding reward policy is not configured"));
    }

    @Transactional
    public OnboardingRewardPolicyView save(OnboardingRewardPolicyCommand command) {
        requireMaster();
        OnboardingRewardPolicy policy = repository.findFirstByDeletedFalseOrderByUpdatedAtDesc()
                .orElseGet(() -> OnboardingRewardPolicy.create(
                        command.active(),
                        command.enableSignupFreeCredits(),
                        command.signupFreeCredits(),
                        command.enableEmailReward(),
                        command.emailRewardCredits(),
                        command.enableFacebookReward(),
                        command.facebookRewardCredits(),
                        command.enableInstagramReward(),
                        command.instagramRewardCredits(),
                        command.rewardOnlyOnce(),
                        command.enableMobileOtpLogin(),
                        command.otpExpiryMinutes(),
                        command.otpResendCooldownSeconds(),
                        command.maxOtpAttempts()));
        policy.update(
                command.active(),
                command.enableSignupFreeCredits(),
                command.signupFreeCredits(),
                command.enableEmailReward(),
                command.emailRewardCredits(),
                command.enableFacebookReward(),
                command.facebookRewardCredits(),
                command.enableInstagramReward(),
                command.instagramRewardCredits(),
                command.rewardOnlyOnce(),
                command.enableMobileOtpLogin(),
                command.otpExpiryMinutes(),
                command.otpResendCooldownSeconds(),
                command.maxOtpAttempts());
        return toView(repository.save(policy));
    }

    public OnboardingRewardPolicyView toView(OnboardingRewardPolicy policy) {
        return new OnboardingRewardPolicyView(
                policy.getId(),
                policy.isActive(),
                policy.isEnableSignupFreeCredits(),
                policy.getSignupFreeCredits(),
                policy.isEnableEmailReward(),
                policy.getEmailRewardCredits(),
                policy.isEnableFacebookReward(),
                policy.getFacebookRewardCredits(),
                policy.isEnableInstagramReward(),
                policy.getInstagramRewardCredits(),
                policy.isEnableProfileRewards(),
                policy.isRewardOnlyOnce(),
                policy.isEnableMobileOtpLogin(),
                policy.getOtpExpiryMinutes(),
                policy.getOtpResendCooldownSeconds(),
                policy.getMaxOtpAttempts(),
                policy.getUpdatedAt());
    }

    private void requireMaster() {
        if (!currentUserContext.requireCurrentUser().roles().contains(Role.MASTER)) {
            throw new BusinessException(ErrorCode.WORKSPACE_ACCESS_DENIED, "Master access is required");
        }
    }
}
