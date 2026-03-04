package com.fortytwotalents.fotogallery.service;

import com.fortytwotalents.fotogallery.model.GalleryCode;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.assertj.core.api.Assertions.assertThat;

class QrCodeGeneratorServiceTest {

    private QrCodeGeneratorService service;

    private static final String BASE_URL = "https://my.site/gallery/";

    @BeforeEach
    void setUp() {
        service = new QrCodeGeneratorService();
    }

    @Test
    void generatesDecodableQrCode() throws Exception {
        GalleryCode code = new GalleryCode("XY9G-AB7K-92QF");

        BufferedImage image = service.generateQrCode(code, BASE_URL, 200);

        // Decode the QR code back to verify content
        String decodedUrl = decodeQrCode(image);
        assertThat(decodedUrl).isEqualTo("https://my.site/gallery/XY9G-AB7K-92QF");
    }

    @Test
    void imageHasExpectedDimensions() {
        GalleryCode code = new GalleryCode("TK2H-XY3M-88PL");

        BufferedImage image = service.generateQrCode(code, BASE_URL, 200);

        assertThat(image.getWidth()).isEqualTo(200);
        assertThat(image.getHeight()).isEqualTo(200);
    }

    @Test
    void differentCodesProduceDifferentImages() {
        GalleryCode code1 = new GalleryCode("XY9G-AB7K-92QF");
        GalleryCode code2 = new GalleryCode("TK2H-XY3M-88PL");

        BufferedImage image1 = service.generateQrCode(code1, BASE_URL, 200);
        BufferedImage image2 = service.generateQrCode(code2, BASE_URL, 200);

        // Images should have different pixel data
        assertThat(imagesAreEqual(image1, image2)).isFalse();
    }

    @Test
    void respectsCustomSize() {
        GalleryCode code = new GalleryCode("XY9G-AB7K-92QF");

        BufferedImage image = service.generateQrCode(code, BASE_URL, 300);

        assertThat(image.getWidth()).isEqualTo(300);
        assertThat(image.getHeight()).isEqualTo(300);
    }

    private String decodeQrCode(BufferedImage image) throws Exception {
        BufferedImageLuminanceSource source = new BufferedImageLuminanceSource(image);
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
        Result result = new MultiFormatReader().decode(bitmap);
        return result.getText();
    }

    private boolean imagesAreEqual(BufferedImage img1, BufferedImage img2) {
        if (img1.getWidth() != img2.getWidth() || img1.getHeight() != img2.getHeight()) {
            return false;
        }
        for (int y = 0; y < img1.getHeight(); y++) {
            for (int x = 0; x < img1.getWidth(); x++) {
                if (img1.getRGB(x, y) != img2.getRGB(x, y)) {
                    return false;
                }
            }
        }
        return true;
    }

}
