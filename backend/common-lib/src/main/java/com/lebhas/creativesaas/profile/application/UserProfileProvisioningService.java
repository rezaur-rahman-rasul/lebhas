package com.lebhas.creativesaas.profile.application;

import com.lebhas.creativesaas.identity.domain.UserEntity;
import com.lebhas.creativesaas.profile.domain.UserProfile;
import com.lebhas.creativesaas.profile.infrastructure.persistence.UserProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserProfileProvisioningService {

    private final UserProfileRepository userProfileRepository;
    private final ProfileDefaultsFactory profileDefaultsFactory;

    public UserProfileProvisioningService(
            UserProfileRepository userProfileRepository,
            ProfileDefaultsFactory profileDefaultsFactory
    ) {
        this.userProfileRepository = userProfileRepository;
        this.profileDefaultsFactory = profileDefaultsFactory;
    }

    @Transactional
    public UserProfile provisionIfMissing(UserEntity user) {
        return userProfileRepository.findByUserIdAndDeletedFalse(user.getId())
                .orElseGet(() -> createProfile(user));
    }

    private UserProfile createProfile(UserEntity user) {
        ProfileDefaultsFactory.UserProfileDefaults defaults = profileDefaultsFactory.userProfileDefaults(user);
        return userProfileRepository.save(UserProfile.create(
                defaults.userId(),
                defaults.firstName(),
                defaults.lastName(),
                defaults.displayName(),
                defaults.phoneNumber(),
                defaults.jobTitle(),
                null,
                null,
                null,
                defaults.timezone(),
                defaults.locale()));
    }
}
