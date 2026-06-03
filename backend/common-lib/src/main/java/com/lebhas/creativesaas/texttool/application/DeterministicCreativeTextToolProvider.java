package com.lebhas.creativesaas.texttool.application;

import com.lebhas.creativesaas.texttool.domain.CreativeTextToolType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class DeterministicCreativeTextToolProvider implements CreativeTextToolProvider {

    @Override
    public Map<String, Object> generate(TextToolGenerationContext context) {
        String brandName = context.brand().getName();
        String productName = context.productService() == null ? "featured offer" : context.productService().getName();
        String platform = context.request().platform().name().toLowerCase();
        String tone = context.request().tone() == null ? "clear" : context.request().tone();
        String idea = context.request().sourceIdea() == null ? context.project().getName() : context.request().sourceIdea();
        String cta = context.brand().getPreferredCta() == null ? "Shop now" : context.brand().getPreferredCta();
        String prefix = "%s %s %s".formatted(brandName, productName, platform);

        if (context.toolType() == CreativeTextToolType.POST) {
            return Map.of(
                    "postText", "%s: %s. A %s message crafted for %s.".formatted(brandName, idea, tone, productName),
                    "shortHeadline", "%s for %s".formatted(productName, platform),
                    "cta", cta);
        }
        if (context.toolType() == CreativeTextToolType.CAPTION) {
            return Map.of(
                    "captionText", "%s brings %s to your next %s moment.".formatted(brandName, productName, platform),
                    "alternativeCaptions", List.of(
                            "%s, made for %s.".formatted(productName, platform),
                            "%s with a %s voice.".formatted(productName, tone)));
        }
        if (context.toolType() == CreativeTextToolType.ADS_COPY) {
            return Map.of(
                    "primaryText", "%s solves the moment with %s. %s".formatted(brandName, productName, idea),
                    "headline", "%s by %s".formatted(productName, brandName),
                    "description", "Deterministic %s ad copy for %s.".formatted(tone, platform),
                    "cta", cta,
                    "platformNotes", "Optimized for %s placement and concise scanning.".formatted(platform));
        }
        return Map.of(
                "hashtags", List.of(
                        hashtag(brandName),
                        hashtag(productName),
                        hashtag(platform),
                        hashtag(context.project().getName())),
                "categoryGroups", Map.of(
                        "brand", List.of(hashtag(brandName), hashtag(prefix)),
                        "campaign", List.of(hashtag(idea), hashtag(platform))));
    }

    private String hashtag(String value) {
        String normalized = value == null ? "lebhas" : value.replaceAll("[^A-Za-z0-9]", "");
        if (normalized.isBlank()) {
            normalized = "lebhas";
        }
        return "#" + normalized;
    }
}
