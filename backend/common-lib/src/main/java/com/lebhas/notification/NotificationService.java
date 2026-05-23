package com.lebhas.notification;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public Optional<Notification> createInternal(NotificationCreateRequest request) {
        if (notificationRepository.existsBySourceEventIdAndDeletedFalse(request.sourceEventId())) {
            return Optional.empty();
        }
        return Optional.of(notificationRepository.save(Notification.create(request)));
    }
}
