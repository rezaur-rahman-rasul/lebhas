package com.lebhas.creativesaas.creative.interfaces;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "Optional query parameters used when resolving a public share link.")
public class ResolveShareLinkRequest {

    @Size(max = 255)
    @Schema(description = "Password required for password-protected links", maxLength = 255)
    private String password;

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
