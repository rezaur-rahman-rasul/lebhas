package com.lebhas.creativesaas.operations.web;

import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.operations.application.SystemFeatureToggleService;
import com.lebhas.creativesaas.operations.domain.SystemFeatureToggleKey;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Set;

@Component
public class MaintenanceModeInterceptor implements HandlerInterceptor {
    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS");
    private final SystemFeatureToggleService toggleService;

    public MaintenanceModeInterceptor(SystemFeatureToggleService toggleService) {
        this.toggleService = toggleService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String path = request.getRequestURI();
        if (SAFE_METHODS.contains(request.getMethod()) || path.startsWith("/health") || path.startsWith("/api/v1/system") || path.startsWith("/api/v1/master/operations/maintenance-mode")) {
            return true;
        }
        if (toggleService.isEnabled(SystemFeatureToggleKey.MAINTENANCE_MODE) && !path.startsWith("/api/v1/master")) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "System is in maintenance mode");
        }
        return true;
    }
}
