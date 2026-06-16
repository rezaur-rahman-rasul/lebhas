package com.lebhas.ai.creative.service;

import com.lebhas.ai.creative.enums.OutputFormat;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

@Service
public class LogoOverlayService {

    public OverlayResult overlay(byte[] imageBytes, byte[] logoBytes, OutputFormat outputFormat) {
        String writerFormat = outputFormat == OutputFormat.jpeg ? "jpg" : "png";
        return overlay(imageBytes, logoBytes, writerFormat);
    }

    public OverlayResult overlay(byte[] imageBytes, byte[] logoBytes, String writerFormat) {
        if (imageBytes == null || imageBytes.length == 0 || logoBytes == null || logoBytes.length == 0) {
            return new OverlayResult(false, imageBytes, Map.of("reason", "missing-image-or-logo"));
        }
        try {
            BufferedImage source = ImageIO.read(new ByteArrayInputStream(imageBytes));
            BufferedImage logo = ImageIO.read(new ByteArrayInputStream(logoBytes));
            if (source == null || logo == null) {
                return new OverlayResult(false, imageBytes, Map.of("reason", "unsupported-logo-format"));
            }

            BufferedImage canvas = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = canvas.createGraphics();
            configure(graphics);
            graphics.drawImage(source, 0, 0, null);

            int margin = Math.max(32, source.getWidth() / 28);
            int maxLogoWidth = Math.max(96, source.getWidth() / 5);
            int maxLogoHeight = Math.max(72, source.getHeight() / 8);
            double scale = Math.min((double) maxLogoWidth / logo.getWidth(), (double) maxLogoHeight / logo.getHeight());
            scale = Math.min(1.0d, Math.max(0.1d, scale));
            int logoWidth = Math.max(1, (int) Math.round(logo.getWidth() * scale));
            int logoHeight = Math.max(1, (int) Math.round(logo.getHeight() * scale));
            int padding = Math.max(14, source.getWidth() / 80);
            int plateWidth = logoWidth + padding * 2;
            int plateHeight = logoHeight + padding * 2;
            int x = source.getWidth() - margin - plateWidth;
            int y = margin;

            graphics.setComposite(AlphaComposite.SrcOver);
            graphics.setColor(new Color(255, 255, 255, 218));
            graphics.fillRoundRect(x, y, plateWidth, plateHeight, padding * 2, padding * 2);
            graphics.drawImage(logo, x + padding, y + padding, logoWidth, logoHeight, null);
            graphics.dispose();

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            if ("jpg".equalsIgnoreCase(writerFormat) || "jpeg".equalsIgnoreCase(writerFormat)) {
                BufferedImage rgb = new BufferedImage(canvas.getWidth(), canvas.getHeight(), BufferedImage.TYPE_INT_RGB);
                Graphics2D jpg = rgb.createGraphics();
                jpg.setColor(Color.WHITE);
                jpg.fillRect(0, 0, rgb.getWidth(), rgb.getHeight());
                jpg.drawImage(canvas, 0, 0, null);
                jpg.dispose();
                ImageIO.write(rgb, "jpg", out);
            } else {
                ImageIO.write(canvas, "png", out);
            }
            return new OverlayResult(true, out.toByteArray(), Map.of(
                    "engine", "JAVA2D_LOGO_OVERLAY",
                    "x", x,
                    "y", y,
                    "width", logoWidth,
                    "height", logoHeight));
        } catch (IOException exception) {
            return new OverlayResult(false, imageBytes, Map.of("reason", "logo-overlay-failed"));
        }
    }

    private void configure(Graphics2D graphics) {
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    }

    public record OverlayResult(boolean applied, byte[] imageBytes, Map<String, Object> metadata) {
    }
}
