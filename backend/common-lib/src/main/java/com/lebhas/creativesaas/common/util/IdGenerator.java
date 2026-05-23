package com.lebhas.creativesaas.common.util;

import java.util.UUID;

public final class IdGenerator {

    private IdGenerator() {
    }

    public static UUID uuid() {
        return UUID.randomUUID();
    }
}
