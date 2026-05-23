package com.lebhas.creativesaas.common.security;

import tools.jackson.databind.ObjectMapper;
import com.lebhas.creativesaas.common.api.ApiError;
import com.lebhas.creativesaas.common.api.ApiResponse;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;
    private final SecurityAuditLogger securityAuditLogger;

    public RestAccessDeniedHandler(ObjectMapper objectMapper, SecurityAuditLogger securityAuditLogger) {
        this.objectMapper = objectMapper;
        this.securityAuditLogger = securityAuditLogger;
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException {
        securityAuditLogger.logSecurityException("access_denied", request.getRequestURI(), accessDeniedException.getMessage());
        writeSecurityError(response, HttpStatus.FORBIDDEN, ErrorCode.FORBIDDEN);
    }

    private void writeSecurityError(HttpServletResponse response, HttpStatus status, ErrorCode errorCode) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), ApiResponse.failure(
                errorCode.defaultMessage(),
                ApiError.of(errorCode.code(), errorCode.defaultMessage())));
    }
}
