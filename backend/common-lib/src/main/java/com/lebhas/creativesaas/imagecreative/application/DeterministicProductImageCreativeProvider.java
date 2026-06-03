package com.lebhas.creativesaas.imagecreative.application;

import com.lebhas.ai.application.dto.ResolvedProviderRouteView;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DeterministicProductImageCreativeProvider implements ProductImageCreativeProvider {

    @Override
    public List<ProductImageCreativeProviderOutput> generate(ProductImageCreativeContext context, int count, ResolvedProviderRouteView route) {
        List<ProductImageCreativeProviderOutput> outputs = new ArrayList<>();
        for (int index = 1; index <= count; index++) {
            String base = "image-creatives/%s/%s/%s-v%s.png".formatted(
                    context.project().getWorkspaceId(),
                    context.project().getId(),
                    context.generationId(),
                    index);
            outputs.add(new ProductImageCreativeProviderOutput(
                    base,
                    "image-creative-%s.png".formatted(index),
                    "image/png",
                    "png",
                    context.request().creativeFormat().width(),
                    context.request().creativeFormat().height(),
                    renderPreviewPng(context, index),
                    Map.of(
                            "stylePreset", context.request().stylePreset() == null ? "" : context.request().stylePreset(),
                            "backgroundStyle", context.request().backgroundStyle() == null ? "" : context.request().backgroundStyle(),
                            "variant", index)));
        }
        return outputs;
    }

    private byte[] renderPreviewPng(ProductImageCreativeContext context, int variant) {
        int width = context.request().creativeFormat().width();
        int height = context.request().creativeFormat().height();
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setPaint(new java.awt.GradientPaint(0, 0, new Color(18, 24, 38), width, height, new Color(168, 85, 247)));
            graphics.fillRect(0, 0, width, height);

            int margin = Math.max(56, width / 18);
            graphics.setColor(new Color(255, 255, 255, 235));
            graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, Math.max(42, width / 18)));
            String productName = context.product() == null || context.product().getName() == null
                    ? "Campaign creative"
                    : context.product().getName();
            drawWrapped(graphics, productName, margin, margin + 80, width - margin * 2, Math.max(48, width / 18 + 12));

            graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, Math.max(30, width / 32)));
            graphics.setColor(new Color(226, 232, 240, 230));
            drawWrapped(graphics, context.request().sourcePrompt(), margin, height / 2 - 40, width - margin * 2, Math.max(36, width / 32 + 10));

            graphics.setColor(new Color(236, 72, 153));
            int ctaHeight = Math.max(72, height / 13);
            int ctaWidth = Math.min(width - margin * 2, Math.max(300, width / 3));
            int ctaY = height - margin - ctaHeight;
            graphics.fillRoundRect(margin, ctaY, ctaWidth, ctaHeight, 34, 34);
            graphics.setColor(Color.WHITE);
            graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, Math.max(28, width / 34)));
            String cta = context.request().cta() == null || context.request().cta().isBlank() ? "Shop Now" : context.request().cta();
            graphics.drawString(cta, margin + 34, ctaY + ctaHeight / 2 + Math.max(10, width / 90));

            graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, Math.max(20, width / 54)));
            graphics.setColor(new Color(255, 255, 255, 175));
            graphics.drawString("Generated preview v" + variant, margin, height - Math.max(18, margin / 3));
        } finally {
            graphics.dispose();
        }

        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not render generated creative preview", exception);
        }
    }

    private void drawWrapped(Graphics2D graphics, String text, int x, int y, int maxWidth, int lineHeight) {
        if (text == null || text.isBlank()) {
            return;
        }
        StringBuilder line = new StringBuilder();
        int currentY = y;
        for (String word : text.trim().split("\\s+")) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (graphics.getFontMetrics().stringWidth(candidate) > maxWidth && !line.isEmpty()) {
                graphics.drawString(line.toString(), x, currentY);
                line = new StringBuilder(word);
                currentY += lineHeight;
            } else {
                line = new StringBuilder(candidate);
            }
        }
        if (!line.isEmpty()) {
            graphics.drawString(line.toString(), x, currentY);
        }
    }
}
