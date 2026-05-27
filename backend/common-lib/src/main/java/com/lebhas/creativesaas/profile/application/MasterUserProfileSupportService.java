package com.lebhas.creativesaas.profile.application;

import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.common.security.context.CurrentUserContext;
import com.lebhas.creativesaas.identity.domain.UserEntity;
import com.lebhas.creativesaas.identity.infrastructure.persistence.UserRepository;
import com.lebhas.creativesaas.profile.application.dto.MasterUserProfileView;
import com.lebhas.creativesaas.profile.domain.UserAccountSettings;
import com.lebhas.creativesaas.profile.domain.UserProfile;
import com.lebhas.creativesaas.profile.infrastructure.persistence.UserAccountSettingsRepository;
import com.lebhas.creativesaas.profile.infrastructure.persistence.UserProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class MasterUserProfileSupportService {

    private final CurrentUserContext currentUserContext;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserAccountSettingsRepository userAccountSettingsRepository;
    private final UserProfileMapper userProfileMapper;

    public MasterUserProfileSupportService(
            CurrentUserContext currentUserContext,
            UserRepository userRepository,
            UserProfileRepository userProfileRepository,
            UserAccountSettingsRepository userAccountSettingsRepository,
            UserProfileMapper userProfileMapper
    ) {
        this.currentUserContext = currentUserContext;
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.userAccountSettingsRepository = userAccountSettingsRepository;
        this.userProfileMapper = userProfileMapper;
    }

    @Transactional(readOnly = true)
    public MasterUserProfileView viewUserProfileMetadata(UUID userId) {
        if (!currentUserContext.requireCurrentUser().isMaster()) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        if (userId == null) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "userId is required");
        }
        UserEntity user = userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "User not found"));
        UserProfile profile = userProfileRepository.findByUserIdAndDeletedFalse(userId).orElse(null);
        UserAccountSettings settings = userAccountSettingsRepository.findByUserIdAndDeletedFalse(userId).orElse(null);
        return userProfileMapper.toMasterView(user, profile, settings);
    }
}
