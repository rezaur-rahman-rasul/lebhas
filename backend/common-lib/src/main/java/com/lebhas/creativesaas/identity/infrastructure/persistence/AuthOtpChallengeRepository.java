package com.lebhas.creativesaas.identity.infrastructure.persistence;

import com.lebhas.creativesaas.identity.domain.AuthOtpChallenge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AuthOtpChallengeRepository extends JpaRepository<AuthOtpChallenge, UUID> {

    Optional<AuthOtpChallenge> findByOtpTokenHashAndDeletedFalse(String otpTokenHash);

    Optional<AuthOtpChallenge> findFirstByMobileNumberAndVerifiedAtIsNullAndDeletedFalseOrderByCreatedAtDesc(String mobileNumber);

    Optional<AuthOtpChallenge> findFirstByEmailAndVerifiedAtIsNullAndDeletedFalseOrderByCreatedAtDesc(String email);
}
