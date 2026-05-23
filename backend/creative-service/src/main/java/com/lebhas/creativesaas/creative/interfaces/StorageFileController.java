package com.lebhas.creativesaas.creative.interfaces;

import com.lebhas.creativesaas.common.api.ApiResponse;
import com.lebhas.creativesaas.storage.application.StorageFileService;
import com.lebhas.creativesaas.storage.application.dto.StorageFileView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/storage-files")
@Tag(name = "Storage Files")
@SecurityRequirement(name = "bearerAuth")
public class StorageFileController {

    private final StorageFileService storageFileService;

    public StorageFileController(StorageFileService storageFileService) {
        this.storageFileService = storageFileService;
    }

    @GetMapping("/{fileId}")
    @PreAuthorize("hasAuthority('WORKSPACE_VIEW')")
    @Operation(summary = "Get storage file metadata")
    public ApiResponse<StorageFileView> getStorageFile(@PathVariable UUID workspaceId, @PathVariable UUID fileId) {
        return ApiResponse.success(storageFileService.getStorageFile(workspaceId, fileId));
    }
}
