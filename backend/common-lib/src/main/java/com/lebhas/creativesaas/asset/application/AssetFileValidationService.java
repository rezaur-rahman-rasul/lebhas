package com.lebhas.creativesaas.asset.application;

import com.lebhas.creativesaas.asset.domain.AssetCategory;
import com.lebhas.creativesaas.asset.domain.AssetFileType;
import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.imageio.ImageIO;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
public class AssetFileValidationService {

    private static final Set<String> IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");
    private static final Set<String> VIDEO_EXTENSIONS = Set.of("mp4", "mov");
    private static final Set<String> LOGO_EXTENSIONS = Set.of("png", "svg", "webp");
    private static final Set<String> OTHER_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "mp4", "mov");
    private static final Set<String> DANGEROUS_EXTENSIONS = Set.of(
            "exe", "bat", "cmd", "com", "msi", "ps1", "php", "jsp", "sh", "jar", "js", "html", "svgz", "hta", "scr");
    private static final Set<String> DISALLOWED_SVG_ELEMENTS = Set.of("script", "foreignobject");
    private static final Set<String> DISALLOWED_SVG_ATTRIBUTE_NAMES = Set.of("href", "xlink:href", "src");
    private static final String SVG_MIME_TYPE = "image/svg+xml";

    private final Map<AssetCategory, ValidationRule> rulesByCategory = new EnumMap<>(AssetCategory.class);
    private final AssetServiceProperties assetServiceProperties;

    public AssetFileValidationService(AssetServiceProperties assetServiceProperties) {
        this.assetServiceProperties = assetServiceProperties;
        ValidationRule imageRule = new ValidationRule(
                IMAGE_EXTENSIONS,
                Set.of("image/jpeg", "image/png", "image/webp"),
                assetServiceProperties.getMaxImageSizeBytes());
        ValidationRule videoRule = new ValidationRule(
                VIDEO_EXTENSIONS,
                Set.of("video/mp4", "video/quicktime"),
                assetServiceProperties.getMaxVideoSizeBytes());
        ValidationRule logoRule = new ValidationRule(
                LOGO_EXTENSIONS,
                Set.of("image/png", "image/webp", SVG_MIME_TYPE),
                assetServiceProperties.getMaxLogoSizeBytes());
        ValidationRule otherRule = new ValidationRule(
                OTHER_EXTENSIONS,
                Set.of("image/jpeg", "image/png", "image/webp", "video/mp4", "video/quicktime"),
                assetServiceProperties.getMaxUploadSizeBytes());

        rulesByCategory.put(AssetCategory.PRODUCT_IMAGE, imageRule);
        rulesByCategory.put(AssetCategory.PACKAGING_IMAGE, imageRule);
        rulesByCategory.put(AssetCategory.EXPORT_IMAGE, imageRule);
        rulesByCategory.put(AssetCategory.REFERENCE_IMAGE, imageRule);
        rulesByCategory.put(AssetCategory.PRODUCT_VIDEO, videoRule);
        rulesByCategory.put(AssetCategory.EXPORT_VIDEO, videoRule);
        rulesByCategory.put(AssetCategory.REFERENCE_VIDEO, videoRule);
        rulesByCategory.put(AssetCategory.BRAND_LOGO, logoRule);
        rulesByCategory.put(AssetCategory.REFERENCE_ASSET, otherRule);
        rulesByCategory.put(AssetCategory.OTHER, otherRule);
    }

    public ValidatedAssetFile validate(MultipartFile file, AssetCategory category) {
        if (file == null || file.isEmpty() || file.getSize() <= 0) {
            throw new BusinessException(ErrorCode.ASSET_FILE_EMPTY);
        }

        String originalFileName = StringUtils.hasText(file.getOriginalFilename())
                ? file.getOriginalFilename().trim()
                : "upload";
        String extension = resolveExtension(originalFileName);
        ValidationRule rule = rulesByCategory.getOrDefault(category, rulesByCategory.get(AssetCategory.OTHER));
        if (!rule.allowedExtensions().contains(extension)) {
            throw new BusinessException(ErrorCode.ASSET_FILE_TYPE_INVALID, "File extension '" + extension + "' is not allowed for " + category.name());
        }
        if (file.getSize() > rule.maxSizeBytes()) {
            throw new BusinessException(
                    ErrorCode.ASSET_FILE_SIZE_EXCEEDED,
                    "File size exceeds the limit for " + category.name() + " (" + rule.maxSizeBytes() + " bytes)");
        }

        DetectedContent detectedContent = detectContent(file, extension);
        if (!rule.allowedMimeTypes().contains(detectedContent.mimeType())) {
            throw new BusinessException(ErrorCode.ASSET_FILE_TYPE_INVALID, "Detected file type is not allowed for " + category.name());
        }

        String sanitizedFilename = sanitizeFilename(originalFileName, extension);
        FileDimensions dimensions = extractDimensions(file, detectedContent.mimeType());
        return new ValidatedAssetFile(
                originalFileName,
                sanitizedFilename,
                extension,
                detectedContent.mimeType(),
                file.getSize(),
                detectedContent.fileType(),
                dimensions.width(),
                dimensions.height(),
                null);
    }

    public ValidatedAssetFile validateMetadata(
            String originalFileName,
            String contentType,
            long size,
            AssetCategory category
    ) {
        if (!StringUtils.hasText(originalFileName)) {
            throw new BusinessException(ErrorCode.ASSET_FILENAME_INVALID, "Filename is required");
        }
        if (!StringUtils.hasText(contentType)) {
            throw new BusinessException(ErrorCode.ASSET_FILE_TYPE_INVALID, "Content type is required");
        }
        if (size <= 0) {
            throw new BusinessException(ErrorCode.ASSET_FILE_EMPTY);
        }
        String extension = resolveExtension(originalFileName.trim());
        ValidationRule rule = rulesByCategory.getOrDefault(category, rulesByCategory.get(AssetCategory.OTHER));
        if (!rule.allowedExtensions().contains(extension)) {
            throw new BusinessException(ErrorCode.ASSET_FILE_TYPE_INVALID, "File extension '" + extension + "' is not allowed for " + category.name());
        }
        if (size > rule.maxSizeBytes()) {
            throw new BusinessException(
                    ErrorCode.ASSET_FILE_SIZE_EXCEEDED,
                    "File size exceeds the limit for " + category.name() + " (" + rule.maxSizeBytes() + " bytes)");
        }
        String normalizedContentType = contentType.trim().toLowerCase(Locale.ROOT);
        if (!rule.allowedMimeTypes().contains(normalizedContentType)) {
            throw new BusinessException(ErrorCode.ASSET_FILE_TYPE_INVALID, "Content type is not allowed for " + category.name());
        }
        String sanitizedFilename = sanitizeFilename(originalFileName.trim(), extension);
        return new ValidatedAssetFile(
                originalFileName.trim(),
                sanitizedFilename,
                extension,
                normalizedContentType,
                size,
                fileTypeFromMimeType(normalizedContentType),
                null,
                null,
                null);
    }

    private String resolveExtension(String originalFileName) {
        String normalized = originalFileName.trim();
        if (normalized.chars().anyMatch(Character::isISOControl)) {
            throw new BusinessException(ErrorCode.ASSET_FILENAME_INVALID, "Filename contains unsupported control characters");
        }
        int lastDotIndex = normalized.lastIndexOf('.');
        if (lastDotIndex < 0 || lastDotIndex == normalized.length() - 1) {
            throw new BusinessException(ErrorCode.ASSET_FILENAME_INVALID, "File extension is required");
        }
        String[] segments = normalized.toLowerCase(Locale.ROOT).split("\\.");
        for (int index = 0; index < segments.length - 1; index++) {
            if (DANGEROUS_EXTENSIONS.contains(segments[index])) {
                throw new BusinessException(ErrorCode.ASSET_FILENAME_INVALID, "Filename contains a blocked extension segment");
            }
        }
        String extension = segments[segments.length - 1];
        if (DANGEROUS_EXTENSIONS.contains(extension)) {
            throw new BusinessException(ErrorCode.ASSET_FILE_TYPE_INVALID, "Blocked file extension");
        }
        return extension;
    }

    private String sanitizeFilename(String originalFileName, String extension) {
        String baseName = originalFileName.substring(0, originalFileName.lastIndexOf('.'));
        String sanitized = baseName
                .replace('\\', '-')
                .replace('/', '-')
                .replaceAll("[^A-Za-z0-9._-]+", "-")
                .replaceAll("-{2,}", "-")
                .replaceAll("^[-.]+|[-.]+$", "");
        if (!StringUtils.hasText(sanitized)) {
            throw new BusinessException(ErrorCode.ASSET_FILENAME_INVALID, "Filename does not contain any safe characters");
        }
        return sanitized + "." + extension;
    }

    private DetectedContent detectContent(MultipartFile file, String extension) {
        try (InputStream inputStream = new BufferedInputStream(file.getInputStream())) {
            inputStream.mark(8192);
            byte[] header = inputStream.readNBytes(64);
            inputStream.reset();

            if (isJpeg(header)) {
                return new DetectedContent("image/jpeg", AssetFileType.IMAGE);
            }
            if (isPng(header)) {
                return new DetectedContent("image/png", AssetFileType.IMAGE);
            }
            if (isWebp(header)) {
                return new DetectedContent("image/webp", AssetFileType.IMAGE);
            }
            if (isMp4(header)) {
                return new DetectedContent("video/mp4", AssetFileType.VIDEO);
            }
            if (isQuickTime(header)) {
                return new DetectedContent("video/quicktime", AssetFileType.VIDEO);
            }
            if ("svg".equals(extension) && isSafeSvg(inputStream)) {
                return new DetectedContent(SVG_MIME_TYPE, AssetFileType.VECTOR_IMAGE);
            }
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.ASSET_FILE_CONTENT_INVALID, "Asset content could not be validated");
        }
        throw new BusinessException(ErrorCode.ASSET_FILE_CONTENT_INVALID, "Asset content does not match a supported file signature");
    }

    private FileDimensions extractDimensions(MultipartFile file, String mimeType) {
        if (!mimeType.startsWith("image/") || SVG_MIME_TYPE.equals(mimeType)) {
            return FileDimensions.empty();
        }
        try (InputStream inputStream = file.getInputStream()) {
            BufferedImage image = ImageIO.read(inputStream);
            if (image == null) {
                return FileDimensions.empty();
            }
            return new FileDimensions(image.getWidth(), image.getHeight());
        } catch (Exception ignored) {
            return FileDimensions.empty();
        }
    }

    private boolean isJpeg(byte[] header) {
        return header.length >= 3
                && (header[0] & 0xFF) == 0xFF
                && (header[1] & 0xFF) == 0xD8
                && (header[2] & 0xFF) == 0xFF;
    }

    private boolean isPng(byte[] header) {
        return header.length >= 8
                && (header[0] & 0xFF) == 0x89
                && header[1] == 0x50
                && header[2] == 0x4E
                && header[3] == 0x47
                && header[4] == 0x0D
                && header[5] == 0x0A
                && header[6] == 0x1A
                && header[7] == 0x0A;
    }

    private boolean isWebp(byte[] header) {
        return header.length >= 12
                && ascii(header, 0, 4).equals("RIFF")
                && ascii(header, 8, 4).equals("WEBP");
    }

    private boolean isMp4(byte[] header) {
        return header.length >= 12
                && ascii(header, 4, 4).equals("ftyp")
                && Set.of("isom", "iso2", "mp41", "mp42", "avc1", "M4V ", "dash").contains(ascii(header, 8, 4));
    }

    private boolean isQuickTime(byte[] header) {
        return header.length >= 12
                && ascii(header, 4, 4).equals("ftyp")
                && ascii(header, 8, 4).equals("qt  ");
    }

    private String ascii(byte[] value, int start, int length) {
        if (value.length < start + length) {
            return "";
        }
        return new String(value, start, length, StandardCharsets.US_ASCII);
    }

    private AssetFileType fileTypeFromMimeType(String mimeType) {
        if (SVG_MIME_TYPE.equals(mimeType)) {
            return AssetFileType.VECTOR_IMAGE;
        }
        if (mimeType != null && mimeType.startsWith("image/")) {
            return AssetFileType.IMAGE;
        }
        if (mimeType != null && mimeType.startsWith("video/")) {
            return AssetFileType.VIDEO;
        }
        return AssetFileType.IMAGE;
    }

    private boolean isSafeSvg(InputStream inputStream) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);

            Document document = factory.newDocumentBuilder().parse(inputStream);
            Element root = document.getDocumentElement();
            if (root == null || !"svg".equalsIgnoreCase(root.getLocalName())) {
                throw new BusinessException(ErrorCode.ASSET_FILE_CONTENT_INVALID, "SVG content is invalid");
            }
            inspectSvgNode(root);
            return true;
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.ASSET_FILE_CONTENT_INVALID, "SVG content is invalid");
        }
    }

    private void inspectSvgNode(Node node) {
        if (!(node instanceof Element element)) {
            return;
        }
        String elementName = element.getTagName().toLowerCase(Locale.ROOT);
        if (DISALLOWED_SVG_ELEMENTS.contains(elementName)) {
            throw new BusinessException(ErrorCode.ASSET_FILE_CONTENT_INVALID, "SVG contains disallowed elements");
        }

        NamedNodeMap attributes = element.getAttributes();
        for (int index = 0; index < attributes.getLength(); index++) {
            Node attribute = attributes.item(index);
            String attributeName = attribute.getNodeName().toLowerCase(Locale.ROOT);
            String attributeValue = attribute.getNodeValue() == null ? "" : attribute.getNodeValue().trim().toLowerCase(Locale.ROOT);
            if (attributeName.startsWith("on")) {
                throw new BusinessException(ErrorCode.ASSET_FILE_CONTENT_INVALID, "SVG contains scriptable event attributes");
            }
            if (DISALLOWED_SVG_ATTRIBUTE_NAMES.contains(attributeName)) {
                throw new BusinessException(ErrorCode.ASSET_FILE_CONTENT_INVALID, "SVG contains external references");
            }
            if (attributeValue.contains("javascript:") || attributeValue.contains("data:text/html") || attributeValue.contains("<script")) {
                throw new BusinessException(ErrorCode.ASSET_FILE_CONTENT_INVALID, "SVG contains scriptable content");
            }
        }

        NodeList children = element.getChildNodes();
        for (int index = 0; index < children.getLength(); index++) {
            inspectSvgNode(children.item(index));
        }
    }

    private record ValidationRule(Set<String> allowedExtensions, Set<String> allowedMimeTypes, long maxSizeBytes) {
    }

    private record FileDimensions(Integer width, Integer height) {

        private static FileDimensions empty() {
            return new FileDimensions(null, null);
        }
    }

    private record DetectedContent(String mimeType, AssetFileType fileType) {
    }

    public record ValidatedAssetFile(
            String originalFileName,
            String sanitizedFileName,
            String extension,
            String mimeType,
            long size,
            AssetFileType fileType,
            Integer width,
            Integer height,
            Long duration
    ) {
    }
}
