package com.lebhas.creativesaas.common.validation.color;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class HexColorValidator implements ConstraintValidator<HexColor, String> {

    private static final Pattern HEX_COLOR = Pattern.compile("^#(?:[0-9a-fA-F]{6})$");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value == null || value.isBlank() || HEX_COLOR.matcher(value.trim()).matches();
    }
}
