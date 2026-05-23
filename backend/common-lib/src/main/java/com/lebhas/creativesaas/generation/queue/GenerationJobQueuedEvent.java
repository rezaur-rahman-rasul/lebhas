package com.lebhas.creativesaas.generation.queue;

import java.util.UUID;

public record GenerationJobQueuedEvent(
        UUID jobId,
        UUID workspaceId,
        UUID requestId,
        String queueName
) {
}
