package com.fortytwotalents.fotogallery.service;

import com.fortytwotalents.fotogallery.model.GalleryCode;
import com.fortytwotalents.fotogallery.model.PdfOptions;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;

@Service
public class PdfGeneratorService {

	private static final Logger LOGGER = LoggerFactory.getLogger(PdfGeneratorService.class);

	private static final float MARGIN = 30f;

	private static final float CELL_PADDING = 14f;

	private static final float TEXT_HEIGHT = 40f;

	private static final float CODE_FONT_SIZE = 14f;

	private static final float EVENT_NAME_FONT_SIZE = 12f;

	private static final float EVENT_NAME_GAP = 3f;

	private static final float CUTTING_LINE_DASH = 4f;

	private static final float CUTTING_LINE_GAP = 4f;

	private static final float CUTTING_LINE_WIDTH = 0.5f;

	private static final float CUTTING_LINE_GRAY = 0.7f;

	// Back-page design constants
	private static final float BACK_LOGO_MAX_HEIGHT = 38f;

	private static final float BACK_LOGO_BOTTOM_PAD = 7f;

	private static final float BACK_DIVIDER_GAP = 5f;

	private static final float BACK_LABEL_FONT_SIZE = 8f;

	private static final float BACK_PASSWORD_FONT_SIZE = 14f;

	private static final float BACK_PASSWORD_BOX_PAD = 5f;

	private static final float BACK_URL_FONT_SIZE = 8f;

	// Accent blue
	private static final float ACCENT_R = 0.18f;

	private static final float ACCENT_G = 0.40f;

	private static final float ACCENT_B = 0.73f;

	// Light blue fill for password box
	private static final float BOX_FILL_R = 0.93f;

	private static final float BOX_FILL_G = 0.96f;

	private static final float BOX_FILL_B = 1.0f;

	// Dark text
	private static final float TEXT_DARK = 0.1f;

	// Gray text
	private static final float TEXT_GRAY = 0.45f;

