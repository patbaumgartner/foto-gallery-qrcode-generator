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
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;

@Service
public class PdfGeneratorService {

	private static final Logger LOGGER = LoggerFactory.getLogger(PdfGeneratorService.class);

	private static final float MARGIN = 30f;

	// Conversion factor: millimetres to PDF points
	private static final float MM_TO_PT = 72f / 25.4f;

	// The card box is expanded 2 mm beyond the original 14 pt padding on every side
	private static final float BOX_EXPANSION_MM = 2f;

	private static final float CELL_PADDING = 14f - BOX_EXPANSION_MM * MM_TO_PT;

	private static final float TEXT_HEIGHT = 75f;

	// Minimum gap between the QR code and the inner box border (top and sides)
	private static final float QR_BORDER_PAD = 3f;

	// Downward vertical offset applied to the QR code image (in mm)
	private static final float QR_VERTICAL_OFFSET_MM = 2f;

	private static final float CODE_FONT_SIZE = 14f;

	private static final float EVENT_NAME_FONT_SIZE = 17f;

	private static final float EVENT_NAME_GAP = 3f + MM_TO_PT;

	private static final float CUTTING_LINE_WIDTH = 0.5f;

	private static final float CUTTING_MARK_LENGTH = 10f;

	// Back-page layout — logo takes the dominant top portion of each cell
	// (fraction of innerHeight)
	private static final float BACK_LOGO_RATIO = 0.50f;

	// Inner padding around the logo image (top + sides)
	private static final float BACK_LOGO_V_PAD = 6f;

	private static final float BACK_LOGO_H_PAD = 8f;

	// Outer card border (thin gray hairline drawn around the whole inner area)
	private static final float BACK_CARD_BORDER_WIDTH = 0.5f;

	// Hairline rules separating logo / password / url zones
	private static final float BACK_RULE_WIDTH = 0.4f;

	// Side inset for the hairline rules
	private static final float BACK_RULE_INSET = 0f;

	// Gap above/below each rule
	private static final float BACK_RULE_GAP = 3.5f;

	// Typography
	private static final float BACK_LABEL_FONT_SIZE = 10f;

	private static final float BACK_PASSWORD_FONT_SIZE = 18f;

	private static final float BACK_URL_FONT_SIZE = 12f;

	// Vertical gap between label text and password text
	private static final float BACK_LABEL_PW_GAP = 5f;

	// Minimum character count when truncating a URL with ellipsis
	private static final int MIN_URL_DISPLAY_LENGTH = 6;

	// Minimum font size used when auto-fitting text within a cell
	private static final float MIN_FONT_SIZE = 8f;

	// Horizontal margin subtracted from cell width when calculating fitting font size
	private static final float FIT_FONT_MARGIN = 4f;

	// Logo download timeouts (ms)
	private static final int LOGO_CONNECT_TIMEOUT_MS = 5000;

	private static final int LOGO_READ_TIMEOUT_MS = 10000;

	// Local resource path prefix stripped when resolving classpath resources
	private static final String RESOURCES_PREFIX = "src/main/resources/";

	// Palette: gray lines / black text
	// Pure black — for all text
	private static final float INK = 0.0f;

	// Used as non-stroking (fill) color for text; kept for backward-compatible naming
	private static final float GRAY = 0.0f;

	// Uniform gray for all drawn lines: card borders, separator rules, cutting marks
	private static final float LINE_GRAY = 0.75f;

