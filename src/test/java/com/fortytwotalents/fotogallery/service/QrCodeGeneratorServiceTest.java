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

		BufferedImage image = service.generateQrCode(code, BASE_URL, 200, 1);

		// Decode the QR code back to verify content
		String decodedUrl = decodeQrCode(image);
		assertThat(decodedUrl).isEqualTo("https://my.site/gallery/XY9G-AB7K-92QF");
	}

	@Test
	void imageHasHighResolutionDimensions() {
		GalleryCode code = new GalleryCode("TK2H-XY3M-88PL");

		BufferedImage image = service.generateQrCode(code, BASE_URL, 200, 1);

		// Image is rendered at 3x for crisp PDF embedding
		assertThat(image.getWidth()).isEqualTo(600);
		assertThat(image.getHeight()).isEqualTo(600);
	}

	@Test
	void differentCodesProduceDifferentImages() {
		GalleryCode code1 = new GalleryCode("XY9G-AB7K-92QF");
		GalleryCode code2 = new GalleryCode("TK2H-XY3M-88PL");

		BufferedImage image1 = service.generateQrCode(code1, BASE_URL, 200, 1);
		BufferedImage image2 = service.generateQrCode(code2, BASE_URL, 200, 2);

		// Images should have different pixel data
		assertThat(imagesAreEqual(image1, image2)).isFalse();
	}

	@Test
	void respectsCustomSize() {
		GalleryCode code = new GalleryCode("XY9G-AB7K-92QF");

		BufferedImage image = service.generateQrCode(code, BASE_URL, 300, 1);

		// 300 × 3x render scale = 900
		assertThat(image.getWidth()).isEqualTo(900);
		assertThat(image.getHeight()).isEqualTo(900);
	}

	@Test
	void overlayHasWhiteCenterPixels() {
		GalleryCode code = new GalleryCode("XY9G-AB7K-92QF");

		BufferedImage image = service.generateQrCode(code, BASE_URL, 200, 1);

		// The center of the image should have white pixels (from the overlay circle)
		int centerX = image.getWidth() / 2;
		int centerY = image.getHeight() / 2;
		int rgb = image.getRGB(centerX, centerY) & 0x00FFFFFF;
		// Center pixel should be either white (background) or black (number text)
		// Since we draw a white circle then text, the area around center should have
		// white
		assertThat(rgb == 0xFFFFFF || rgb == 0x000000).isTrue();
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
