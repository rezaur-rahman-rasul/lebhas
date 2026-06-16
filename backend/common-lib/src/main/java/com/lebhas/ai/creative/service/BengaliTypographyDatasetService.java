package com.lebhas.ai.creative.service;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class BengaliTypographyDatasetService {

    private static final List<FontProfile> FONT_PROFILES = List.of(
            new FontProfile("Hind Siliguri", List.of(400, 500, 600, 700), 95, 96, 94, 92, "modern-sans"),
            new FontProfile("Noto Sans Bengali", List.of(400, 500, 600, 700, 800), 94, 95, 96, 90, "neutral-sans"),
            new FontProfile("Noto Serif Bengali", List.of(400, 500, 600, 700), 90, 84, 95, 88, "premium-serif"),
            new FontProfile("SolaimanLipi", List.of(400, 700), 88, 86, 89, 82, "editorial"),
            new FontProfile("Siyam Rupali", List.of(400), 84, 78, 83, 76, "classic"),
            new FontProfile("Kalpurush", List.of(400), 82, 76, 82, 74, "classic"),
            new FontProfile("AdorshoLipi", List.of(400), 80, 75, 82, 74, "classic"),
            new FontProfile("Baloo Da 2", List.of(400, 500, 600, 700, 800), 86, 92, 84, 88, "friendly-rounded"),
            new FontProfile("Bangla MN", List.of(400, 700), 82, 72, 84, 70, "system-serif"),
            new FontProfile("Li Ador Noirrit", List.of(400, 700), 88, 82, 94, 87, "luxury-display"),
            new FontProfile("Nikosh", List.of(400, 700), 86, 82, 86, 78, "government-print"),
            new FontProfile("Mukti Narrow", List.of(400, 700), 82, 80, 80, 76, "compact"),
            new FontProfile("Ruposhi Bangla", List.of(400, 700), 83, 78, 86, 80, "display"),
            new FontProfile("Vrinda", List.of(400, 700), 80, 78, 78, 70, "windows-fallback")
    );

    private static final List<ConjunctExample> CONJUNCTS = List.of(
            c("ক্ত", "শক্তিশালী অফার", "premium headline, not cramped"),
            c("ক্ষ", "বিশেষ মূল্য", "offer badge and headline"),
            c("জ্ঞ", "বিজ্ঞাপন ডিজাইন", "headline and body copy"),
            c("ঙ্গ", "রঙিন সংগ্রহ", "product naming"),
            c("চ্ছ", "পছন্দের পণ্য", "headline"),
            c("দ্ব", "দ্বিগুণ সুবিধা", "offer copy"),
            c("ত্ত", "উত্তম মান", "quality claim"),
            c("শ্চ", "নিশ্চিত ডেলিভারি", "service promise"),
            c("স্ত", "সস্তায় নয়, সেরায়", "headline"),
            c("হ্ম", "ব্রহ্মপুত্র কালেকশন", "brand/product naming"),
            c("ম্প", "সম্পূর্ণ প্যাকেজ", "offer copy"),
            c("ন্ত্র", "নির্ভরযোগ্য মন্ত্র", "large heading stress test"),
            c("স্ম", "স্মার্ট পছন্দ", "CTA-adjacent headline"),
            c("ল্ল", "ভালোবাসার কালেকশন", "headline"),
            c("জ্জ", "সজ্জিত করুন", "CTA"),
            c("ঙ্ক", "আকর্ষণীয় প্যাক", "offer"),
            c("ঞ্চ", "পঞ্চাশ শতাংশ ছাড়", "price layout"),
            c("শ্র", "শ্রেষ্ঠ মান", "premium headline"),
            c("ক্র", "ক্রয় করুন", "CTA"),
            c("গ্র", "গ্রাহকের পছন্দ", "trust copy"),
            c("প্র", "প্রযুক্তির পরীক্ষা", "font shaping test"),
            c("ভ্র", "ভ্রমণের সঙ্গী", "lifestyle copy"),
            c("ত্র", "ত্রুটিমুক্ত ডিজাইন", "quality copy"),
            c("দ্ম", "পদ্মা কালেকশন", "brand/product naming"),
            c("গ্ধ", "মুগ্ধ করা স্বাদ", "FMCG headline"),
            c("ল্ক", "হাল্কা আরাম", "fashion copy"),
            c("ব্ধ", "উপলব্ধ আজ", "availability copy"),
            c("স্ফ", "স্ফূর্তির শুরু", "headline")
    );

    private static final List<String> MARKETING_COPY = List.of(
            "এখনই কিনুন",
            "সীমিত অফার",
            "আজকের বিশেষ মূল্য",
            "সরাসরি অর্ডার করুন",
            "ফ্রি ডেলিভারি",
            "বিশুদ্ধ আম",
            "গরমে প্রশান্তি",
            "স্বাদের সেরা আম",
            "আজই সংগ্রহ করুন",
            "নতুন আগমন",
            "বিশেষ ছাড়",
            "অর্ডার করুন আজই"
    );

    public List<FontProfile> fontProfiles() {
        return FONT_PROFILES;
    }

    public List<ConjunctExample> conjunctExamples() {
        return CONJUNCTS;
    }

    public List<String> marketingCopy() {
        return MARKETING_COPY;
    }

    public Map<String, Object> fewShotPromptPolicy() {
        return Map.of(
                "imageModelRole", "composition-only",
                "forbidden", List.of("rasterized Bengali text", "fake Bengali glyphs", "malformed conjuncts"),
                "required", List.of("empty premium text-safe regions", "backend rendered Unicode text", "optional CTA-safe empty region"),
                "examplePolicy", "Use request-provided copy only; never invent visible Bengali marketing text"
        );
    }

    public FontProfile bestFontFor(TextRole role) {
        return FONT_PROFILES.stream()
                .max((left, right) -> Integer.compare(score(left, role), score(right, role)))
                .orElse(FONT_PROFILES.getFirst());
    }

    private int score(FontProfile profile, TextRole role) {
        return switch (role) {
            case HEADLINE -> profile.family().equals("Noto Sans Bengali")
                    ? 10_000
                    : profile.headingSuitability() + profile.premiumScore() + profile.modernityScore();
            case OFFER -> profile.family().equals("Noto Sans Bengali")
                    ? 10_000
                    : profile.headingSuitability() + profile.premiumScore() + profile.readability();
            case CTA -> profile.ctaSuitability() + profile.readability() + profile.modernityScore();
            case BODY -> profile.readability() + profile.premiumScore();
        };
    }

    private static ConjunctExample c(String conjunct, String wordUsage, String adUsage) {
        return new ConjunctExample(conjunct, wordUsage, adUsage);
    }

    public enum TextRole {
        HEADLINE,
        OFFER,
        CTA,
        BODY
    }

    public record FontProfile(
            String family,
            List<Integer> weights,
            int readability,
            int ctaSuitability,
            int headingSuitability,
            int premiumScore,
            String style
    ) {
        public int modernityScore() {
            return switch (style) {
                case "modern-sans", "neutral-sans", "friendly-rounded", "compact" -> 92;
                case "luxury-display", "premium-serif", "display" -> 86;
                case "editorial", "government-print" -> 80;
                default -> 74;
            };
        }
    }

    public record ConjunctExample(String conjunct, String wordUsage, String adTypographyUsage) {
    }
}
