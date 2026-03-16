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
import java.nio.file.Files;
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

	// Back-page layout — logo takes the dominant top portion of each cell
	// (fraction of innerHeight)
	private static final float BACK_LOGO_RATIO = 0.55f;

	// Inner padding around the logo image (top + sides)
	private static final float BACK_LOGO_V_PAD = 6f;

	private static final float BACK_LOGO_H_PAD = 8f;

	// Outer card border (thin black hairline drawn around the whole inner area)
	private static final float BACK_CARD_BORDER_WIDTH = 0.5f;

	// Hairline rules separating logo / password / url zones
	private static final float BACK_RULE_WIDTH = 0.4f;

	// Side inset for the hairline rules
	private static final float BACK_RULE_INSET = 0f;

	// Gap above/below each rule
	private static final float BACK_RULE_GAP = 3.5f;

	// Typography
	private static final float BACK_LABEL_FONT_SIZE = 6f;

	private static final float BACK_PASSWORD_FONT_SIZE = 14f;

	private static final float BACK_URL_FONT_SIZE = 6.5f;

	// Vertical gap between label text and password text
	private static final float BACK_LABEL_PW_GAP = 3f;

	// Minimum character count when truncating a URL with ellipsis
	private static final int MIN_URL_DISPLAY_LENGTH = 6;

	// Logo download timeouts (ms)
	private static final int LOGO_CONNECT_TIMEOUT_MS = 5000;

	private static final int LOGO_READ_TIMEOUT_MS = 10000;

	// Local resource path prefix stripped when resolving classpath resources
	private static final String RESOURCES_PREFIX = "src/main/resources/";

	// ── Palette: black / white / gray only ──────────────────────────────────────
	// Pure black — for borders, password text, card border
	private static final float INK = 0.0f;

	// Medium gray — for hairline rules, label, URL
	private static final float GRAY = 0.55f;

	// Light gray — subtle rule color
	private static final float RULE_GRAY = 0.70f;

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

	/**
	 * Draws the back of a single card cell.
	 *
	 * Layout (top → bottom, pure black & white):
	 *   ┌─────────────────────────────────────┐  ← thin black card border
	 *   │                                     │
	 *   │   [LOGO — fills ~55% of height]     │  ← logo centered, max size
	 *   │                                     │
	 *   │ ─────────────────────────────────── │  ← 0.4 pt gray rule
	 *   │  GALLERY PASSWORD                   │  ← 6 pt uppercase gray label
	 *   │  XY9G-AB7K-92QF                     │  ← 14 pt bold black password
	 *   │ ─────────────────────────────────── │  ← 0.4 pt gray rule
	 *   │  mel-rohrer.ch/gallery              │  ← 6.5 pt gray URL
	 *   └─────────────────────────────────────┘
	 */
	private void drawBackCell(PDDocument document, PDPage page, GalleryCode code, float innerX, float innerY,
			float innerWidth, float innerHeight, PDType1Font fontBold, PDType1Font fontRegular,
			PDImageXObject logoImage, String galleryUrl) throws IOException {

		try (PDPageContentStream cs = new PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND,
				true, true)) {

			// ── OUTER CARD BORDER ─────────────────────────────────────────────────────
			cs.setStrokingColor(INK, INK, INK);
			cs.setLineWidth(BACK_CARD_BORDER_WIDTH);
			cs.addRect(innerX, innerY, innerWidth, innerHeight);
			cs.stroke();

			// ── LOGO ZONE (top BACK_LOGO_RATIO of inner height) ──────────────────────
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

			// ── TOP RULE (below logo zone) ────────────────────────────────────────────
			float rule1Y = logoZoneBotY - BACK_RULE_GAP;
			cs.setStrokingColor(RULE_GRAY, RULE_GRAY, RULE_GRAY);
			cs.setLineWidth(BACK_RULE_WIDTH);
			cs.moveTo(innerX + BACK_RULE_INSET, rule1Y);
			cs.lineTo(innerX + innerWidth - BACK_RULE_INSET, rule1Y);
			cs.stroke();

			// ── PASSWORD SECTION ──────────────────────────────────────────────────────
			// Stack: label then password, centered vertically in remaining space above
			// the bottom rule.
			float bottomRuleY = innerY + BACK_RULE_GAP * 2f
					+ (galleryUrl.isBlank() ? 0f : BACK_URL_FONT_SIZE + BACK_RULE_GAP);
			float passwordSectionBotY = bottomRuleY + BACK_RULE_GAP;

			// Combined height of the password block (label + gap + password)
			float blockH = BACK_LABEL_FONT_SIZE + BACK_LABEL_PW_GAP + BACK_PASSWORD_FONT_SIZE;
			float blockBotY = passwordSectionBotY + ((rule1Y - BACK_RULE_GAP - passwordSectionBotY) - blockH) / 2f;

			// "GALLERY PASSWORD" label
			String label = "GALLERY PASSWORD";
			float labelW = fontRegular.getStringWidth(label) / 1000f * BACK_LABEL_FONT_SIZE;
			float labelX = innerX + (innerWidth - labelW) / 2f;
			float labelY = blockBotY + BACK_PASSWORD_FONT_SIZE + BACK_LABEL_PW_GAP;
			cs.beginText();
			cs.setNonStrokingColor(GRAY, GRAY, GRAY);
			cs.setFont(fontRegular, BACK_LABEL_FONT_SIZE);
			cs.newLineAtOffset(labelX, labelY);
			cs.showText(label);
			cs.endText();

			// Password in large bold black
			String password = code.password().isBlank() ? "\u2014" : code.password();
			float pwW = fontBold.getStringWidth(password) / 1000f * BACK_PASSWORD_FONT_SIZE;
			float pwX = innerX + (innerWidth - pwW) / 2f;
			cs.beginText();
			cs.setNonStrokingColor(INK, INK, INK);
			cs.setFont(fontBold, BACK_PASSWORD_FONT_SIZE);
			cs.newLineAtOffset(pwX, blockBotY);
			cs.showText(password);
			cs.endText();

			// ── BOTTOM RULE ───────────────────────────────────────────────────────────
			cs.setStrokingColor(RULE_GRAY, RULE_GRAY, RULE_GRAY);
			cs.setLineWidth(BACK_RULE_WIDTH);
			cs.moveTo(innerX + BACK_RULE_INSET, bottomRuleY);
			cs.lineTo(innerX + innerWidth - BACK_RULE_INSET, bottomRuleY);
			cs.stroke();

			// ── URL (below bottom rule) ───────────────────────────────────────────────
			if (!galleryUrl.isBlank()) {
				String displayUrl = truncateUrl(galleryUrl, fontRegular, BACK_URL_FONT_SIZE, innerWidth - 8f);
				float urlW = fontRegular.getStringWidth(displayUrl) / 1000f * BACK_URL_FONT_SIZE;
				float urlX = innerX + (innerWidth - urlW) / 2f;
				float urlY = innerY + BACK_RULE_GAP;
				cs.beginText();
				cs.setNonStrokingColor(GRAY, GRAY, GRAY);
				cs.setFont(fontRegular, BACK_URL_FONT_SIZE);
				cs.newLineAtOffset(urlX, urlY);
				cs.showText(displayUrl);
				cs.endText();
			}
		}
	}

	/** Truncates a URL string so its rendered width fits within {@code maxWidth} points. */
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
		// Truncate with ellipsis (MIN_URL_DISPLAY_LENGTH ensures a readable stub remains)
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
	 * Downloads and decodes a logo image from an HTTP/HTTPS URL or a local file path.
	 *
	 * Supports JPEG, PNG, and WebP (via TwelveMonkeys ImageIO plugin on the class path).
	 * Other formats are attempted via ImageIO and converted to PNG as a fallback.
	 * Local paths starting with {@code src/main/resources/} are also resolved as
	 * classpath resources for portability in packaged JARs.
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
			LOGGER.warn("Invalid logo URL '{}': {}", logoUrl, ex.getMessage());
			return null;
		}
		try {
			java.net.URLConnection connection = uri.toURL().openConnection();
			connection.setConnectTimeout(LOGO_CONNECT_TIMEOUT_MS);
			connection.setReadTimeout(LOGO_READ_TIMEOUT_MS);
			byte[] imageData;
			try (InputStream inputStream = connection.getInputStream()) {
				imageData = inputStream.readAllBytes();
			}
			return createLogoImageFromBytes(document, imageData, logoUrl);
		}
		catch (IOException ex) {
			LOGGER.warn("Could not load logo from URL '{}': {}", logoUrl, ex.getMessage());
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
				LOGGER.warn("Could not read logo from file '{}': {}", path, ex.getMessage());
				return null;
			}
		}
		// Fallback: classpath resource (strip src/main/resources/ prefix for JAR portability)
		String resourcePath = path.startsWith(RESOURCES_PREFIX)
				? "/" + path.substring(RESOURCES_PREFIX.length())
				: (path.startsWith("/") ? path : "/" + path);
		try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
			if (is == null) {
				LOGGER.warn("Logo '{}' not found as file or classpath resource", path);
				return null;
			}
			return createLogoImageFromBytes(document, is.readAllBytes(), path);
		}
		catch (IOException ex) {
			LOGGER.warn("Could not load logo from classpath '{}': {}", resourcePath, ex.getMessage());
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
			BufferedImage bufferedImage = ImageIO.read(new java.io.ByteArrayInputStream(imageData));
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
