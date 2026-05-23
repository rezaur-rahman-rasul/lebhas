package com.lebhas.approval.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Create an approval comment.")
public record ApprovalCommentCreateRequest(
        @NotBlank(message = "commentText is required")
        @Size(max = 2000, message = "commentText must be 2000 characters or fewer")
        @Schema(description = "Comment text.", maxLength = 2000, requiredMode = Schema.RequiredMode.REQUIRED)
        String commentText,
        @Schema(description = "Marks the comment as reviewer-internal. Only reviewers, admins, or master support users can create internal comments.")
        Boolean internalOnly
) {
}
