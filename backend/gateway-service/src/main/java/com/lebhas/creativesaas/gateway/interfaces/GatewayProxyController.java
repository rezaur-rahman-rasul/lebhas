package com.lebhas.creativesaas.gateway.interfaces;

import com.lebhas.creativesaas.common.api.ApiError;
import com.lebhas.creativesaas.common.api.ApiResponse;
import com.lebhas.creativesaas.common.constants.CommonHeaders;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.gateway.config.GatewayRoutingProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerMapping;

import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.time.Instant;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import tools.jackson.databind.ObjectMapper;

@RestController
public class GatewayProxyController {

    private static final Logger log = LoggerFactory.getLogger(GatewayProxyController.class);
    private static final Set<String> EXCLUDED_REQUEST_HEADERS = Set.of(
            HttpHeaders.HOST.toLowerCase(),
            HttpHeaders.CONTENT_LENGTH.toLowerCase(),
            HttpHeaders.CONNECTION.toLowerCase(),
            HttpHeaders.EXPECT.toLowerCase(),
            HttpHeaders.UPGRADE.toLowerCase());
    private static final Set<String> EXCLUDED_RESPONSE_HEADERS = Set.of(
            HttpHeaders.CONNECTION.toLowerCase(),
            HttpHeaders.TRANSFER_ENCODING.toLowerCase(),
            "keep-alive");
    private static final org.springframework.util.AntPathMatcher PATH_MATCHER = new org.springframework.util.AntPathMatcher();

    private final HttpClient httpClient;
    private final GatewayRoutingProperties properties;
    private final ObjectMapper objectMapper;

    public GatewayProxyController(
            HttpClient httpClient,
            GatewayRoutingProperties properties,
            ObjectMapper objectMapper
    ) {
        this.httpClient = httpClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @RequestMapping({
            "/api/v1/auth/**",
            "/api/v1/credit-packages/**",
            "/api/v1/master/**",
            "/api/v1/payments/**",
            "/api/v1/workspaces/**",
            "/internal/storage/local/assets/**"
    })
    public void proxy(HttpServletRequest request, HttpServletResponse response) throws IOException {
        GatewayRoute route = resolveRoute(request.getRequestURI())
                .orElse(null);
        if (route == null) {
            writeGatewayError(response, HttpStatus.NOT_FOUND, ErrorCode.GATEWAY_ROUTE_NOT_FOUND);
            return;
        }

        Instant startedAt = Instant.now();
        String correlationId = Optional.ofNullable(request.getHeader(CommonHeaders.CORRELATION_ID))
                .filter(value -> !value.isBlank())
                .orElseGet(() -> Optional.ofNullable(response.getHeader(CommonHeaders.CORRELATION_ID))
                        .filter(value -> !value.isBlank())
                        .orElse(UUID.randomUUID().toString()));
        response.setHeader(CommonHeaders.CORRELATION_ID, correlationId);

        HttpRequest outboundRequest = buildOutboundRequest(request, route, correlationId);
        try {
            HttpResponse<InputStream> downstreamResponse = httpClient.send(outboundRequest, HttpResponse.BodyHandlers.ofInputStream());
            copyResponse(downstreamResponse, response, correlationId);
            log.info(
                    "gateway_proxy routeId={} method={} path={} target={} status={} durationMs={}",
                    route.id(),
                    request.getMethod(),
                    request.getRequestURI(),
                    route.baseUrl(),
                    downstreamResponse.statusCode(),
                    Duration.between(startedAt, Instant.now()).toMillis());
        } catch (HttpTimeoutException exception) {
            log.warn("Gateway timeout routeId={} path={} target={}", route.id(), request.getRequestURI(), route.baseUrl());
            writeGatewayError(response, HttpStatus.GATEWAY_TIMEOUT, ErrorCode.GATEWAY_UPSTREAM_TIMEOUT);
        } catch (ConnectException exception) {
            log.warn("Gateway upstream unavailable routeId={} path={} target={}", route.id(), request.getRequestURI(), route.baseUrl());
            writeGatewayError(response, HttpStatus.SERVICE_UNAVAILABLE, ErrorCode.GATEWAY_UPSTREAM_UNAVAILABLE);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            writeGatewayError(response, HttpStatus.SERVICE_UNAVAILABLE, ErrorCode.GATEWAY_UPSTREAM_UNAVAILABLE);
        } catch (IOException exception) {
            log.warn("Gateway proxy I/O failure routeId={} path={} target={} message={}", route.id(), request.getRequestURI(), route.baseUrl(), exception.getMessage());
            writeGatewayError(response, HttpStatus.SERVICE_UNAVAILABLE, ErrorCode.GATEWAY_UPSTREAM_UNAVAILABLE);
        }
    }

    private Optional<GatewayRoute> resolveRoute(String requestUri) {
        List<GatewayRoute> routes = List.of(
                new GatewayRoute("creative-assets", properties.getServices().getCreative(), List.of(
                        "/api/v1/workspaces/*/assets/**",
                        "/api/v1/workspaces/*/asset-folders/**",
                        "/api/v1/workspaces/*/projects/**",
                        "/api/v1/workspaces/*/storage-files/**",
                        "/api/v1/workspaces/*/creative-generations/**",
                        "/api/v1/workspaces/*/creative-outputs/**",
                        "/api/v1/workspaces/*/creative-approvals/**",
                        "/internal/storage/local/assets/**")),
                new GatewayRoute("billing", properties.getServices().getBilling(), List.of(
                        "/api/v1/credit-packages/**",
                        "/api/v1/master/payment-providers/**",
                        "/api/v1/master/payment-provider-configurations/**",
                        "/api/v1/master/credit-packages/**",
                        "/api/v1/master/pricing-plans/**",
                        "/api/v1/master/workspace-subscriptions/**",
                        "/api/v1/payments/**",
                        "/api/v1/workspaces/*/payments/**",
                        "/api/v1/workspaces/*/invoices/**",
                        "/api/v1/workspaces/*/credits/**",
                        "/api/v1/workspaces/*/subscriptions/**",
                        "/api/v1/workspaces/*/usage-billing/**")),
                new GatewayRoute("notification", properties.getServices().getNotification(), List.of(
                        "/api/v1/master/monitoring/**",
                        "/api/v1/workspaces/*/notifications/**",
                        "/api/v1/workspaces/*/notification-preferences/**",
                        "/api/v1/workspaces/*/activity-feed/**",
                        "/api/v1/workspaces/*/audit-logs/**",
                        "/api/v1/workspaces/*/timeline/**")),
                new GatewayRoute("auth", properties.getServices().getAuth(), List.of("/api/v1/auth/**")),
                new GatewayRoute("workspace", properties.getServices().getWorkspace(), List.of("/api/v1/workspaces/**")));

        return routes.stream()
                .filter(route -> route.pathPatterns().stream().anyMatch(pattern -> PATH_MATCHER.match(pattern, requestUri)))
                .findFirst();
    }

    private HttpRequest buildOutboundRequest(HttpServletRequest request, GatewayRoute route, String correlationId) throws IOException {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(resolveTargetUri(request, route.baseUrl()))
                .timeout(properties.getReadTimeout())
                .method(request.getMethod(), createBodyPublisher(request));

        copyRequestHeaders(request, builder);
        appendForwardingHeaders(request, builder, correlationId);
        return builder.build();
    }

    private HttpRequest.BodyPublisher createBodyPublisher(HttpServletRequest request) {
        boolean hasBody = request.getContentLengthLong() > 0
                || request.getHeader(HttpHeaders.TRANSFER_ENCODING) != null
                || Set.of("POST", "PUT", "PATCH").contains(request.getMethod());
        return hasBody
                ? HttpRequest.BodyPublishers.ofInputStream(() -> {
                    try {
                        return request.getInputStream();
                    } catch (IOException exception) {
                        throw new IllegalStateException("Request body could not be read", exception);
                    }
                })
                : HttpRequest.BodyPublishers.noBody();
    }

    private URI resolveTargetUri(HttpServletRequest request, URI baseUrl) {
        String queryString = request.getQueryString();
        String path = request.getRequestURI();
        if (queryString != null && !queryString.isBlank()) {
            path = path + "?" + queryString;
        }
        return baseUrl.resolve(path);
    }

    private void copyRequestHeaders(HttpServletRequest request, HttpRequest.Builder builder) {
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            if (EXCLUDED_REQUEST_HEADERS.contains(headerName.toLowerCase())) {
                continue;
            }
            Enumeration<String> values = request.getHeaders(headerName);
            while (values.hasMoreElements()) {
                builder.header(headerName, values.nextElement());
            }
        }
    }

