package com.lebhas.creativesaas.notification.interfaces;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record NotificationPreferenceUpdateRequest(
        @NotEmpty List<@Valid PreferenceItem> preferences
) {
    public record PreferenceItem(
            @NotBlank String notificationType,
            boolean inAppEnabled,
            boolean emailEnabled,
            boolean smsEnabled,
            boolean pushEnabled
    ) {
    }
}
