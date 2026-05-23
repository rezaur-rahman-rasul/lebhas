package com.lebhas.creativesaas.sharing.infrastructure.persistence;

import com.lebhas.creativesaas.sharing.domain.ShareLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ShareLinkRepository extends JpaRepository<ShareLink, UUID> {

    Optional<ShareLink> findByIdAndWorkspaceId(UUID id, UUID workspaceId);

    Optional<ShareLink> findByToken(String token);

    boolean existsByToken(String token);

    Optional<ShareLink> findByTokenAndWorkspaceId(String token, UUID workspaceId);

    List<ShareLink> findAllByWorkspaceIdAndGeneratedVersionIdOrderByCreatedAtDesc(UUID workspaceId, UUID generatedVersionId);
}
