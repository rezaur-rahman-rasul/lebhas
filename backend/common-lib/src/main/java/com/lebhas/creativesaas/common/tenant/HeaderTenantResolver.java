package com.lebhas.creativesaas.common.tenant;

import com.lebhas.creativesaas.common.constants.CommonHeaders;
import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Optional;
import java.util.UUID;

@Component
public class HeaderTenantResolver implements TenantResolver {

    @Override
    public Optional<UUID> resolve(HttpServletRequest request) {
        String headerValue = request.getHeader(CommonHeaders.WORKSPACE_ID);
        if (!StringUtils.hasText(headerValue)) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(headerValue.trim()));
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.TENANT_HEADER_INVALID);
        }
    }
}
