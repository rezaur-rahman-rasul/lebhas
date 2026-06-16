package com.lebhas.ai.creative.service;

import com.lebhas.ai.application.BanglaTypographyQualityService;
import com.lebhas.ai.creative.dto.AiCreativeGenerateRequest;
import com.lebhas.ai.creative.enums.OutputFormat;
import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.font.TextAttribute;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.AttributedString;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class BengaliTypographyOverlayService {

    private final BanglaTypographyQualityService qualityService;
    private final BengaliTypographyDatasetService datasetService;

    public BengaliTypographyOverlayService(
            BanglaTypographyQualityService qualityService,
            BengaliTypographyDatasetService datasetService
    ) {
        this.qualityService = qualityService;
        this.datasetService = datasetService;
    }

    public boolean requiresOverlay(AiCreativeGenerateRequest request) {
        if (request.includeTypography() != null && !request.includeTypography()) {
            return false;
        }
        return qualityService.isBanglaLanguage(request.language())
                || qualityService.containsBangla(request.headline())
                || qualityService.containsBangla(request.subheadline())
                || qualityService.containsBangla(request.offerText())
                || qualityService.containsBangla(request.cta())
                || hasText(request.headline())
                || hasText(request.subheadline())
                || hasText(request.offerText())
                || hasText(request.campaignIdea())
                || (request.includeCta() != null && request.includeCta() && hasText(request.cta()));
    }

    public RenderedTypography render(byte[] imageBytes, AiCreativeGenerateRequest request, OutputFormat requestedFormat) {
        if (!requiresOverlay(request)) {
            return new RenderedTypography(false, imageBytes, Map.of());
        }
        try {
            BufferedImage source = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (source == null) {
                throw new BusinessException(ErrorCode.GENERATION_PROVIDER_REQUEST_FAILED, "Generated image could not be read for Bangla typography overlay");
            }
            BufferedImage canvas = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = canvas.createGraphics();
            graphics.drawImage(source, 0, 0, null);
            configure(graphics);

            TypographyCopy copy = copy(request);
            validate(copy);
            List<Map<String, Object>> regions = drawTypography(graphics, canvas.getWidth(), canvas.getHeight(), copy);
            graphics.dispose();

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            String writerFormat = requestedFormat == OutputFormat.jpeg ? "jpg" : "png";
            if ("jpg".equals(writerFormat)) {
                BufferedImage rgb = new BufferedImage(canvas.getWidth(), canvas.getHeight(), BufferedImage.TYPE_INT_RGB);
                Graphics2D jpg = rgb.createGraphics();
                jpg.setColor(Color.WHITE);
                jpg.fillRect(0, 0, rgb.getWidth(), rgb.getHeight());
                jpg.drawImage(canvas, 0, 0, null);
                jpg.dispose();
                ImageIO.write(rgb, writerFormat, out);
            } else {
                ImageIO.write(canvas, writerFormat, out);
            }
            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("engine", "JAVA2D_UNICODE_TEXT_LAYOUT");
            metadata.put("unicodeNormalization", "NFC");
            metadata.put("shaping", "programmatic-overlay-opentype-font-required");
            metadata.put("fonts", Map.of(
                    "headline", datasetService.bestFontFor(BengaliTypographyDatasetService.TextRole.HEADLINE).family(),
                    "offer", datasetService.bestFontFor(BengaliTypographyDatasetService.TextRole.OFFER).family(),
                    "cta", datasetService.bestFontFor(BengaliTypographyDatasetService.TextRole.CTA).family(),
                    "body", datasetService.bestFontFor(BengaliTypographyDatasetService.TextRole.BODY).family()
            ));
            metadata.put("regions", regions);
            metadata.put("conjunctDatasetSize", datasetService.conjunctExamples().size());
            metadata.put("marketingCopyDatasetSize", datasetService.marketingCopy().size());
            return new RenderedTypography(true, out.toByteArray(), metadata);
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.GENERATION_PROVIDER_REQUEST_FAILED, "Bangla typography overlay failed");
        }
    }

    private TypographyCopy copy(AiCreativeGenerateRequest request) {
        if (qualityService.isBanglaLanguage(request.language())) {
            String headline = firstNonBlank(
                    banglaText(request.headline()),
                    banglaText(request.campaignIdea()),
                    banglaText(request.productDescription()));
            String subheadline = firstNonBlank(
                    banglaText(request.subheadline()),
                    banglaText(request.campaignObjective()),
                    banglaText(request.targetAudience()));
            String offer = banglaText(request.offerText());
            String cta = request.includeCta() != null && request.includeCta()
                    ? banglaText(request.cta())
                    : "";
            if (headline.isBlank() && subheadline.isBlank() && offer.isBlank() && cta.isBlank()) {
                return fallbackBanglaCopy(request);
            }
            return new TypographyCopy(
                    qualityService.normalizeBangla(headline),
                    qualityService.normalizeBangla(subheadline),
                    qualityService.normalizeBangla(offer),
                    qualityService.normalizeBangla(cta));
        }
        String headline = firstNonBlank(
                explicitTextLayer(request.headline()),
                shortText(request.campaignIdea(), 80),
                shortText(request.productDescription(), 80));
        String subheadline = firstNonBlank(
                explicitTextLayer(request.subheadline()),
                shortText(request.campaignObjective(), 120),
                shortText(request.targetAudience(), 120));
        String offer = explicitTextLayer(request.offerText());
        String cta = explicitTextLayer(request.cta());
        return new TypographyCopy(
                qualityService.normalizeBangla(headline),
                qualityService.normalizeBangla(subheadline),
                qualityService.normalizeBangla(offer),
                qualityService.normalizeBangla(cta));
    }

    private String banglaText(String value) {
        String cleaned = explicitTextLayer(value);
        return qualityService.containsBangla(cleaned) ? cleaned : "";
    }

    private TypographyCopy fallbackBanglaCopy(AiCreativeGenerateRequest request) {
        String source = firstNonBlank(
                explicitTextLayer(request.productDescription()),
                explicitTextLayer(request.campaignIdea()),
                explicitTextLayer(request.targetAudience()));
        String normalized = source.toLowerCase(Locale.ROOT);
        if (normalized.contains("mango")) {
            return new TypographyCopy("স্বাদের সেরা আম", "রাজশাহীর তাজা আম", "", fallbackCta(request));
        }
        if (normalized.contains("fashion") || normalized.contains("attire") || normalized.contains("clothing")) {
            return new TypographyCopy("নতুন স্টাইল", "আপনার পছন্দের পোশাক", "", fallbackCta(request));
        }
        if (normalized.contains("food") || normalized.contains("restaurant")) {
            return new TypographyCopy("স্বাদের নতুন অভিজ্ঞতা", "আজই উপভোগ করুন", "", fallbackCta(request));
        }
        return new TypographyCopy("বিশেষ অফার", "আজই সংগ্রহ করুন", "", fallbackCta(request));
    }

    private String fallbackCta(AiCreativeGenerateRequest request) {
        if (request.includeCta() == null || !request.includeCta()) {
            return "";
        }
        String cta = banglaText(request.cta());
        return cta.isBlank() ? "অর্ডার করুন" : cta;
    }

    private void validate(TypographyCopy copy) {
        List<String> failures = new ArrayList<>();
        for (String text : List.of(copy.headline(), copy.subheadline(), copy.offer(), copy.cta())) {
            BanglaTypographyQualityService.TypographyValidation validation = qualityService.validateText(text);
            failures.addAll(validation.failures());
        }
        requireDisplayable(resolveFont(BengaliTypographyDatasetService.TextRole.HEADLINE, 48, Font.BOLD), copy.headline(), "headline");
        requireDisplayable(resolveFont(BengaliTypographyDatasetService.TextRole.BODY, 30, Font.PLAIN), copy.subheadline(), "subheadline");
        requireDisplayable(resolveFont(BengaliTypographyDatasetService.TextRole.OFFER, 30, Font.BOLD), copy.offer(), "offer");
        if (!copy.cta().isBlank()) {
            requireDisplayable(resolveFont(BengaliTypographyDatasetService.TextRole.CTA, 32, Font.BOLD), copy.cta(), "cta");
        }
        if (!failures.isEmpty()) {
            throw new BusinessException(ErrorCode.GENERATION_VALIDATION_FAILED, String.join(" ", failures));
        }
    }

    private void requireDisplayable(Font font, String text, String role) {
        if (text == null || text.isBlank()) {
            return;
        }
        if (font.canDisplayUpTo(text) >= 0) {
            throw new BusinessException(
                    ErrorCode.GENERATION_VALIDATION_FAILED,
                    "No installed Bangla-capable font can render " + role + " text. Install Noto Sans Bengali or Hind Siliguri on the rendering host.");
        }
    }

    private List<Map<String, Object>> drawTypography(Graphics2D graphics, int width, int height, TypographyCopy copy) {
        List<Map<String, Object>> regions = new ArrayList<>();
        int margin = Math.max(44, width / 18);
        int headlineWidth = width - (margin * 2);
        int y = Math.max(margin, height / 12);

        if (!copy.headline().isBlank()) {
            Font headlineFont = resolveFont(BengaliTypographyDatasetService.TextRole.HEADLINE, Math.max(42, width / 14), Font.BOLD);
            y = drawWrapped(graphics, copy.headline(), headlineFont, Color.WHITE, margin, y, headlineWidth, 1.12f, regions, "headline");
        }

        if (!copy.subheadline().isBlank()) {
            Font bodyFont = resolveFont(BengaliTypographyDatasetService.TextRole.BODY, Math.max(24, width / 30), Font.PLAIN);
            y += Math.max(10, height / 80);
            y = drawWrapped(graphics, copy.subheadline(), bodyFont, new Color(255, 246, 224), margin, y, headlineWidth, 1.22f, regions, "subheadline");
        }

        if (!copy.offer().isBlank()) {
            Font offerFont = resolveFont(BengaliTypographyDatasetService.TextRole.OFFER, Math.max(22, width / 32), Font.BOLD);
            int badgeY = Math.max(y + 18, height / 2);
            drawBadgeText(graphics, copy.offer(), offerFont, margin, badgeY, regions, "offer");
        }

        if (!copy.cta().isBlank()) {
            Font ctaFont = resolveFont(BengaliTypographyDatasetService.TextRole.CTA, Math.max(24, width / 30), Font.BOLD);
            int ctaY = height - Math.max(108, height / 7);
            drawCta(graphics, copy.cta(), ctaFont, margin, ctaY, width - (margin * 2), regions);
        }
        return regions;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private String shortText(String value, int maxLength) {
        String cleaned = explicitTextLayer(value);
        if (cleaned.isBlank()) {
            return "";
        }
        String firstSentence = cleaned.split("[.!?।]", 2)[0].trim();
        if (firstSentence.isBlank()) {
            firstSentence = cleaned;
        }
        return firstSentence.length() > maxLength
                ? firstSentence.substring(0, Math.max(0, maxLength - 1)).trim() + "..."
                : firstSentence;
    }

    private int drawWrapped(Graphics2D graphics, String text, Font font, Color color, int x, int y, int maxWidth, float lineHeight, List<Map<String, Object>> regions, String role) {
        if (text == null || text.isBlank()) {
            return y;
        }
        graphics.setFont(font);
        graphics.setColor(shadowColor());
        FontMetrics metrics = graphics.getFontMetrics(font);
        List<String> lines = wrap(text, metrics, maxWidth);
        int currentY = y;
        int lineAdvance = Math.round(metrics.getHeight() * lineHeight);
        for (String line : lines) {
            drawAttributed(graphics, line, font, shadowColor(), x + 3, currentY + 3);
            drawAttributed(graphics, line, font, color, x, currentY);
            currentY += lineAdvance;
        }
        regions.add(region(role, x, y - metrics.getAscent(), maxWidth, Math.max(lineAdvance, lines.size() * lineAdvance), font));
        return currentY;
    }

    private void drawBadgeText(Graphics2D graphics, String text, Font font, int x, int y, List<Map<String, Object>> regions, String role) {
        graphics.setFont(font);
        FontMetrics metrics = graphics.getFontMetrics(font);
        int paddingX = 22;
        int paddingY = 14;
        int width = metrics.stringWidth(text) + paddingX * 2;
        int height = metrics.getHeight() + paddingY;
        graphics.setColor(new Color(19, 31, 25, 220));
        graphics.fillRoundRect(x, y, width, height, height, height);
        drawAttributed(graphics, text, font, new Color(255, 230, 155), x + paddingX, y + paddingY + metrics.getAscent() - 4);
        regions.add(region(role, x, y, width, height, font));
    }

    private void drawCta(Graphics2D graphics, String text, Font font, int x, int y, int maxWidth, List<Map<String, Object>> regions) {
        graphics.setFont(font);
        FontMetrics metrics = graphics.getFontMetrics(font);
        int paddingX = Math.max(26, maxWidth / 18);
        int width = Math.min(maxWidth, metrics.stringWidth(text) + paddingX * 2);
        int height = Math.max(64, metrics.getHeight() + 24);
        graphics.setColor(new Color(255, 207, 73));
        graphics.fillRoundRect(x, y, width, height, height, height);
        drawAttributed(graphics, text, font, new Color(21, 24, 18), x + paddingX, y + (height - metrics.getHeight()) / 2 + metrics.getAscent());
        regions.add(region("cta", x, y, width, height, font));
    }

    private List<String> wrap(String text, FontMetrics metrics, int maxWidth) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String token : text.split("\\s+")) {
            String candidate = line.isEmpty() ? token : line + " " + token;
            if (metrics.stringWidth(candidate) <= maxWidth || line.isEmpty()) {
                line = new StringBuilder(candidate);
            } else {
                lines.add(line.toString());
                line = new StringBuilder(token);
            }
        }
        if (!line.isEmpty()) {
            lines.add(line.toString());
        }
        return lines;
    }

    private Font resolveFont(BengaliTypographyDatasetService.TextRole role, int size, int style) {
        List<String> installed = List.of(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames(Locale.ENGLISH));
        List<String> preferred = new ArrayList<>();
        preferred.add(datasetService.bestFontFor(role).family());
        preferred.addAll(datasetService.fontProfiles().stream().map(BengaliTypographyDatasetService.FontProfile::family).toList());
        preferred.add("Nirmala UI");
        preferred.add("Vrinda");
        preferred.add("Serif");
        for (String family : preferred) {
            if (installed.stream().anyMatch(name -> name.equalsIgnoreCase(family)) || family.equals("Serif")) {
                return new Font(family, style, size);
            }
        }
        return new Font(Font.SERIF, style, size);
    }

    private void drawAttributed(Graphics2D graphics, String text, Font font, Color color, int x, int y) {
        AttributedString attributed = new AttributedString(text);
        attributed.addAttribute(TextAttribute.FONT, font);
        attributed.addAttribute(TextAttribute.LIGATURES, TextAttribute.LIGATURES_ON);
        graphics.setColor(color);
        graphics.drawString(attributed.getIterator(), x, y);
    }

    private void configure(Graphics2D graphics) {
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
    }

    private Map<String, Object> region(String role, int x, int y, int width, int height, Font font) {
        Map<String, Object> region = new LinkedHashMap<>();
        region.put("role", role);
        region.put("x", x);
        region.put("y", y);
        region.put("width", width);
        region.put("height", height);
        region.put("fontFamily", font.getFamily());
        region.put("fontSize", font.getSize());
        return region;
    }

    private Color shadowColor() {
        return new Color(0, 0, 0, 150);
    }

    private String explicitTextLayer(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String cleaned = value.trim();
        String normalized = cleaned.toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        if (normalized.contains("PLACEHOLDER")
                || normalized.equals("HEADLINE")
                || normalized.equals("SUBHEADLINE")
                || normalized.equals("OFFER")
                || normalized.equals("CTA")) {
            return "";
        }
        return cleaned;
    }

    public record RenderedTypography(boolean applied, byte[] imageBytes, Map<String, Object> metadata) {
    }

    private record TypographyCopy(String headline, String subheadline, String offer, String cta) {
    }
}

