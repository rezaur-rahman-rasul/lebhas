package com.lebhas.creativesaas.common.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    VALIDATION_FAILED("COMMON-400", "Validation failed", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED("COMMON-401", "Authentication is required", HttpStatus.UNAUTHORIZED),
    FORBIDDEN("COMMON-403", "Access denied", HttpStatus.FORBIDDEN),
    RESOURCE_NOT_FOUND("COMMON-404", "Resource not found", HttpStatus.NOT_FOUND),
    BUSINESS_RULE_VIOLATION("COMMON-409", "Business rule violation", HttpStatus.CONFLICT),
    TENANT_HEADER_INVALID("TENANT-400", "Workspace header is invalid", HttpStatus.BAD_REQUEST),
    WORKSPACE_CONTEXT_REQUIRED("TENANT-401", "Workspace context is required", HttpStatus.BAD_REQUEST),
    WORKSPACE_ACCESS_DENIED("TENANT-403", "Workspace access denied", HttpStatus.FORBIDDEN),
    INVALID_CREDENTIALS("AUTH-401", "Invalid email or password", HttpStatus.UNAUTHORIZED),
    TOKEN_INVALID("AUTH-401-01", "Token is invalid", HttpStatus.UNAUTHORIZED),
    TOKEN_EXPIRED("AUTH-401-02", "Token has expired", HttpStatus.UNAUTHORIZED),
    TOKEN_REVOKED("AUTH-401-03", "Token has been revoked", HttpStatus.UNAUTHORIZED),
    REFRESH_TOKEN_INVALID("AUTH-401-04", "Refresh token is invalid", HttpStatus.UNAUTHORIZED),
    AUTH_RATE_LIMITED("AUTH-429-01", "Too many authentication attempts. Try again later.", HttpStatus.TOO_MANY_REQUESTS),
    AUTH_RATE_LIMITER_UNAVAILABLE("AUTH-503-01", "Authentication protection is temporarily unavailable", HttpStatus.SERVICE_UNAVAILABLE),
    INVITATION_INVALID("AUTH-400-01", "Invitation is invalid", HttpStatus.BAD_REQUEST),
    EMAIL_ALREADY_EXISTS("USER-409-01", "Email is already registered", HttpStatus.CONFLICT),
    USER_INACTIVE("USER-403-01", "User account is not active", HttpStatus.FORBIDDEN),
    WORKSPACE_NOT_FOUND("WORKSPACE-404-01", "Workspace not found", HttpStatus.NOT_FOUND),
    WORKSPACE_SLUG_ALREADY_EXISTS("WORKSPACE-409-01", "Workspace slug is already in use", HttpStatus.CONFLICT),
    WORKSPACE_MEMBER_NOT_FOUND("WORKSPACE-404-02", "Workspace member not found", HttpStatus.NOT_FOUND),
    WORKSPACE_OWNER_REQUIRED("WORKSPACE-403-01", "Workspace owner access is required", HttpStatus.FORBIDDEN),
    WORKSPACE_PERMISSION_INVALID("WORKSPACE-400-01", "Workspace permission assignment is invalid", HttpStatus.BAD_REQUEST),
    BRAND_NOT_FOUND("BRAND-404-01", "Brand not found", HttpStatus.NOT_FOUND),
    PRODUCT_SERVICE_NOT_FOUND("PRODUCT-404-01", "Product or service not found", HttpStatus.NOT_FOUND),
    PROJECT_NOT_FOUND("PROJECT-404-01", "Project not found", HttpStatus.NOT_FOUND),
    PROJECT_CAMPAIGN_NOT_FOUND("CAMPAIGN-404-01", "Project or campaign not found", HttpStatus.NOT_FOUND),
    CREATIVE_REQUEST_FOUNDATION_NOT_FOUND("CREATIVE-404-01", "Creative request not found", HttpStatus.NOT_FOUND),
    GENERATED_VERSION_NOT_FOUND("VERSION-404-01", "Generated version not found", HttpStatus.NOT_FOUND),
    CREDIT_WALLET_NOT_FOUND("CREDIT-404-01", "Credit wallet not found", HttpStatus.NOT_FOUND),
    CREDIT_BALANCE_INSUFFICIENT("CREDIT-409-01", "Insufficient available credits", HttpStatus.CONFLICT),
    CREDIT_RESERVE_INVALID("CREDIT-409-02", "Credit reservation is not valid", HttpStatus.CONFLICT),
    PLAN_QUOTA_EXCEEDED("PLAN-409-01", "Plan quota exceeded", HttpStatus.CONFLICT),
    PLAN_FEATURE_DISABLED("PLAN-403-01", "Plan feature is not available", HttpStatus.FORBIDDEN),
    PLAN_SUBSCRIPTION_INACTIVE("PLAN-403-02", "Workspace subscription is not active", HttpStatus.FORBIDDEN),
    ASSET_NOT_FOUND("ASSET-404-01", "Asset not found", HttpStatus.NOT_FOUND),
    ASSET_FOLDER_NOT_FOUND("ASSET-404-02", "Asset folder not found", HttpStatus.NOT_FOUND),
    ASSET_FILE_EMPTY("ASSET-400-01", "Uploaded file must not be empty", HttpStatus.BAD_REQUEST),
    ASSET_FILE_TYPE_INVALID("ASSET-400-02", "Asset file type is not supported", HttpStatus.BAD_REQUEST),
    ASSET_FILE_SIZE_EXCEEDED("ASSET-400-03", "Asset file size exceeds the allowed limit", HttpStatus.BAD_REQUEST),
    ASSET_FILENAME_INVALID("ASSET-400-04", "Asset filename is invalid", HttpStatus.BAD_REQUEST),
    ASSET_FILE_CONTENT_INVALID("ASSET-400-05", "Asset file content is invalid", HttpStatus.BAD_REQUEST),
    ASSET_FOLDER_NOT_EMPTY("ASSET-409-01", "Asset folder must be empty before deletion", HttpStatus.CONFLICT),
    ASSET_DUPLICATE_UPLOAD_IN_PROGRESS("ASSET-409-02", "A matching asset upload is already in progress", HttpStatus.CONFLICT),
    ASSET_PREVIEW_NOT_READY("ASSET-409-03", "Asset preview is not ready", HttpStatus.CONFLICT),
    ASSET_STORAGE_FAILURE("ASSET-500-01", "Asset storage operation failed", HttpStatus.INTERNAL_SERVER_ERROR),
    ASSET_URL_INVALID("ASSET-403-01", "Asset URL is invalid", HttpStatus.FORBIDDEN),
    ASSET_URL_EXPIRED("ASSET-403-02", "Asset URL has expired", HttpStatus.FORBIDDEN),
    ASSET_METADATA_INVALID("ASSET-400-06", "Asset metadata is invalid", HttpStatus.BAD_REQUEST),
    UPLOAD_SESSION_NOT_FOUND("UPLOAD-404-01", "Upload session not found", HttpStatus.NOT_FOUND),
    STORAGE_FILE_NOT_FOUND("STORAGE-404-01", "Storage file not found", HttpStatus.NOT_FOUND),
    STORAGE_FILE_HASH_FAILED("STORAGE-500-01", "Storage file hash calculation failed", HttpStatus.INTERNAL_SERVER_ERROR),
    PROMPT_TEMPLATE_NOT_FOUND("PROMPT-404-01", "Prompt template not found", HttpStatus.NOT_FOUND),
    PROMPT_HISTORY_NOT_FOUND("PROMPT-404-02", "Prompt history not found", HttpStatus.NOT_FOUND),
    PROMPT_DRAFT_NOT_FOUND("PROMPT-404-03", "Prompt draft not found", HttpStatus.NOT_FOUND),
    PROMPT_LENGTH_INVALID("PROMPT-400-01", "Prompt length is invalid", HttpStatus.BAD_REQUEST),
    PROMPT_BRAND_CONTEXT_REQUIRED("PROMPT-400-02", "Brand context is required", HttpStatus.BAD_REQUEST),
    PROMPT_UNSUPPORTED_COMBINATION("PROMPT-400-03", "Prompt request contains an unsupported combination", HttpStatus.BAD_REQUEST),
    PROMPT_CONTEXT_INVALID("PROMPT-400-04", "Prompt context is invalid", HttpStatus.BAD_REQUEST),
    PROMPT_READINESS_BLOCKED("PROMPT-422-01", "Prompt is not ready for generation", HttpStatus.UNPROCESSABLE_ENTITY),
    PROMPT_AI_REQUEST_FAILED("PROMPT-502-01", "AI provider rejected the request", HttpStatus.BAD_GATEWAY),
    PROMPT_AI_RESPONSE_INVALID("PROMPT-502-02", "AI provider response is invalid", HttpStatus.BAD_GATEWAY),
    PROMPT_AI_PROVIDER_UNAVAILABLE("PROMPT-503-01", "AI provider is unavailable", HttpStatus.SERVICE_UNAVAILABLE),
    PROMPT_RATE_LIMITED("PROMPT-429-01", "Prompt requests are temporarily rate limited", HttpStatus.TOO_MANY_REQUESTS),
    PROMPT_RATE_LIMITER_UNAVAILABLE("PROMPT-503-02", "Prompt rate limiting is temporarily unavailable", HttpStatus.SERVICE_UNAVAILABLE),
    GENERATION_REQUEST_NOT_FOUND("GENERATION-404-01", "Creative generation request not found", HttpStatus.NOT_FOUND),
    GENERATION_JOB_NOT_FOUND("GENERATION-404-02", "Generation job not found", HttpStatus.NOT_FOUND),
    CREATIVE_OUTPUT_NOT_FOUND("GENERATION-404-03", "Creative output not found", HttpStatus.NOT_FOUND),
    GENERATION_VALIDATION_FAILED("GENERATION-400-01", "Creative generation request is invalid", HttpStatus.BAD_REQUEST),
    GENERATION_CONTEXT_INVALID("GENERATION-400-02", "Creative generation context is invalid", HttpStatus.BAD_REQUEST),
    GENERATION_UNSUPPORTED_FORMAT("GENERATION-400-03", "Creative output format is not supported for this creative type", HttpStatus.BAD_REQUEST),
    GENERATION_READINESS_BLOCKED("GENERATION-422-01", "Creative request is not ready for generation", HttpStatus.UNPROCESSABLE_ENTITY),
    GENERATION_STATE_CONFLICT("GENERATION-409-01", "Creative generation request is not in a valid state for this operation", HttpStatus.CONFLICT),
    GENERATION_PROVIDER_REQUEST_FAILED("GENERATION-502-01", "Creative AI provider rejected the request", HttpStatus.BAD_GATEWAY),
    GENERATION_PROVIDER_RESPONSE_INVALID("GENERATION-502-02", "Creative AI provider response is invalid", HttpStatus.BAD_GATEWAY),
    GENERATION_PROVIDER_UNAVAILABLE("GENERATION-503-01", "Creative AI provider is unavailable", HttpStatus.SERVICE_UNAVAILABLE),
    GENERATION_RATE_LIMITED("GENERATION-429-01", "Creative generation requests are temporarily rate limited", HttpStatus.TOO_MANY_REQUESTS),
    GENERATION_RATE_LIMITER_UNAVAILABLE("GENERATION-503-02", "Creative generation rate limiting is temporarily unavailable", HttpStatus.SERVICE_UNAVAILABLE),
    AI_ROUTING_POLICY_INVALID("AI-422-01", "AI routing policy is not valid", HttpStatus.UNPROCESSABLE_ENTITY),
    CREATIVE_APPROVAL_NOT_FOUND("APPROVAL-404-01", "Creative approval not found", HttpStatus.NOT_FOUND),
    CREATIVE_APPROVAL_DUPLICATE("APPROVAL-409-01", "An active approval already exists for this creative output", HttpStatus.CONFLICT),
    CREATIVE_APPROVAL_INVALID_TRANSITION("APPROVAL-409-02", "Creative approval status transition is not allowed", HttpStatus.CONFLICT),
    CREATIVE_APPROVAL_REVIEW_IN_PROGRESS("APPROVAL-409-03", "Another approval review submission is already in progress", HttpStatus.CONFLICT),
    CREATIVE_APPROVAL_REASON_REQUIRED("APPROVAL-400-01", "Approval review reason is required", HttpStatus.BAD_REQUEST),
    CREATIVE_REVIEW_COMMENT_INVALID("APPROVAL-400-02", "Creative review comment is invalid", HttpStatus.BAD_REQUEST),
    KAFKA_PUBLISH_FAILED("KAFKA-503-01", "Kafka publish failed", HttpStatus.SERVICE_UNAVAILABLE),
    REDIS_OPERATION_FAILED("REDIS-503-01", "Redis operation failed", HttpStatus.SERVICE_UNAVAILABLE),
    GATEWAY_ROUTE_NOT_FOUND("GATEWAY-404-01", "No gateway route matched the request", HttpStatus.NOT_FOUND),
    GATEWAY_UPSTREAM_UNAVAILABLE("GATEWAY-503-01", "Requested service is unavailable", HttpStatus.SERVICE_UNAVAILABLE),
    GATEWAY_UPSTREAM_TIMEOUT("GATEWAY-504-01", "Requested service did not respond in time", HttpStatus.GATEWAY_TIMEOUT),
    INTERNAL_ERROR("COMMON-500", "Unexpected server error", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String code;
    private final String defaultMessage;
    private final HttpStatus httpStatus;

    ErrorCode(String code, String defaultMessage, HttpStatus httpStatus) {
        this.code = code;
        this.defaultMessage = defaultMessage;
        this.httpStatus = httpStatus;
    }

    public String code() {
        return code;
    }

    public String defaultMessage() {
        return defaultMessage;
    }

    public HttpStatus httpStatus() {
        return httpStatus;
    }
}
