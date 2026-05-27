package com.lebhas.creativesaas.profile.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.kafka.annotation.KafkaListener;

public class ProfileEventConsumer {

    private final ObjectMapper objectMapper;
    private final ProfileEventConsumerHooks hooks;

    public ProfileEventConsumer(ObjectMapper objectMapper, ProfileEventConsumerHooks hooks) {
        this.objectMapper = objectMapper;
        this.hooks = hooks;
    }

    @KafkaListener(topics = "${platform.kafka.topic-prefix:}profile.updated", groupId = "${spring.application.name:application}-profile-events")
    public void onProfileUpdated(Object payload) {
        hooks.onProfileUpdated(convert(payload, ProfileUpdatedEventDto.class));
    }

    @KafkaListener(topics = "${platform.kafka.topic-prefix:}profile.image.upload.requested", groupId = "${spring.application.name:application}-profile-events")
    public void onProfileImageUploadRequested(Object payload) {
        hooks.onProfileImageUploadRequested(convert(payload, ProfileImageUploadRequestedEventDto.class));
    }

    @KafkaListener(topics = "${platform.kafka.topic-prefix:}profile.image.updated", groupId = "${spring.application.name:application}-profile-events")
    public void onProfileImageUpdated(Object payload) {
        hooks.onProfileImageUpdated(convert(payload, ProfileImageChangedEventDto.class));
    }

    @KafkaListener(topics = "${platform.kafka.topic-prefix:}profile.image.removed", groupId = "${spring.application.name:application}-profile-events")
    public void onProfileImageRemoved(Object payload) {
        hooks.onProfileImageRemoved(convert(payload, ProfileImageChangedEventDto.class));
    }

    @KafkaListener(topics = "${platform.kafka.topic-prefix:}profile.settings.updated", groupId = "${spring.application.name:application}-profile-events")
    public void onProfileSettingsUpdated(Object payload) {
        hooks.onProfileSettingsUpdated(convert(payload, ProfileSettingsUpdatedEventDto.class));
    }

    @KafkaListener(topics = "${platform.kafka.topic-prefix:}profile.password.changed", groupId = "${spring.application.name:application}-profile-events")
    public void onProfilePasswordChanged(Object payload) {
        hooks.onProfilePasswordChanged(convert(payload, ProfilePasswordChangedEventDto.class));
    }

    @KafkaListener(topics = "${platform.kafka.topic-prefix:}profile.session.revoked", groupId = "${spring.application.name:application}-profile-events")
    public void onProfileSessionRevoked(Object payload) {
        hooks.onProfileSessionRevoked(convert(payload, ProfileSessionRevokedEventDto.class));
    }

    @KafkaListener(topics = "${platform.kafka.topic-prefix:}profile.security.activity.created", groupId = "${spring.application.name:application}-profile-events")
    public void onProfileSecurityActivityCreated(Object payload) {
        hooks.onProfileSecurityActivityCreated(convert(payload, ProfileSecurityActivityCreatedEventDto.class));
    }

    private <T> T convert(Object payload, Class<T> type) {
        JsonNode node = objectMapper.valueToTree(payload);
        JsonNode envelopePayload = node.path("payload");
        if (!envelopePayload.isMissingNode()) {
            JsonNode attributes = envelopePayload.path("attributes");
            if (!attributes.isMissingNode()) {
                return objectMapper.convertValue(attributes, type);
            }
            return objectMapper.convertValue(envelopePayload, type);
        }
        return objectMapper.convertValue(payload, type);
    }
}
