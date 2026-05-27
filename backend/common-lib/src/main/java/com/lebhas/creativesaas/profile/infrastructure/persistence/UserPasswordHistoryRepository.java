package com.lebhas.creativesaas.profile.infrastructure.persistence;

import com.lebhas.creativesaas.profile.domain.UserPasswordHistory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UserPasswordHistoryRepository extends JpaRepository<UserPasswordHistory, UUID> {

    List<UserPasswordHistory> findAllByUserIdAndDeletedFalseOrderByCreatedAtDesc(UUID userId, Pageable pageable);
}
