package com.lebhas.creativesaas.common.security;

import java.util.Optional;

public interface JwtTokenParser {

    Optional<AuthenticatedPrincipal> parse(String token);
}
