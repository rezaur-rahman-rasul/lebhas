package com.lebhas.creativesaas.profile.application;

import com.lebhas.creativesaas.profile.application.dto.SecurityActivityView;
import com.lebhas.creativesaas.profile.domain.UserSecurityActivity;
import org.springframework.stereotype.Component;

@Component
public class UserSecurityActivityMapper {

    public SecurityActivityView toView(UserSecurityActivity activity) {
        return new SecurityActivityView(
                activity.getId(),
                activity.getUserId(),
                activity.getActivityType(),
                activity.getIpAddress(),
                activity.getUserAgent(),
                activity.getLocationHint(),
                activity.isSuccess(),
                activity.getFailureReason(),
                activity.getCreatedAt());
    }
}
