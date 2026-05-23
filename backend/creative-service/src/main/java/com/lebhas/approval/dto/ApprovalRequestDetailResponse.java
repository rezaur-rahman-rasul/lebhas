package com.lebhas.approval.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Detailed approval request response.")
public record ApprovalRequestDetailResponse(
        @Schema(description = "Approval request summary.")
        ApprovalRequestResponse approvalRequest,
        @ArraySchema(schema = @Schema(implementation = ApprovalAssignmentResponse.class))
        List<ApprovalAssignmentResponse> assignments,
        @ArraySchema(schema = @Schema(implementation = ApprovalReviewResponse.class))
        List<ApprovalReviewResponse> reviews,
        @ArraySchema(schema = @Schema(implementation = ApprovalAuditLogResponse.class))
        List<ApprovalAuditLogResponse> auditTrail
) {
}
