package com.lebhas.creativesaas.creative.interfaces;

import com.lebhas.creativesaas.download.application.dto.DownloadRequestContext;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class DownloadRequestContextFactory {

    public DownloadRequestContext create(HttpServletRequest request, String downloadSource) {
        return new DownloadRequestContext(
                downloadSource,
                resolveClientIp(request),
                request == null ? null : request.getHeader("User-Agent"));
    }

    private String resolveClientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwardedFor)) {
            int separator = forwardedFor.indexOf(',');
            return (separator >= 0 ? forwardedFor.substring(0, separator) : forwardedFor).trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(realIp)) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }
}
