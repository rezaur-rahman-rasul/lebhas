package com.lebhas.creativesaas.user.interfaces;

import jakarta.validation.constraints.NotBlank;

public record SocialConnectionRequest(@NotBlank String profileUrl) {
}
