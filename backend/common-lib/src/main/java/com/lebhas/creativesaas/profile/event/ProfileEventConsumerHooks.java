package com.lebhas.creativesaas.profile.event;

public interface ProfileEventConsumerHooks {

    default void onProfileUpdated(ProfileUpdatedEventDto event) {
    }

    default void onProfileImageUploadRequested(ProfileImageUploadRequestedEventDto event) {
    }

    default void onProfileImageUpdated(ProfileImageChangedEventDto event) {
    }

    default void onProfileImageRemoved(ProfileImageChangedEventDto event) {
    }

    default void onProfileSettingsUpdated(ProfileSettingsUpdatedEventDto event) {
    }

    default void onProfilePasswordChanged(ProfilePasswordChangedEventDto event) {
    }

    default void onProfileSessionRevoked(ProfileSessionRevokedEventDto event) {
    }

    default void onProfileSecurityActivityCreated(ProfileSecurityActivityCreatedEventDto event) {
    }
}
