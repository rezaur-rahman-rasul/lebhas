package com.lebhas.creativesaas.profile.application;

import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.profile.domain.UserPasswordHistory;
import com.lebhas.creativesaas.profile.infrastructure.persistence.UserPasswordHistoryRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UserPasswordHistoryService {

    private static final int PASSWORD_REUSE_LOOKBACK = 5;

    private final UserPasswordHistoryRepository userPasswordHistoryRepository;
    private final PasswordEncoder passwordEncoder;

    public UserPasswordHistoryService(
            UserPasswordHistoryRepository userPasswordHistoryRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userPasswordHistoryRepository = userPasswordHistoryRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public void assertNotReused(UUID userId, String newPassword, String currentPasswordHash) {
        if (passwordEncoder.matches(newPassword, currentPasswordHash)) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "New password must be different from the current password");
        }
        boolean reused = userPasswordHistoryRepository
                .findAllByUserIdAndDeletedFalseOrderByCreatedAtDesc(userId, PageRequest.of(0, PASSWORD_REUSE_LOOKBACK))
                .stream()
                .anyMatch(history -> passwordEncoder.matches(newPassword, history.getPasswordHash()));
        if (reused) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "New password was used recently");
        }
    }

    @Transactional
    public UserPasswordHistory record(UUID userId, String encodedPasswordHash) {
        return userPasswordHistoryRepository.save(UserPasswordHistory.record(userId, encodedPasswordHash));
    }
}