	public int createPdf(List<GalleryCode> codes, LinkedHashMap<GalleryCode, BufferedImage> qrImages, String baseUrl,
			PdfOptions options) throws IOException {

		int gridColumns = options.gridColumns();
		int gridRows = options.gridRows();
		boolean showCuttingLines = options.showCuttingLines();
		String eventName = options.eventName();
		Path outputPath = options.outputPath();
		String galleryUrl = options.galleryUrl();
		String logoUrl = options.logoUrl();

		int codesPerPage = gridColumns * gridRows;

		float pageWidth = PDRectangle.A4.getWidth();
		float pageHeight = PDRectangle.A4.getHeight();
		float usableWidth = pageWidth - 2 * MARGIN;
		float usableHeight = pageHeight - 2 * MARGIN;
		float cellWidth = usableWidth / gridColumns;
		float cellHeight = usableHeight / gridRows;
		float innerWidth = cellWidth - 2 * CELL_PADDING;
		float innerHeight = cellHeight - 2 * CELL_PADDING;
		float qrSize = Math.min(innerWidth, innerHeight - TEXT_HEIGHT);

		boolean hasEventName = eventName != null && !eventName.isBlank();
		boolean hasBackPage = !galleryUrl.isBlank() || !logoUrl.isBlank();

		int numFrontPages = (int) Math.ceil((double) codes.size() / codesPerPage);

		try (PDDocument document = new PDDocument()) {
			PDType1Font fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
			PDType1Font fontRegular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

			// Load logo image once (if URL provided)
			PDImageXObject logoImage = null;
			if (!logoUrl.isBlank()) {
				logoImage = loadLogoImage(document, logoUrl);
			}

			for (int pageIdx = 0; pageIdx < numFrontPages; pageIdx++) {
				int startI = pageIdx * codesPerPage;
				int endI = Math.min(startI + codesPerPage, codes.size());

				// === FRONT PAGE ===
				PDPage frontPage = new PDPage(PDRectangle.A4);
				document.addPage(frontPage);

				for (int i = startI; i < endI; i++) {
					int indexOnPage = i - startI;
					int col = indexOnPage % gridColumns;
					int row = indexOnPage / gridColumns;

					GalleryCode code = codes.get(i);
					BufferedImage qrImage = qrImages.get(code);

					float cellX = MARGIN + col * cellWidth;
					float cellY = pageHeight - MARGIN - (row + 1) * cellHeight;
					float innerX = cellX + CELL_PADDING;
					float innerY = cellY + CELL_PADDING;
					float qrX = innerX + (innerWidth - qrSize) / 2;
					float qrY = innerY + TEXT_HEIGHT;

					byte[] imageBytes = toByteArray(qrImage);
					PDImageXObject pdImage = PDImageXObject.createFromByteArray(document, imageBytes,
							"qr-" + code.code());

					try (PDPageContentStream content = new PDPageContentStream(document, frontPage,
							PDPageContentStream.AppendMode.APPEND, true, true)) {
						content.drawImage(pdImage, qrX, qrY, qrSize, qrSize);

						String codeLabel = code.code();
						float codeLabelWidth = fontBold.getStringWidth(codeLabel) / 1000 * CODE_FONT_SIZE;
						float codeLabelX = innerX + (innerWidth - codeLabelWidth) / 2;
						float codeLabelY = innerY + (TEXT_HEIGHT - CODE_FONT_SIZE) / 2;

						if (hasEventName) {
							float combinedHeight = CODE_FONT_SIZE + EVENT_NAME_GAP + EVENT_NAME_FONT_SIZE;
							float blockStartY = innerY + (TEXT_HEIGHT - combinedHeight) / 2;

							content.beginText();
							content.setFont(fontBold, CODE_FONT_SIZE);
							content.newLineAtOffset(codeLabelX, blockStartY);
							content.showText(codeLabel);
							content.endText();

							float eventNameWidth = fontRegular.getStringWidth(eventName) / 1000
									* EVENT_NAME_FONT_SIZE;
							float eventNameX = innerX + (innerWidth - eventNameWidth) / 2;
							float eventNameY = blockStartY + CODE_FONT_SIZE + EVENT_NAME_GAP;

							content.beginText();
							content.setFont(fontRegular, EVENT_NAME_FONT_SIZE);
							content.newLineAtOffset(eventNameX, eventNameY);
							content.showText(eventName);
							content.endText();
						}
						else {
							content.beginText();
							content.setFont(fontBold, CODE_FONT_SIZE);
							content.newLineAtOffset(codeLabelX, codeLabelY);
							content.showText(codeLabel);
							content.endText();
						}
					}
				}

				// === BACK PAGE (for duplex printing — mirrored horizontally) ===
				if (hasBackPage) {
					PDPage backPage = new PDPage(PDRectangle.A4);
					document.addPage(backPage);

					for (int i = startI; i < endI; i++) {
						int indexOnPage = i - startI;
						int col = indexOnPage % gridColumns;
						int row = indexOnPage / gridColumns;
						int mirroredCol = gridColumns - 1 - col;

						GalleryCode code = codes.get(i);

						float cellX = MARGIN + mirroredCol * cellWidth;
						float cellY = pageHeight - MARGIN - (row + 1) * cellHeight;
						float innerX = cellX + CELL_PADDING;
						float innerY = cellY + CELL_PADDING;

						drawBackCell(document, backPage, code, innerX, innerY, innerWidth, innerHeight, fontBold,
								fontRegular, logoImage, galleryUrl);
					}
				}
			}

			if (showCuttingLines) {
				for (int p = 0; p < document.getNumberOfPages(); p++) {
					PDPage page = document.getPage(p);
					drawCuttingLines(document, page, pageWidth, pageHeight, gridColumns, gridRows, cellWidth,
							cellHeight);
				}
			}

			document.save(outputPath.toFile());
		}

		LOGGER.atInfo()
			.addArgument(outputPath)
			.addArgument(numFrontPages)
			.addArgument(() -> codes.size())
			.log("Generated PDF: {} ({} pages, {} codes)");
		return numFrontPages;
	}

