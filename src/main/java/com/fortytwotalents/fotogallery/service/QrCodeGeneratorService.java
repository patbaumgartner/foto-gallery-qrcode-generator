package com.fortytwotalents.fotogallery.service;

import com.fortytwotalents.fotogallery.model.GalleryCode;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.util.Map;

@Service
public class QrCodeGeneratorService {

	public BufferedImage generateQrCode(GalleryCode galleryCode, String baseUrl, int size) {
		String url = galleryCode.toUrl(baseUrl);
		try {
			Map<EncodeHintType, Object> hints = Map.of(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M,
					EncodeHintType.MARGIN, 1, EncodeHintType.CHARACTER_SET, "UTF-8");

			QRCodeWriter writer = new QRCodeWriter();
			BitMatrix bitMatrix = writer.encode(url, BarcodeFormat.QR_CODE, size, size, hints);
			return MatrixToImageWriter.toBufferedImage(bitMatrix);
		}
		catch (WriterException e) {
			throw new RuntimeException("Failed to generate QR code for: " + url, e);
		}
	}

}
