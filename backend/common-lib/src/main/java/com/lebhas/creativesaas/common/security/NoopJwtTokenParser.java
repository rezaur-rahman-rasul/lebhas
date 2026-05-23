package com.lebhas.creativesaas.common.security;

import java.util.Optional;

public class NoopJwtTokenParser implements JwtTokenParser {

    @Override
    public Optional<AuthenticatedPrincipal> parse(String token) {
        return Optional.empty();
    }
}
