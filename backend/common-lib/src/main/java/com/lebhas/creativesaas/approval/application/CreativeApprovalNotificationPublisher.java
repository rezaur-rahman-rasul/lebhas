package com.lebhas.creativesaas.approval.application;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@Deprecated(forRemoval = true)
public class CreativeApprovalNotificationPublisher {

    private final ApplicationEventPublisher eventPublisher;

    public CreativeApprovalNotificationPublisher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public void publish(CreativeApprovalNotificationEvent event) {
        eventPublisher.publishEvent(event);
    }
}
