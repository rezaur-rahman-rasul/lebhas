package com.lebhas.creativesaas.user.interfaces;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ProfileEmailRequest(@NotBlank @Email String email) {
}