	private void drawBackCell(PDDocument document, PDPage page, GalleryCode code, float innerX, float innerY,
			float innerWidth, float innerHeight, PDType1Font fontBold, PDType1Font fontRegular,
			PDImageXObject logoImage, String galleryUrl) throws IOException {

		try (PDPageContentStream content = new PDPageContentStream(document, page,
				PDPageContentStream.AppendMode.APPEND, true, true)) {

			// Start from the top of the inner area and work downwards
			float topY = innerY + innerHeight;

			// ---- LOGO ----
			if (logoImage != null) {
				float logoAspect = (float) logoImage.getWidth() / logoImage.getHeight();
				float logoH = Math.min(BACK_LOGO_MAX_HEIGHT, innerHeight * 0.28f);
				float logoW = logoAspect * logoH;
				if (logoW > innerWidth) {
					logoW = innerWidth;
					logoH = logoW / logoAspect;
				}
				float logoX = innerX + (innerWidth - logoW) / 2;
				content.drawImage(logoImage, logoX, topY - logoH, logoW, logoH);
				topY -= logoH + BACK_LOGO_BOTTOM_PAD;
			}

			// ---- TOP DIVIDER ----
			topY -= BACK_DIVIDER_GAP;
			content.setStrokingColor(ACCENT_R, ACCENT_G, ACCENT_B);
			content.setLineWidth(0.8f);
			content.moveTo(innerX, topY);
			content.lineTo(innerX + innerWidth, topY);
			content.stroke();
			topY -= BACK_DIVIDER_GAP;

			// ---- "Gallery Password:" LABEL ----
			topY -= BACK_LABEL_FONT_SIZE;
			String label = "Gallery Password:";
			float labelWidth = fontRegular.getStringWidth(label) / 1000 * BACK_LABEL_FONT_SIZE;
			float labelX = innerX + (innerWidth - labelWidth) / 2;
			content.beginText();
			content.setNonStrokingColor(TEXT_GRAY, TEXT_GRAY, TEXT_GRAY);
			content.setFont(fontRegular, BACK_LABEL_FONT_SIZE);
			content.newLineAtOffset(labelX, topY);
			content.showText(label);
			content.endText();
			topY -= 5f;

			// ---- PASSWORD BOX ----
			String password = code.password().isBlank() ? "\u2014" : code.password();
			float pwWidth = fontBold.getStringWidth(password) / 1000 * BACK_PASSWORD_FONT_SIZE;
			float boxW = Math.min(pwWidth + BACK_PASSWORD_BOX_PAD * 4, innerWidth - 6);
			float boxH = BACK_PASSWORD_FONT_SIZE + BACK_PASSWORD_BOX_PAD * 2;
			float boxX = innerX + (innerWidth - boxW) / 2;
			float boxY = topY - boxH;

			// Filled background
			content.setNonStrokingColor(BOX_FILL_R, BOX_FILL_G, BOX_FILL_B);
			content.addRect(boxX, boxY, boxW, boxH);
			content.fill();

			// Border
			content.setStrokingColor(ACCENT_R, ACCENT_G, ACCENT_B);
			content.setLineWidth(0.7f);
			content.addRect(boxX, boxY, boxW, boxH);
			content.stroke();

			// Password text
			float pwX = innerX + (innerWidth - pwWidth) / 2;
			float pwTextY = boxY + BACK_PASSWORD_BOX_PAD;
			content.beginText();
			content.setNonStrokingColor(TEXT_DARK, TEXT_DARK, TEXT_DARK);
			content.setFont(fontBold, BACK_PASSWORD_FONT_SIZE);
			content.newLineAtOffset(pwX, pwTextY);
			content.showText(password);
			content.endText();

			topY = boxY - 5f;

			// ---- BOTTOM DIVIDER ----
			topY -= BACK_DIVIDER_GAP;
			content.setStrokingColor(ACCENT_R, ACCENT_G, ACCENT_B);
			content.setLineWidth(0.8f);
			content.moveTo(innerX, topY);
			content.lineTo(innerX + innerWidth, topY);
			content.stroke();
			topY -= BACK_DIVIDER_GAP;

			// ---- GALLERY URL ----
			if (!galleryUrl.isBlank()) {
				String displayUrl = galleryUrl;
				float urlWidth = fontRegular.getStringWidth(displayUrl) / 1000 * BACK_URL_FONT_SIZE;
				while (urlWidth > innerWidth && displayUrl.length() > 10) {
					displayUrl = displayUrl.substring(0, displayUrl.length() - 4) + "...";
					urlWidth = fontRegular.getStringWidth(displayUrl) / 1000 * BACK_URL_FONT_SIZE;
				}
				topY -= BACK_URL_FONT_SIZE;
				float urlX = innerX + (innerWidth - urlWidth) / 2;
				content.beginText();
				content.setNonStrokingColor(ACCENT_R, ACCENT_G, ACCENT_B);
				content.setFont(fontRegular, BACK_URL_FONT_SIZE);
				content.newLineAtOffset(urlX, topY);
				content.showText(displayUrl);
				content.endText();
			}
		}
	}

