package com.lebhas.creativesaas.identity.infrastructure.persistence;

import com.lebhas.creativesaas.identity.domain.RegistrationSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RegistrationSessionRepository extends JpaRepository<RegistrationSession, UUID> {

    Optional<RegistrationSession> findBySessionTokenHashAndDeletedFalse(String sessionTokenHash);
}
