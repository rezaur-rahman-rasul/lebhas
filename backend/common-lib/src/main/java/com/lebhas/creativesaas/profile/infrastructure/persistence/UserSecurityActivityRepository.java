package com.lebhas.creativesaas.profile.infrastructure.persistence;

import com.lebhas.creativesaas.profile.domain.UserSecurityActivity;
import com.lebhas.creativesaas.profile.domain.UserSecurityActivityType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UserSecurityActivityRepository extends JpaRepository<UserSecurityActivity, UUID> {

    List<UserSecurityActivity> findAllByUserIdAndDeletedFalseOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    List<UserSecurityActivity> findAllByUserIdAndActivityTypeAndDeletedFalseOrderByCreatedAtDesc(
            UUID userId,
            UserSecurityActivityType activityType,
            Pageable pageable);
}
