package com.lebhas.creativesaas.asset.application;

import com.lebhas.creativesaas.asset.domain.AssetCategory;
import com.lebhas.creativesaas.asset.domain.AssetFileType;
import com.lebhas.creativesaas.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AssetFileValidationServiceTest {

    private final AssetFileValidationService service = new AssetFileValidationService(new AssetServiceProperties());

    @Test
    void shouldAcceptJpgUpload() {
        AssetFileValidationService.ValidatedAssetFile validated = service.validate(
                new MockMultipartFile("file", "photo.jpg", "image/jpeg", jpgBytes()),
                AssetCategory.PRODUCT_IMAGE);

        assertThat(validated.extension()).isEqualTo("jpg");
        assertThat(validated.mimeType()).isEqualTo("image/jpeg");
        assertThat(validated.fileType()).isEqualTo(AssetFileType.IMAGE);
    }

    @Test
    void shouldAcceptPngUpload() {
        AssetFileValidationService.ValidatedAssetFile validated = service.validate(
                new MockMultipartFile("file", "photo.png", "image/png", pngBytes()),
                AssetCategory.REFERENCE_IMAGE);

        assertThat(validated.extension()).isEqualTo("png");
        assertThat(validated.mimeType()).isEqualTo("image/png");
        assertThat(validated.fileType()).isEqualTo(AssetFileType.IMAGE);
    }

    @Test
    void shouldAcceptJpgBrandLogoUpload() {
        AssetFileValidationService.ValidatedAssetFile validated = service.validate(
                new MockMultipartFile("file", "brand-logo.jpg", "image/jpeg", jpgBytes()),
                AssetCategory.BRAND_LOGO);

        assertThat(validated.extension()).isEqualTo("jpg");
        assertThat(validated.mimeType()).isEqualTo("image/jpeg");
        assertThat(validated.fileType()).isEqualTo(AssetFileType.IMAGE);
    }

    @Test
    void shouldAcceptMp4Upload() {
        AssetFileValidationService.ValidatedAssetFile validated = service.validate(
                new MockMultipartFile("file", "clip.mp4", "video/mp4", mp4Bytes()),
                AssetCategory.PRODUCT_VIDEO);

        assertThat(validated.extension()).isEqualTo("mp4");
        assertThat(validated.mimeType()).isEqualTo("video/mp4");
        assertThat(validated.fileType()).isEqualTo(AssetFileType.VIDEO);
    }

    @Test
    void shouldRejectUnsupportedFileType() {
        assertThatThrownBy(() -> service.validate(
                new MockMultipartFile("file", "notes.txt", "text/plain", "invalid".getBytes()),
                AssetCategory.OTHER))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("not allowed");
    }

    @Test
    void shouldRejectOversizedImage() {
        assertThatThrownBy(() -> service.validate(
                oversizedFile("too-large.png", "image/png", (10L * 1024L * 1024L) + 1L, pngBytes()),
                AssetCategory.PRODUCT_IMAGE))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("File size exceeds the limit");
    }

    @Test
    void shouldRejectOversizedVideo() {
        assertThatThrownBy(() -> service.validate(
                oversizedFile("too-large.mp4", "video/mp4", (200L * 1024L * 1024L) + 1L, mp4Bytes()),
                AssetCategory.PRODUCT_VIDEO))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("File size exceeds the limit");
    }

    @Test
    void shouldRejectMaliciousSvgUpload() {
        assertThatThrownBy(() -> service.validate(
                new MockMultipartFile(
                        "file",
                        "brand-logo.svg",
                        "image/svg+xml",
                        """
                        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 10 10">
                          <script>alert('xss')</script>
                        </svg>
                        """.getBytes()),
                AssetCategory.BRAND_LOGO))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("SVG");
    }

    private MultipartFile oversizedFile(String fileName, String contentType, long size, byte[] content) {
        return new MultipartFile() {
            @Override
            public String getName() {
                return "file";
            }

            @Override
            public String getOriginalFilename() {
                return fileName;
            }

            @Override
            public String getContentType() {
                return contentType;
            }

            @Override
            public boolean isEmpty() {
                return false;
            }

            @Override
            public long getSize() {
                return size;
            }

            @Override
            public byte[] getBytes() {
                return content.clone();
            }

            @Override
            public InputStream getInputStream() {
                return new ByteArrayInputStream(content);
            }

            @Override
            public void transferTo(java.io.File dest) throws IOException, IllegalStateException {
                throw new UnsupportedOperationException("Not needed for this test");
            }

            @Override
            public void transferTo(java.nio.file.Path dest) throws IOException, IllegalStateException {
                throw new UnsupportedOperationException("Not needed for this test");
            }
        };
    }

    private byte[] pngBytes() {
        return java.util.Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+pC9sAAAAASUVORK5CYII=");
    }

    private byte[] jpgBytes() {
        return new byte[]{
                (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0,
                0x00, 0x10, 0x4A, 0x46, 0x49, 0x46, 0x00, 0x01
        };
    }

    private byte[] mp4Bytes() {
        return new byte[]{
                0x00, 0x00, 0x00, 0x18,
                0x66, 0x74, 0x79, 0x70,
                0x69, 0x73, 0x6F, 0x6D,
                0x00, 0x00, 0x00, 0x00
        };
    }
}
