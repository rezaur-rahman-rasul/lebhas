package com.lebhas.creativesaas.creativerequest.application;

import com.lebhas.ai.job.AiJobStateCacheEntry;
import com.lebhas.creativesaas.creativerequest.application.dto.CreativeRequestJobView;
import com.lebhas.creativesaas.creativerequest.application.dto.CreativeRequestResponse;
import com.lebhas.creativesaas.creativerequest.application.dto.CreativeRequestView;
import com.lebhas.creativesaas.creativerequest.cache.dto.CreativeRequestCacheEntry;
import com.lebhas.creativesaas.creativerequest.domain.CreativeRequestEntity;
import com.lebhas.creativesaas.generatedversion.application.GeneratedVersionViewMapper;
import com.lebhas.creativesaas.generatedversion.application.dto.GeneratedVersionView;
import com.lebhas.creativesaas.generatedversion.domain.GeneratedVersionEntity;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class CreativeRequestMapper {

    private final CreativeRequestViewMapper creativeRequestViewMapper;
    private final GeneratedVersionViewMapper generatedVersionViewMapper;

    public CreativeRequestMapper(
            CreativeRequestViewMapper creativeRequestViewMapper,
            GeneratedVersionViewMapper generatedVersionViewMapper
    ) {
        this.creativeRequestViewMapper = creativeRequestViewMapper;
        this.generatedVersionViewMapper = generatedVersionViewMapper;
    }

    public CreativeRequestResponse toResponse(
            CreativeRequestEntity request,
            List<GeneratedVersionEntity> versions,
            AiJobStateCacheEntry jobState,
            BigDecimal estimatedCreditCost
    ) {
        CreativeRequestView requestView = creativeRequestViewMapper.toView(request);
        List<GeneratedVersionView> versionViews = versions.stream()
                .map(generatedVersionViewMapper::toView)
                .toList();
        GeneratedVersionView latestVersion = versionViews.isEmpty() ? null : versionViews.get(0);
        CreativeRequestJobView jobView = jobState == null
                ? null
                : new CreativeRequestJobView(
                        jobState.jobId(),
                        jobState.providerType(),
                        jobState.model(),
                        jobState.state(),
                        jobState.attempt(),
                        jobState.providerJobId(),
                        jobState.message(),
                        jobState.updatedAt());
        return new CreativeRequestResponse(
                requestView,
                latestVersion,
                versionViews,
                jobView,
                estimatedCreditCost);
    }

    public CreativeRequestResponse toResponse(
            CreativeRequestCacheEntry request,
            List<GeneratedVersionEntity> versions,
            AiJobStateCacheEntry jobState,
            BigDecimal estimatedCreditCost
    ) {
        CreativeRequestView requestView = creativeRequestViewMapper.toView(request);
        List<GeneratedVersionView> versionViews = versions.stream()
                .map(generatedVersionViewMapper::toView)
                .toList();
        GeneratedVersionView latestVersion = versionViews.isEmpty() ? null : versionViews.get(0);
        CreativeRequestJobView jobView = jobState == null
                ? null
                : new CreativeRequestJobView(
                        jobState.jobId(),
                        jobState.providerType(),
                        jobState.model(),
                        jobState.state(),
                        jobState.attempt(),
                        jobState.providerJobId(),
                        jobState.message(),
                        jobState.updatedAt());
        return new CreativeRequestResponse(
                requestView,
                latestVersion,
                versionViews,
                jobView,
                estimatedCreditCost);
    }
}