	public int createPdf(List<GalleryCode> codes, LinkedHashMap<GalleryCode, BufferedImage> qrImages,
			PdfOptions options) throws IOException {

		int gridColumns = options.gridColumns();
		int gridRows = options.gridRows();
		boolean showCuttingLines = options.showCuttingLines();
		String eventName = options.eventName();
		Path outputPath = options.outputPath();
		String baseUrl = options.baseUrl();
		String logoUrl = options.logoUrl();
		String galleryCodeLabel = options.galleryCodeLabel();
		String galleryPasswordLabel = options.galleryPasswordLabel();

		int codesPerPage = gridColumns * gridRows;

		float pageWidth = PDRectangle.A4.getWidth();
		float pageHeight = PDRectangle.A4.getHeight();
		float usableWidth = pageWidth - 2 * MARGIN;
		float usableHeight = pageHeight - 2 * MARGIN;
		float cellWidth = usableWidth / gridColumns;
		float cellHeight = usableHeight / gridRows;
		float innerWidth = cellWidth - 2 * CELL_PADDING;
		float innerHeight = cellHeight - 2 * CELL_PADDING;
		float qrSize = Math.min(innerWidth - 2 * QR_BORDER_PAD, innerHeight - TEXT_HEIGHT - QR_BORDER_PAD);

		boolean hasEventName = eventName != null && !eventName.isBlank();
		boolean hasBackPage = true;

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
					float qrY = innerY + TEXT_HEIGHT - QR_VERTICAL_OFFSET_MM * MM_TO_PT;

					byte[] imageBytes = toByteArray(qrImage);
					PDImageXObject pdImage = PDImageXObject.createFromByteArray(document, imageBytes,
							"qr-" + code.code());

					try (PDPageContentStream content = new PDPageContentStream(document, frontPage,
							PDPageContentStream.AppendMode.APPEND, true, true)) {

						// FRONT CELL BORDER
						content.setStrokingColor(LINE_GRAY, LINE_GRAY, LINE_GRAY);
						content.setLineWidth(BACK_CARD_BORDER_WIDTH);
						content.addRect(innerX, innerY, innerWidth, innerHeight);
						content.stroke();

						content.drawImage(pdImage, qrX, qrY, qrSize, qrSize);

						String codeLabel = code.code();
						float actualCodeFontSize = fitFontSize(fontBold, codeLabel, innerWidth - FIT_FONT_MARGIN, CODE_FONT_SIZE,
								MIN_FONT_SIZE);
						float codeLabelWidth = fontBold.getStringWidth(codeLabel) / 1000f * actualCodeFontSize;
						float codeLabelX = innerX + (innerWidth - codeLabelWidth) / 2;

						float galleryCodeLabelWidth = fontRegular.getStringWidth(galleryCodeLabel) / 1000f
								* BACK_LABEL_FONT_SIZE;
						float galleryCodeLabelX = innerX + (innerWidth - galleryCodeLabelWidth) / 2;

						if (hasEventName) {
							float combinedHeight = actualCodeFontSize + BACK_LABEL_PW_GAP + BACK_LABEL_FONT_SIZE
									+ EVENT_NAME_GAP + EVENT_NAME_FONT_SIZE;
							float blockStartY = innerY + (TEXT_HEIGHT - combinedHeight) / 2;

							content.beginText();
							content.setFont(fontBold, actualCodeFontSize);
							content.newLineAtOffset(codeLabelX, blockStartY);
							content.showText(codeLabel);
							content.endText();

							float galleryCodeLabelY = blockStartY + actualCodeFontSize + BACK_LABEL_PW_GAP;
							content.beginText();
							content.setFont(fontRegular, BACK_LABEL_FONT_SIZE);
							content.newLineAtOffset(galleryCodeLabelX, galleryCodeLabelY);
							content.showText(galleryCodeLabel);
							content.endText();

							float eventNameWidth = fontRegular.getStringWidth(eventName) / 1000 * EVENT_NAME_FONT_SIZE;
							float eventNameX = innerX + (innerWidth - eventNameWidth) / 2;
							float eventNameY = galleryCodeLabelY + BACK_LABEL_FONT_SIZE + EVENT_NAME_GAP + MM_TO_PT;

							content.beginText();
							content.setFont(fontRegular, EVENT_NAME_FONT_SIZE);
							content.newLineAtOffset(eventNameX, eventNameY);
							content.showText(eventName);
							content.endText();
						} else {
							float combinedHeight = actualCodeFontSize + BACK_LABEL_PW_GAP + BACK_LABEL_FONT_SIZE;
							float blockStartY = innerY + (TEXT_HEIGHT - combinedHeight) / 2;

							content.beginText();
							content.setFont(fontBold, actualCodeFontSize);
							content.newLineAtOffset(codeLabelX, blockStartY);
							content.showText(codeLabel);
							content.endText();

							float galleryCodeLabelY = blockStartY + actualCodeFontSize + BACK_LABEL_PW_GAP;
							content.beginText();
							content.setFont(fontRegular, BACK_LABEL_FONT_SIZE);
							content.newLineAtOffset(galleryCodeLabelX, galleryCodeLabelY);
							content.showText(galleryCodeLabel);
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
								fontRegular, logoImage, baseUrl, galleryPasswordLabel);
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

	/**
	 * Draws the back of a single card cell.
	 *
	 * Layout (top → bottom): ┌─────────────────────────────────────┐ ← thin gray card
	 * border │ │ │ [LOGO — fills ~55% of height] │ ← logo centered, max size │ │ │
	 * ─────────────────────────────────── │ ← 0.4 pt gray rule │ GALLERY PASSWORD │ ← 6
	 * pt uppercase black label │ XY9G-AB7K-92QF │ ← 14 pt bold black password │
	 * ─────────────────────────────────── │ ← 0.4 pt gray rule │ my.site │ ← 6.5 pt
	 * black base URL └─────────────────────────────────────┘
	 */
	private void drawBackCell(PDDocument document, PDPage page, GalleryCode code, float innerX, float innerY,
			float innerWidth, float innerHeight, PDType1Font fontBold, PDType1Font fontRegular,
			PDImageXObject logoImage, String baseUrl, String galleryPasswordLabel) throws IOException {

		try (PDPageContentStream cs = new PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND,
				true, true)) {

			// OUTER CARD BORDER
			cs.setStrokingColor(LINE_GRAY, LINE_GRAY, LINE_GRAY);
			cs.setLineWidth(BACK_CARD_BORDER_WIDTH);
			cs.addRect(innerX, innerY, innerWidth, innerHeight);
			cs.stroke();

			// LOGO ZONE (top BACK_LOGO_RATIO of inner height)
			float logoZoneH = innerHeight * BACK_LOGO_RATIO;
			float logoZoneTopY = innerY + innerHeight;
			float logoZoneBotY = logoZoneTopY - logoZoneH;

			if (logoImage != null) {
				float logoAspect = (float) logoImage.getWidth() / logoImage.getHeight();
				float maxLogoW = innerWidth - BACK_LOGO_H_PAD * 2f;
				float maxLogoH = logoZoneH - BACK_LOGO_V_PAD * 2f;
				float logoW = maxLogoW;
				float logoH = logoW / logoAspect;
				if (logoH > maxLogoH) {
					logoH = maxLogoH;
					logoW = logoH * logoAspect;
				}
				float logoX = innerX + (innerWidth - logoW) / 2f;
				float logoY = logoZoneBotY + (logoZoneH - logoH) / 2f;
				cs.drawImage(logoImage, logoX, logoY, logoW, logoH);
			}

			// TOP RULE (below logo zone)
			float rule1Y = logoZoneBotY - BACK_RULE_GAP;
			cs.setStrokingColor(LINE_GRAY, LINE_GRAY, LINE_GRAY);
			cs.setLineWidth(BACK_RULE_WIDTH);
			cs.moveTo(innerX + BACK_RULE_INSET, rule1Y);
			cs.lineTo(innerX + innerWidth - BACK_RULE_INSET, rule1Y);
			cs.stroke();

			// PASSWORD SECTION
			// Stack: label then password, centered vertically in remaining space above
			// the bottom rule.
			float bottomRuleY = innerY + BACK_RULE_GAP * 2f
					+ (baseUrl.isBlank() ? 0f : BACK_URL_FONT_SIZE + BACK_RULE_GAP);
			float passwordSectionBotY = bottomRuleY + BACK_RULE_GAP;

			// Combined height of the password block (label + gap + password)
			String password = code.password().isBlank() ? "\u2014" : code.password();
			float actualPwFontSize = fitFontSize(fontBold, password, innerWidth - FIT_FONT_MARGIN, BACK_PASSWORD_FONT_SIZE, MIN_FONT_SIZE);
			float blockH = BACK_LABEL_FONT_SIZE + BACK_LABEL_PW_GAP + actualPwFontSize;
			float blockBotY = passwordSectionBotY + ((rule1Y - BACK_RULE_GAP - passwordSectionBotY) - blockH) / 2f;

			// password label
			String label = galleryPasswordLabel;
			float labelW = fontRegular.getStringWidth(label) / 1000f * BACK_LABEL_FONT_SIZE;
			float labelX = innerX + (innerWidth - labelW) / 2f;
			float labelY = blockBotY + actualPwFontSize + BACK_LABEL_PW_GAP;
			cs.beginText();
			cs.setNonStrokingColor(GRAY, GRAY, GRAY);
			cs.setFont(fontRegular, BACK_LABEL_FONT_SIZE);
			cs.newLineAtOffset(labelX, labelY);
			cs.showText(label);
			cs.endText();

			// Password in large bold black
			float pwW = fontBold.getStringWidth(password) / 1000f * actualPwFontSize;
			float pwX = innerX + (innerWidth - pwW) / 2f;
			cs.beginText();
			cs.setNonStrokingColor(INK, INK, INK);
			cs.setFont(fontBold, actualPwFontSize);
			cs.newLineAtOffset(pwX, blockBotY);
			cs.showText(password);
			cs.endText();

			// BOTTOM RULE
			cs.setStrokingColor(LINE_GRAY, LINE_GRAY, LINE_GRAY);
			cs.setLineWidth(BACK_RULE_WIDTH);
			cs.moveTo(innerX + BACK_RULE_INSET, bottomRuleY);
			cs.lineTo(innerX + innerWidth - BACK_RULE_INSET, bottomRuleY);
			cs.stroke();

			// BASE URL (below bottom rule)
			if (!baseUrl.isBlank()) {
				String displayUrl = truncateUrl(baseUrl, fontBold, BACK_URL_FONT_SIZE, innerWidth - FIT_FONT_MARGIN);
				float urlW = fontBold.getStringWidth(displayUrl) / 1000f * BACK_URL_FONT_SIZE;
				float urlX = innerX + (innerWidth - urlW) / 2f;
				float urlY = innerY + (bottomRuleY - innerY - BACK_URL_FONT_SIZE) / 2f + MM_TO_PT;
				cs.beginText();
				cs.setNonStrokingColor(INK, INK, INK);
				cs.setFont(fontBold, BACK_URL_FONT_SIZE);
				cs.newLineAtOffset(urlX, urlY);
				cs.showText(displayUrl);
				cs.endText();
			}
		}
	}

	/**
	 * Truncates a URL string so its rendered width fits within {@code maxWidth} points.
	 */
	private String truncateUrl(String url, PDType1Font font, float fontSize, float maxWidth) throws IOException {
		float w = font.getStringWidth(url) / 1000f * fontSize;
		if (w <= maxWidth) {
			return url;
		}
		// Strip scheme (https://) for display to save space
		String display = url.replaceFirst("^https?://", "");
		w = font.getStringWidth(display) / 1000f * fontSize;
		if (w <= maxWidth) {
			return display;
		}
		// Truncate with ellipsis (MIN_URL_DISPLAY_LENGTH ensures a readable stub
		// remains)
		while (display.length() > MIN_URL_DISPLAY_LENGTH) {
			display = display.substring(0, display.length() - 4) + "...";
			w = font.getStringWidth(display) / 1000f * fontSize;
			if (w <= maxWidth) {
				break;
			}
		}
		return display;
	}

	/**
	 * Returns the largest font size <= maxFontSize such that {@code text} renders
	 * within {@code maxWidth} points. Falls back to {@code minFontSize} if nothing fits.
	 */
	private float fitFontSize(PDType1Font font, String text, float maxWidth, float maxFontSize, float minFontSize)
			throws IOException {
		float size = maxFontSize;
		while (size > minFontSize) {
			float w = font.getStringWidth(text) / 1000f * size;
			if (w <= maxWidth) {
				break;
			}
			size -= 0.5f;
		}
		return size;
	}

	/**
	 * Downloads and decodes a logo image from an HTTP/HTTPS URL or a local file path.
	 *
	 * Supports JPEG, PNG, and WebP (via TwelveMonkeys ImageIO plugin on the class path).
	 * Other formats are attempted via ImageIO and converted to PNG as a fallback. Local
	 * paths starting with {@code src/main/resources/} are also resolved as classpath
	 * resources for portability in packaged JARs.
	 */
	private PDImageXObject loadLogoImage(PDDocument document, String logoUrl) {
		if (logoUrl.startsWith("http://") || logoUrl.startsWith("https://")) {
			return loadLogoFromHttp(document, logoUrl);
		}
		return loadLogoFromLocalPath(document, logoUrl);
	}

	private PDImageXObject loadLogoFromHttp(PDDocument document, String logoUrl) {
		URI uri;
		try {
			uri = URI.create(logoUrl);
		}
		catch (IllegalArgumentException ex) {
			LOGGER.error("Invalid logo URL '{}': {}", logoUrl, ex.getMessage(), ex);
			return null;
		}
		try {
			URLConnection connection = uri.toURL().openConnection();
			connection.setConnectTimeout(LOGO_CONNECT_TIMEOUT_MS);
			connection.setReadTimeout(LOGO_READ_TIMEOUT_MS);
			byte[] imageData;
			try (InputStream inputStream = connection.getInputStream()) {
				imageData = inputStream.readAllBytes();
			}
			return createLogoImageFromBytes(document, imageData, logoUrl);
		}
		catch (IOException ex) {
			LOGGER.error("Could not load logo from URL '{}': {}", logoUrl, ex.getMessage(), ex);
			return null;
		}
	}

	private PDImageXObject loadLogoFromLocalPath(PDDocument document, String path) {
		// Try as a filesystem path first
		Path filePath = Path.of(path);
		if (Files.isReadable(filePath)) {
			try {
				return createLogoImageFromBytes(document, Files.readAllBytes(filePath), path);
			}
			catch (IOException ex) {
				LOGGER.error("Could not read logo from file '{}': {}", path, ex.getMessage(), ex);
				return null;
			}
		}
		// Fallback: classpath resource (strip src/main/resources/ prefix for JAR
		// portability)
		String resourcePath = path.startsWith(RESOURCES_PREFIX) ? "/" + path.substring(RESOURCES_PREFIX.length())
				: (path.startsWith("/") ? path : "/" + path);
		try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
			if (is == null) {
				LOGGER.warn("Logo '{}' not found as file or classpath resource", path);
				return null;
			}
			return createLogoImageFromBytes(document, is.readAllBytes(), path);
		}
		catch (IOException ex) {
			LOGGER.error("Could not load logo from classpath '{}': {}", resourcePath, ex.getMessage(), ex);
			return null;
		}
	}