	private PDImageXObject loadLogoImage(PDDocument document, String logoUrl) {
		try {
			URL url = URI.create(logoUrl).toURL();
			byte[] imageData;
			try (InputStream inputStream = url.openStream()) {
				imageData = inputStream.readAllBytes();
			}
			// Try PDFBox direct load first (handles JPEG, PNG, etc.)
			try {
				return PDImageXObject.createFromByteArray(document, imageData, "logo");
			}
			catch (Exception ex) {
				// Fall back to ImageIO for other formats
				BufferedImage bufferedImage = ImageIO.read(new java.io.ByteArrayInputStream(imageData));
				if (bufferedImage == null) {
					LOGGER.warn("Could not decode logo image from URL '{}' (unsupported format)", logoUrl);
					return null;
				}
				try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
					ImageIO.write(bufferedImage, "PNG", baos);
					return PDImageXObject.createFromByteArray(document, baos.toByteArray(), "logo");
				}
			}
		}
		catch (Exception ex) {
			LOGGER.warn("Could not load logo from URL '{}': {}", logoUrl, ex.getMessage());
			return null;
		}
	}

	private void drawCuttingLines(PDDocument document, PDPage page, float pageWidth, float pageHeight, int gridColumns,
			int gridRows, float cellWidth, float cellHeight) throws IOException {
		try (PDPageContentStream content = new PDPageContentStream(document, page,
				PDPageContentStream.AppendMode.APPEND, true, true)) {
			content.setStrokingColor(CUTTING_LINE_GRAY, CUTTING_LINE_GRAY, CUTTING_LINE_GRAY);
			content.setLineWidth(CUTTING_LINE_WIDTH);
			content.setLineDashPattern(new float[]{CUTTING_LINE_DASH, CUTTING_LINE_GAP}, 0);

			// Vertical lines between columns
			for (int col = 1; col < gridColumns; col++) {
				float x = MARGIN + col * cellWidth;
				content.moveTo(x, MARGIN);
				content.lineTo(x, pageHeight - MARGIN);
				content.stroke();
			}

			// Horizontal lines between rows
			for (int row = 1; row < gridRows; row++) {
				float y = pageHeight - MARGIN - row * cellHeight;
				content.moveTo(MARGIN, y);
				content.lineTo(pageWidth - MARGIN, y);
				content.stroke();
			}
		}
	}

	private byte[] toByteArray(BufferedImage image) throws IOException {
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
			ImageIO.write(image, "PNG", baos);
			return baos.toByteArray();
		}
	}

}
