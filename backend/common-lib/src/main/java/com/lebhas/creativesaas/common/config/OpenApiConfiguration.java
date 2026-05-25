package com.lebhas.creativesaas.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {

    private static final String BEARER_AUTH = "bearerAuth";

    @Bean
    OpenAPI platformOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("AI Creative SaaS Platform API")
                        .version("0.1.0")
                        .description("Backend foundation for the Bangladesh creative SaaS platform"))
                .components(new Components().addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
    }

    @Bean
    GroupedOpenApi foundationApiGroup() {
        return GroupedOpenApi.builder()
                .group("foundation")
                .pathsToMatch("/health", "/liveness", "/readiness", "/actuator/**")
                .build();
    }

    @Bean
    GroupedOpenApi authApiGroup() {
        return GroupedOpenApi.builder()
                .group("auth")
                .pathsToMatch("/api/v1/auth/**")
                .build();
    }

    @Bean
    GroupedOpenApi userApiGroup() {
        return GroupedOpenApi.builder()
                .group("users")
                .pathsToMatch("/api/v1/users/**")
                .build();
    }

    @Bean
    GroupedOpenApi workspaceApiGroup() {
        return GroupedOpenApi.builder()
                .group("workspaces")
                .pathsToMatch("/api/v1/workspaces/**")
                .build();
    }

    @Bean
    GroupedOpenApi promptApiGroup() {
        return GroupedOpenApi.builder()
                .group("prompts")
                .pathsToMatch(
                        "/api/v1/workspaces/*/prompt-templates/**",
                        "/api/v1/workspaces/*/prompts/**",
                        "/api/v1/workspaces/*/prompt-history/**")
                .build();
    }

    @Bean
    GroupedOpenApi creativeGenerationApiGroup() {
        return GroupedOpenApi.builder()
                .group("creative-generation")
                .pathsToMatch(
                        "/api/v1/workspaces/*/creative-generations/**",
                        "/api/v1/workspaces/*/creative-outputs/**")
                .build();
    }

    @Bean
    GroupedOpenApi creativeApprovalApiGroup() {
        return GroupedOpenApi.builder()
                .group("approvals")
                .pathsToMatch(
                        "/api/v1/workspaces/*/approval-requests/**",
                        "/api/v1/workspaces/*/generated-versions/*/submit-approval")
                .build();
    }

    @Bean
    GroupedOpenApi pricingApiGroup() {
        return GroupedOpenApi.builder()
                .group("pricing")
                .pathsToMatch(
                        "/api/v1/master/pricing-plans/**",
                        "/api/v1/master/workspaces/*/subscription",
                        "/api/v1/pricing-plans/public")
                .build();
    }

    @Bean
    GroupedOpenApi paymentProviderApiGroup() {
        return GroupedOpenApi.builder()
                .group("master-payment-providers")
                .pathsToMatch(
                        "/api/v1/master/payment-providers/**",
                        "/api/v1/master/payment-provider-configurations/**")
                .build();
    }

    @Bean
    GroupedOpenApi creditPackageApiGroup() {
        return GroupedOpenApi.builder()
                .group("credit-packages")
                .pathsToMatch(
                        "/api/v1/master/credit-packages/**",
                        "/api/v1/credit-packages/**")
                .build();
    }

    @Bean
    GroupedOpenApi masterAiPipelineApiGroup() {
        return GroupedOpenApi.builder()
                .group("master-ai-pipelines")
                .pathsToMatch("/api/v1/master/creative-pipelines/**")
                .build();
    }
}
