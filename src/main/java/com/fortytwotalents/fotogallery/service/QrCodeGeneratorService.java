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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.util.Map;

@Service
public class QrCodeGeneratorService {

	private static final int RENDER_SCALE = 3;

	private static final double CIRCLE_RATIO = 0.28;

	private static final double FONT_RATIO = 0.60;

	public BufferedImage generateQrCode(GalleryCode galleryCode, String baseUrl, int size, int number) {
		String url = galleryCode.toUrl(baseUrl);
		try {
			int renderSize = size * RENDER_SCALE;

			Map<EncodeHintType, Object> hints = Map.of(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H,
					EncodeHintType.MARGIN, 1, EncodeHintType.CHARACTER_SET, "UTF-8");

			QRCodeWriter writer = new QRCodeWriter();
			BitMatrix bitMatrix = writer.encode(url, BarcodeFormat.QR_CODE, renderSize, renderSize, hints);
			BufferedImage hiResImage = MatrixToImageWriter.toBufferedImage(bitMatrix);
			drawNumberOverlay(hiResImage, number);
			return hiResImage;
		}
		catch (WriterException e) {
			throw new IllegalStateException("Failed to generate QR code for: " + url, e);
		}
	}

	private void drawNumberOverlay(BufferedImage image, int number) {
		int size = image.getWidth();
		int diameter = (int) (size * CIRCLE_RATIO);
		int centerX = size / 2;
		int centerY = size / 2;

		Graphics2D g2d = image.createGraphics();
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

		// White filled circle
		Ellipse2D circle = new Ellipse2D.Double(centerX - diameter / 2.0, centerY - diameter / 2.0, diameter, diameter);
		g2d.setColor(Color.WHITE);
		g2d.fill(circle);

		// Thin gray border
		g2d.setColor(new Color(160, 160, 160));
		g2d.setStroke(new BasicStroke(2.0f));
		g2d.draw(circle);

		// Bold number text centered in the circle
		String text = String.valueOf(number);
		int fontSize = (int) (diameter * FONT_RATIO);
		Font font = new Font(Font.SANS_SERIF, Font.BOLD, fontSize);
		g2d.setFont(font);
		g2d.setColor(Color.BLACK);

		FontMetrics metrics = g2d.getFontMetrics();
		int textWidth = metrics.stringWidth(text);
		int textAscent = metrics.getAscent();
		int textDescent = metrics.getDescent();
		int textX = centerX - textWidth / 2;
		int textY = centerY + (textAscent - textDescent) / 2;

		g2d.drawString(text, textX, textY);
		g2d.dispose();
	}

}
