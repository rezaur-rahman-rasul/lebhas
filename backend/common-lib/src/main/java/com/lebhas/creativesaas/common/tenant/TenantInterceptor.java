package com.lebhas.creativesaas.common.tenant;

import com.lebhas.creativesaas.common.constants.CommonHeaders;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

@Component
public class TenantInterceptor implements HandlerInterceptor {

    private static final String MDC_WORKSPACE_ID = "workspaceId";

    private final TenantResolver tenantResolver;

    public TenantInterceptor(TenantResolver tenantResolver) {
        this.tenantResolver = tenantResolver;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        tenantResolver.resolve(request).ifPresent(this::setTenant);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception exception) {
        TenantContext.clear();
        MDC.remove(MDC_WORKSPACE_ID);
    }

    private void setTenant(UUID workspaceId) {
        TenantContext.setWorkspaceId(workspaceId);
        MDC.put(MDC_WORKSPACE_ID, workspaceId.toString());
    }

    public static String tenantHeaderName() {
        return CommonHeaders.WORKSPACE_ID;
    }
}
