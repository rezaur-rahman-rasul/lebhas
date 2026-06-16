package com.lebhas.ai.application;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class BanglaTypographyQualityService {

    private static final Pattern BANGLA_BLOCK = Pattern.compile("[\\u0980-\\u09FF]");
    private static final Pattern LATIN_FAKE_BANGLA_MIX = Pattern.compile("[\\u0980-\\u09FF].*[A-Za-z]|[A-Za-z].*[\\u0980-\\u09FF]");
    private static final Pattern DANGLING_HASANTA = Pattern.compile("\\u09CD(?![\\u0995-\\u09B9\\u09DC-\\u09DF])");
    private static final Pattern DOUBLE_SPACING = Pattern.compile("\\s{2,}");

    public BigDecimal score(BigDecimal measuredScore) {
        return QualityScoreCalculator.normalizeScore(measuredScore);
    }

    public boolean isBanglaLanguage(String language) {
        if (language == null) {
            return false;
        }
        String normalized = language.trim().toLowerCase();
        return normalized.equals("bn")
                || normalized.equals("bangla")
                || normalized.equals("bengali")
                || normalized.contains("বাংলা")
                || normalized.contains("bangla")
                || normalized.contains("bengali");
    }

    public boolean containsBangla(String value) {
        return value != null && BANGLA_BLOCK.matcher(value).find();
    }

    public String normalizeBangla(String value) {
        if (value == null) {
            return "";
        }
        String normalized = Normalizer.normalize(value.trim(), Normalizer.Form.NFC);
        return DOUBLE_SPACING.matcher(normalized).replaceAll(" ");
    }

    public TypographyValidation validateText(String value) {
        String normalized = normalizeBangla(value);
        List<String> failures = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        if (normalized.isBlank()) {
            return new TypographyValidation(true, normalized, failures, warnings);
        }
        if (containsBangla(normalized) && DANGLING_HASANTA.matcher(normalized).find()) {
            failures.add("Bangla hasanta is dangling; conjunct shaping would break.");
        }
        if (containsBangla(normalized) && LATIN_FAKE_BANGLA_MIX.matcher(normalized).find()) {
            warnings.add("Mixed Bangla/Latin text detected; render with mixed-language fallback fonts.");
        }
        if (!Normalizer.isNormalized(normalized, Normalizer.Form.NFC)) {
            failures.add("Bangla text is not NFC-normalized.");
        }
        return new TypographyValidation(failures.isEmpty(), normalized, failures, warnings);
    }

    public record TypographyValidation(
            boolean valid,
            String normalizedText,
            List<String> failures,
            List<String> warnings
    ) {
    }
}
