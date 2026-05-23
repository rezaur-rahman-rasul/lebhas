package com.lebhas.creativesaas.workspace.application;

import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.workspace.infrastructure.persistence.WorkspaceRepository;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.Locale;
import java.util.UUID;

@Component
public class WorkspaceSlugService {

    private final WorkspaceRepository workspaceRepository;

    public WorkspaceSlugService(WorkspaceRepository workspaceRepository) {
        this.workspaceRepository = workspaceRepository;
    }

    public String resolveCreateSlug(String requestedSlug, String workspaceName) {
        String normalizedRequestedSlug = normalize(requestedSlug);
        if (normalizedRequestedSlug != null) {
            ensureUnique(normalizedRequestedSlug, null);
            return normalizedRequestedSlug;
        }
        String baseSlug = normalize(workspaceName);
        if (baseSlug == null) {
            baseSlug = "workspace";
        }
        String candidate = baseSlug;
        int sequence = 2;
        while (workspaceRepository.existsBySlugIgnoreCaseAndDeletedFalse(candidate)) {
            candidate = baseSlug + "-" + sequence++;
        }
        return candidate;
    }

    public String resolveUpdateSlug(String requestedSlug, String workspaceName, UUID workspaceId) {
        String normalizedRequestedSlug = normalize(requestedSlug);
        if (normalizedRequestedSlug != null) {
            ensureUnique(normalizedRequestedSlug, workspaceId);
            return normalizedRequestedSlug;
        }
        String baseSlug = normalize(workspaceName);
        if (baseSlug == null) {
            baseSlug = "workspace";
        }
        String candidate = baseSlug;
        int sequence = 2;
        while (true) {
            var existingWorkspace = workspaceRepository.findBySlugIgnoreCaseAndDeletedFalse(candidate);
            if (existingWorkspace.isEmpty() || existingWorkspace.get().getId().equals(workspaceId)) {
                return candidate;
            }
            candidate = baseSlug + "-" + sequence++;
        }
    }

    private void ensureUnique(String slug, UUID workspaceId) {
        workspaceRepository.findBySlugIgnoreCaseAndDeletedFalse(slug)
                .filter(workspace -> workspaceId == null || !workspace.getId().equals(workspaceId))
                .ifPresent(workspace -> {
                    throw new BusinessException(ErrorCode.WORKSPACE_SLUG_ALREADY_EXISTS);
                });
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String ascii = Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        String normalized = ascii.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "")
                .replaceAll("-{2,}", "-");
        return normalized.isBlank() ? null : normalized;
    }
}