	private PDImageXObject createLogoImageFromBytes(PDDocument document, byte[] imageData, String source)
			throws IOException {
		// Try PDFBox native first (handles JPEG, PNG)
		try {
			return PDImageXObject.createFromByteArray(document, imageData, "logo");
		}
		catch (IOException | IllegalArgumentException ex) {
			// PDFBox doesn't know this format (e.g. WebP) — fall back to ImageIO,
			// which supports WebP when TwelveMonkeys imageio-webp is on the class path.
			BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(imageData));
			if (bufferedImage == null) {
				LOGGER.warn("Could not decode logo from '{}' (unsupported format)", source);
				return null;
			}
			try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
				ImageIO.write(bufferedImage, "PNG", baos);
				return PDImageXObject.createFromByteArray(document, baos.toByteArray(), "logo");
			}
		}
	}

	private void drawCuttingLines(PDDocument document, PDPage page, float pageWidth, float pageHeight, int gridColumns,
			int gridRows, float cellWidth, float cellHeight) throws IOException {
		try (PDPageContentStream content = new PDPageContentStream(document, page,
				PDPageContentStream.AppendMode.APPEND, true, true)) {
			content.setStrokingColor(LINE_GRAY, LINE_GRAY, LINE_GRAY);
			content.setLineWidth(CUTTING_LINE_WIDTH);

			// All x positions where vertical cuts happen (left boundary, column dividers,
			// right boundary)
			float[] xCuts = new float[gridColumns + 1];
			xCuts[0] = MARGIN;
			for (int col = 1; col < gridColumns; col++) {
				xCuts[col] = MARGIN + col * cellWidth;
			}
			xCuts[gridColumns] = pageWidth - MARGIN;

			// All y positions where horizontal cuts happen (top boundary, row dividers,
			// bottom boundary)
			float[] yCuts = new float[gridRows + 1];
			yCuts[0] = pageHeight - MARGIN;
			for (int row = 1; row < gridRows; row++) {
				yCuts[row] = pageHeight - MARGIN - row * cellHeight;
			}
			yCuts[gridRows] = MARGIN;

			// For each vertical cut x-position: short ticks in the top and bottom margins
			for (float x : xCuts) {
				content.moveTo(x, pageHeight - MARGIN);
				content.lineTo(x, pageHeight - MARGIN + CUTTING_MARK_LENGTH);
				content.stroke();
				content.moveTo(x, MARGIN);
				content.lineTo(x, MARGIN - CUTTING_MARK_LENGTH);
				content.stroke();
			}

			// For each horizontal cut y-position: short ticks in the left and right
			// margins
			for (float y : yCuts) {
				content.moveTo(MARGIN, y);
				content.lineTo(MARGIN - CUTTING_MARK_LENGTH, y);
				content.stroke();
				content.moveTo(pageWidth - MARGIN, y);
				content.lineTo(pageWidth - MARGIN + CUTTING_MARK_LENGTH, y);
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
