package com.lebhas.creativesaas.prompt.cache;

import java.util.UUID;

public record PromptRedisOperationContext(
        UUID workspaceId,
        UUID creativeRequestId,
        UUID promptTemplateId,
        UUID pricingPlanId,
        String promptHash
) {

    public static PromptRedisOperationContext creativeRequest(UUID workspaceId, UUID creativeRequestId) {
        return new PromptRedisOperationContext(workspaceId, creativeRequestId, null, null, null);
    }

    public static PromptRedisOperationContext promptHash(String promptHash) {
        return new PromptRedisOperationContext(null, null, null, null, promptHash);
    }

    public static PromptRedisOperationContext promptTemplate(UUID workspaceId, UUID promptTemplateId) {
        return new PromptRedisOperationContext(workspaceId, null, promptTemplateId, null, null);
    }

    public static PromptRedisOperationContext generationQuota(UUID workspaceId, UUID pricingPlanId) {
        return new PromptRedisOperationContext(workspaceId, null, null, pricingPlanId, null);
    }
}
