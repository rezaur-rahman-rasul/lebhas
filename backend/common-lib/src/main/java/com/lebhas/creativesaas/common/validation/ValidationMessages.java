package com.lebhas.creativesaas.common.validation;

public final class ValidationMessages {

    public static final String REQUIRED = "must be provided";
    public static final String INVALID_UUID = "must be a valid UUID";
    public static final String INVALID_SLUG = "must contain only lowercase letters, numbers, and hyphens";
    public static final String INVALID_COLOR = "must be a valid hex color";
    public static final String INVALID_URL = "must be a valid http or https URL";
    public static final String INVALID_TIMEZONE = "must be a valid IANA timezone";

    private ValidationMessages() {
    }
}
