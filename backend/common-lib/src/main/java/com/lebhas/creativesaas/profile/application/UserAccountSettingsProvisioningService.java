package com.lebhas.creativesaas.profile.application;

import com.lebhas.creativesaas.identity.domain.UserEntity;
import com.lebhas.creativesaas.profile.domain.UserAccountSettings;
import com.lebhas.creativesaas.profile.infrastructure.persistence.UserAccountSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserAccountSettingsProvisioningService {

    private final UserAccountSettingsRepository userAccountSettingsRepository;
    private final ProfileDefaultsFactory profileDefaultsFactory;

    public UserAccountSettingsProvisioningService(
            UserAccountSettingsRepository userAccountSettingsRepository,
            ProfileDefaultsFactory profileDefaultsFactory
    ) {
        this.userAccountSettingsRepository = userAccountSettingsRepository;
        this.profileDefaultsFactory = profileDefaultsFactory;
    }

    @Transactional
    public UserAccountSettings provisionIfMissing(UserEntity user) {
        return userAccountSettingsRepository.findByUserIdAndDeletedFalse(user.getId())
                .orElseGet(() -> createSettings(user));
    }

    private UserAccountSettings createSettings(UserEntity user) {
        ProfileDefaultsFactory.UserAccountSettingsDefaults defaults = profileDefaultsFactory.accountSettingsDefaults(user);
        return userAccountSettingsRepository.save(UserAccountSettings.create(
                defaults.userId(),
                defaults.preferredLanguage(),
                defaults.themePreference(),
                defaults.notificationEmailEnabled(),
                defaults.notificationInAppEnabled(),
                defaults.marketingEmailEnabled()));
    }
}