    private void appendForwardingHeaders(HttpServletRequest request, HttpRequest.Builder builder, String correlationId) {
        builder.setHeader(CommonHeaders.CORRELATION_ID, correlationId);
        builder.setHeader("X-Forwarded-For", forwardedFor(request));
        builder.setHeader("X-Forwarded-Host", request.getHeader(HttpHeaders.HOST));
        builder.setHeader("X-Forwarded-Proto", request.getScheme());
        builder.setHeader("X-Forwarded-Prefix", request.getContextPath() == null ? "" : request.getContextPath());
        Object bestPattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        if (bestPattern != null) {
            builder.setHeader("X-Gateway-Route-Pattern", String.valueOf(bestPattern));
        }
    }

    private String forwardedFor(HttpServletRequest request) {
        String existing = request.getHeader("X-Forwarded-For");
        String remoteAddress = request.getRemoteAddr();
        if (existing == null || existing.isBlank()) {
            return remoteAddress;
        }
        return existing + ", " + remoteAddress;
    }

    private void copyResponse(HttpResponse<InputStream> downstreamResponse, HttpServletResponse response, String correlationId) throws IOException {
        response.setStatus(downstreamResponse.statusCode());
        response.setHeader(CommonHeaders.CORRELATION_ID, correlationId);
        for (Map.Entry<String, List<String>> header : downstreamResponse.headers().map().entrySet()) {
            if (EXCLUDED_RESPONSE_HEADERS.contains(header.getKey().toLowerCase())) {
                continue;
            }
            for (String value : header.getValue()) {
                response.addHeader(header.getKey(), value);
            }
        }
        try (InputStream bodyStream = downstreamResponse.body()) {
            bodyStream.transferTo(response.getOutputStream());
        }
    }

    private void writeGatewayError(HttpServletResponse response, HttpStatus status, ErrorCode errorCode) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(java.nio.charset.StandardCharsets.UTF_8.name());
        objectMapper.writeValue(
                response.getOutputStream(),
                ApiResponse.failure(errorCode.defaultMessage(), ApiError.of(errorCode.code(), errorCode.defaultMessage())));
    }

    private record GatewayRoute(String id, URI baseUrl, List<String> pathPatterns) {
    }
}
