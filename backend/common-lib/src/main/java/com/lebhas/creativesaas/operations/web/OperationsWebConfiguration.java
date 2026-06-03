package com.lebhas.creativesaas.operations.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class OperationsWebConfiguration implements WebMvcConfigurer {
    private final MaintenanceModeInterceptor maintenanceModeInterceptor;

    public OperationsWebConfiguration(MaintenanceModeInterceptor maintenanceModeInterceptor) {
        this.maintenanceModeInterceptor = maintenanceModeInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(maintenanceModeInterceptor);
    }
}
