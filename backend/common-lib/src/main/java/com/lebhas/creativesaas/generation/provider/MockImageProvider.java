package com.lebhas.creativesaas.generation.provider;

import com.lebhas.creativesaas.generation.domain.CreativeOutputFormat;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class MockImageProvider implements ImageGenerationProvider {

    private static final int DEFAULT_IMAGE_WIDTH = 1024;
    private static final int DEFAULT_IMAGE_HEIGHT = 1024;

    @Override
    public CreativeAiProviderType type() {
        return CreativeAiProviderType.MOCK;
    }

    @Override
    public AiGenerationResponse generate(AiGenerationRequest request) {
        int width = request.width() == null ? DEFAULT_IMAGE_WIDTH : request.width();
        int height = request.height() == null ? DEFAULT_IMAGE_HEIGHT : request.height();
        CreativeOutputFormat responseFormat = resolveResponseFormat(request.outputFormat());
        byte[] content = renderImage(request, responseFormat, width, height);

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("renderMode", "mock");
        metadata.put("requestedOutputFormat", request.outputFormat().name());
        metadata.put("responseOutputFormat", responseFormat.name());
        metadata.put("promptHash", Integer.toUnsignedString(request.prompt().hashCode()));

        return new AiGenerationResponse(
                CreativeAiProviderType.MOCK.name(),
                "mock-image-v1",
                "mock-" + UUID.randomUUID(),
                content,
                responseFormat.mimeType(),
                responseFormat,
                width,
                height,
                null,
                headlineFor(request),
                subheadlineFor(request),
                ctaFor(request),
                metadata);
    }

    private byte[] renderImage(
            AiGenerationRequest request,
            CreativeOutputFormat responseFormat,
            int width,
            int height
    ) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            int accentSeed = Math.abs(request.requestId().hashCode());
            Color start = new Color(30 + accentSeed % 80, 50 + accentSeed % 120, 90 + accentSeed % 140);
            Color end = new Color(180, 90 + accentSeed % 100, 70 + accentSeed % 80);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setPaint(new GradientPaint(0, 0, start, width, height, end));
            graphics.fillRect(0, 0, width, height);

            graphics.setColor(new Color(255, 255, 255, 38));
            graphics.fillRoundRect(width / 10, height / 8, (width * 4) / 5, (height * 3) / 4, 32, 32);

            graphics.setStroke(new BasicStroke(Math.max(4, width / 128f)));
            graphics.setColor(new Color(255, 255, 255, 110));
            graphics.drawRoundRect(width / 10, height / 8, (width * 4) / 5, (height * 3) / 4, 32, 32);

            graphics.setColor(Color.WHITE);
            graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, Math.max(28, width / 20)));
            graphics.drawString(request.creativeType().name().replace('_', ' '), width / 7, height / 3);

            graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, Math.max(18, width / 38)));
            graphics.drawString(request.platform().name().replace('_', ' '), width / 7, height / 3 + Math.max(40, height / 14));
            graphics.drawString(truncate(request.prompt(), 80), width / 7, height / 3 + Math.max(95, height / 8));
        } finally {
            graphics.dispose();
        }

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ImageIO.write(image, imageFormatName(responseFormat), outputStream);
            return outputStream.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Mock image provider could not render a synthetic image", exception);
        }
    }

    private CreativeOutputFormat resolveResponseFormat(CreativeOutputFormat requestedFormat) {
        if (requestedFormat == CreativeOutputFormat.JPG) {
            return CreativeOutputFormat.JPG;
        }
        return CreativeOutputFormat.PNG;
    }

    private String imageFormatName(CreativeOutputFormat responseFormat) {
        return responseFormat == CreativeOutputFormat.JPG ? "jpg" : "png";
    }

    private String headlineFor(AiGenerationRequest request) {
        return request.creativeType().name().replace('_', ' ');
    }

    private String subheadlineFor(AiGenerationRequest request) {
        return request.platform().name().replace('_', ' ');
    }

    private String ctaFor(AiGenerationRequest request) {
        return request.language().name().equals("BANGLA") ? "আরও দেখুন" : "Shop now";
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return "Mock creative output";
        }
        String normalized = new String(value.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8)
                .replaceAll("\\s+", " ")
                .trim();
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength - 3) + "...";
    }
}
