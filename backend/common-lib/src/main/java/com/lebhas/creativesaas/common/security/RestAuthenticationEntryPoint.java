package com.lebhas.creativesaas.common.security;

import tools.jackson.databind.ObjectMapper;
import com.lebhas.creativesaas.common.api.ApiError;
import com.lebhas.creativesaas.common.api.ApiResponse;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;
    private final SecurityAuditLogger securityAuditLogger;

    public RestAuthenticationEntryPoint(ObjectMapper objectMapper, SecurityAuditLogger securityAuditLogger) {
        this.objectMapper = objectMapper;
        this.securityAuditLogger = securityAuditLogger;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {
        securityAuditLogger.logSecurityException("authentication_failed", request.getRequestURI(), authException.getMessage());
        writeSecurityError(response, HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHORIZED);
    }

    private void writeSecurityError(HttpServletResponse response, HttpStatus status, ErrorCode errorCode) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), ApiResponse.failure(
                errorCode.defaultMessage(),
                ApiError.of(errorCode.code(), errorCode.defaultMessage())));
    }
}
