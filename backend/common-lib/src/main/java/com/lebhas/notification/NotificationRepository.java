package com.lebhas.notification;

import com.lebhas.creativesaas.common.jpa.TenantAwareRepository;

public interface NotificationRepository extends TenantAwareRepository<Notification> {

    boolean existsBySourceEventIdAndDeletedFalse(String sourceEventId);
}
