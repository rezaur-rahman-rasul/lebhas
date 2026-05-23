package com.lebhas.creativesaas.generation.application.dto;

import java.time.Instant;

public record CreativeOutputUrlView(
        String url,
        Instant expiresAt
) {
}
