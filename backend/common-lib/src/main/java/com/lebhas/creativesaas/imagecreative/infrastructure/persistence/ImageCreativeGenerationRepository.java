package com.lebhas.creativesaas.imagecreative.infrastructure.persistence;

import com.lebhas.creativesaas.common.jpa.TenantAwareRepository;
import com.lebhas.creativesaas.imagecreative.domain.ImageCreativeGeneration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ImageCreativeGenerationRepository extends TenantAwareRepository<ImageCreativeGeneration> {

    Page<ImageCreativeGeneration> findAllByWorkspaceIdAndProjectIdAndDeletedFalseOrderByCreatedAtDesc(
            UUID workspaceId,
            UUID projectId,
            Pageable pageable);
}
