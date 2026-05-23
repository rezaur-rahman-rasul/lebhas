package com.lebhas.creativesaas.creative.interfaces;

import com.lebhas.creativesaas.approval.domain.CreativeReviewCommentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddCreativeReviewCommentRequest(
        @NotBlank
        @Size(min = 2, max = 2000)
        String comment,
        CreativeReviewCommentType commentType
) {
}
