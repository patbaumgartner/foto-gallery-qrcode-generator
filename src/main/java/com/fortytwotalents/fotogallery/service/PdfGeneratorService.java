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

	// Back-page layout proportions (fraction of innerHeight)
	private static final float BACK_HEADER_RATIO = 0.42f;

	private static final float BACK_FOOTER_RATIO = 0.18f;

	private static final float BACK_STRIPE_HEIGHT = 2f;

	// Back-page logo sizing
	private static final float BACK_LOGO_VERTICAL_PADDING = 14f;

	private static final float BACK_LOGO_HORIZONTAL_PADDING = 16f;

	// Back-page typography
	private static final float BACK_LABEL_FONT_SIZE = 7.5f;

	// Label Y position within body zone (fraction from top)
	private static final float BACK_LABEL_Y_RATIO = 0.24f;

	private static final float BACK_PASSWORD_FONT_SIZE = 13f;

	private static final float BACK_PASSWORD_BOX_PAD_H = 8f;

	private static final float BACK_PASSWORD_BOX_PAD_V = 5f;

	// Password box: min side margin so the box never fills the full inner width
	private static final float BACK_PASSWORD_BOX_MARGIN = 10f;

	// Password box center Y position within body zone (fraction from top)
	private static final float BACK_PASSWORD_BOX_Y_RATIO = 0.65f;

	private static final float BACK_PASSWORD_BOX_BORDER_WIDTH = 0.8f;

	private static final float BACK_URL_FONT_SIZE = 7f;

	// Left+right margin kept clear when fitting the URL in the footer
	private static final float BACK_URL_HORIZONTAL_MARGIN = 8f;

	// Minimum character count when truncating a URL with ellipsis
	private static final int MIN_URL_DISPLAY_LENGTH = 6;

	// Logo download timeouts (ms)
	private static final int LOGO_CONNECT_TIMEOUT_MS = 5000;

	private static final int LOGO_READ_TIMEOUT_MS = 10000;

	// Accent blue (header / dividers / URL)
	private static final float ACCENT_R = 0.18f;

	private static final float ACCENT_G = 0.40f;

	private static final float ACCENT_B = 0.73f;

	// Pale blue fill for password box and footer
	private static final float PALE_R = 0.93f;

	private static final float PALE_G = 0.96f;

	private static final float PALE_B = 1.0f;

	// Header section background (very light warm gray — lets logo pop)
	private static final float HEADER_BG_R = 0.97f;

	private static final float HEADER_BG_G = 0.97f;

	private static final float HEADER_BG_B = 0.97f;

	// Dark text
	private static final float TEXT_DARK = 0.10f;

	// Medium gray for labels
	private static final float TEXT_GRAY = 0.50f;

	// White
	private static final float WHITE = 1.0f;

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
	 * Layout (top → bottom within the inner cell area):
	 *   ┌──────────────────────────────┐
	 *   │  HEADER BAND (light gray bg)  │  ← logo centered here
	 *   ├══════════════════════════════╡  ← 2pt accent stripe
	 *   │        "Gallery Password"     │  ← small gray label
	 *   │   ╔══════════════════════╗   │
	 *   │   ║   A B C 1 2 3 4     ║   │  ← password box (pale-blue bg)
	 *   │   ╚══════════════════════╝   │
	 *   ├══════════════════════════════╡  ← 2pt accent stripe
	 *   │   https://gallery.example    │  ← footer (pale-blue bg), URL text
	 *   └──────────────────────────────┘
	 */
	private void drawBackCell(PDDocument document, PDPage page, GalleryCode code, float innerX, float innerY,
			float innerWidth, float innerHeight, PDType1Font fontBold, PDType1Font fontRegular,
			PDImageXObject logoImage, String galleryUrl) throws IOException {

		float headerH = innerHeight * BACK_HEADER_RATIO;
		float footerH = innerHeight * BACK_FOOTER_RATIO;

		float headerTopY = innerY + innerHeight;
		float headerBotY = headerTopY - headerH;
		float footerTopY = innerY + footerH;

		try (PDPageContentStream cs = new PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND,
				true, true)) {

			// ── HEADER BAND background (light gray) ──────────────────────────────────
			cs.setNonStrokingColor(HEADER_BG_R, HEADER_BG_G, HEADER_BG_B);
			cs.addRect(innerX, headerBotY, innerWidth, headerH);
			cs.fill();

			// ── FOOTER BAND background (pale blue) ───────────────────────────────────
			cs.setNonStrokingColor(PALE_R, PALE_G, PALE_B);
			cs.addRect(innerX, innerY, innerWidth, footerH);
			cs.fill();

			// ── TOP ACCENT STRIPE (top edge of header) ───────────────────────────────
			cs.setNonStrokingColor(ACCENT_R, ACCENT_G, ACCENT_B);
			cs.addRect(innerX, headerTopY - BACK_STRIPE_HEIGHT, innerWidth, BACK_STRIPE_HEIGHT);
			cs.fill();

			// ── HEADER–BODY SEPARATOR STRIPE ─────────────────────────────────────────
			cs.setNonStrokingColor(ACCENT_R, ACCENT_G, ACCENT_B);
			cs.addRect(innerX, headerBotY, innerWidth, BACK_STRIPE_HEIGHT);
			cs.fill();

			// ── BODY–FOOTER SEPARATOR STRIPE ─────────────────────────────────────────
			cs.setNonStrokingColor(ACCENT_R, ACCENT_G, ACCENT_B);
			cs.addRect(innerX, footerTopY - BACK_STRIPE_HEIGHT, innerWidth, BACK_STRIPE_HEIGHT);
			cs.fill();

			// ── BOTTOM ACCENT STRIPE ──────────────────────────────────────────────────
			cs.setNonStrokingColor(ACCENT_R, ACCENT_G, ACCENT_B);
			cs.addRect(innerX, innerY, innerWidth, BACK_STRIPE_HEIGHT);
			cs.fill();

			// ── LOGO (centered in header band) ───────────────────────────────────────
			if (logoImage != null) {
				float logoAspect = (float) logoImage.getWidth() / logoImage.getHeight();
				float maxLogoH = headerH - BACK_LOGO_VERTICAL_PADDING;
				float maxLogoW = innerWidth - BACK_LOGO_HORIZONTAL_PADDING;
				float logoH = maxLogoH;
				float logoW = logoAspect * logoH;
				if (logoW > maxLogoW) {
					logoW = maxLogoW;
					logoH = logoW / logoAspect;
				}
				float logoX = innerX + (innerWidth - logoW) / 2f;
				float logoY = headerBotY + (headerH - logoH) / 2f;
				cs.drawImage(logoImage, logoX, logoY, logoW, logoH);
			}

			// ── PASSWORD SECTION ──────────────────────────────────────────────────────
			// Body zone sits between the two separator stripes
			float bodyTopY = headerBotY - BACK_STRIPE_HEIGHT;
			float bodyBotY = footerTopY;
			float bodyH = bodyTopY - bodyBotY;

			// "Gallery Password" label
			float labelY = bodyTopY - bodyH * BACK_LABEL_Y_RATIO;
			String label = "Gallery Password";
			float labelW = fontRegular.getStringWidth(label) / 1000f * BACK_LABEL_FONT_SIZE;
			float labelX = innerX + (innerWidth - labelW) / 2f;
			cs.beginText();
			cs.setNonStrokingColor(TEXT_GRAY, TEXT_GRAY, TEXT_GRAY);
			cs.setFont(fontRegular, BACK_LABEL_FONT_SIZE);
			cs.newLineAtOffset(labelX, labelY);
			cs.showText(label);
			cs.endText();

			// Password box — centered within the body zone
			String password = code.password().isBlank() ? "\u2014" : code.password();
			float pwW = fontBold.getStringWidth(password) / 1000f * BACK_PASSWORD_FONT_SIZE;
			float boxW = Math.min(pwW + BACK_PASSWORD_BOX_PAD_H * 2f, innerWidth - BACK_PASSWORD_BOX_MARGIN);
			float boxH = BACK_PASSWORD_FONT_SIZE + BACK_PASSWORD_BOX_PAD_V * 2f;
			float boxX = innerX + (innerWidth - boxW) / 2f;
			float boxCenterY = bodyTopY - bodyH * BACK_PASSWORD_BOX_Y_RATIO;
			float boxY = boxCenterY - boxH / 2f;

			// Box fill
			cs.setNonStrokingColor(PALE_R, PALE_G, PALE_B);
			cs.addRect(boxX, boxY, boxW, boxH);
			cs.fill();
			// Box border
			cs.setStrokingColor(ACCENT_R, ACCENT_G, ACCENT_B);
			cs.setLineWidth(BACK_PASSWORD_BOX_BORDER_WIDTH);
			cs.addRect(boxX, boxY, boxW, boxH);
			cs.stroke();
			// Password text
			float pwX = innerX + (innerWidth - pwW) / 2f;
			float pwTextY = boxY + BACK_PASSWORD_BOX_PAD_V;
			cs.beginText();
			cs.setNonStrokingColor(TEXT_DARK, TEXT_DARK, TEXT_DARK);
			cs.setFont(fontBold, BACK_PASSWORD_FONT_SIZE);
			cs.newLineAtOffset(pwX, pwTextY);
			cs.showText(password);
			cs.endText();

			// ── FOOTER URL ────────────────────────────────────────────────────────────
			if (!galleryUrl.isBlank()) {
				String displayUrl = truncateUrl(galleryUrl, fontRegular, BACK_URL_FONT_SIZE,
						innerWidth - BACK_URL_HORIZONTAL_MARGIN);
				float urlW = fontRegular.getStringWidth(displayUrl) / 1000f * BACK_URL_FONT_SIZE;
				float urlX = innerX + (innerWidth - urlW) / 2f;
				float urlY = innerY + (footerH - BACK_STRIPE_HEIGHT - BACK_URL_FONT_SIZE) / 2f;
				cs.beginText();
				cs.setNonStrokingColor(ACCENT_R, ACCENT_G, ACCENT_B);
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
	 * Downloads and decodes a logo image from an HTTP/HTTPS URL.
	 *
	 * Supports JPEG, PNG, and WebP (via TwelveMonkeys ImageIO plugin on the class path).
	 * Other formats are attempted via ImageIO and converted to PNG as a fallback.
	 */
	private PDImageXObject loadLogoImage(PDDocument document, String logoUrl) {
		URI uri;
		try {
			uri = URI.create(logoUrl);
		}
		catch (IllegalArgumentException ex) {
			LOGGER.warn("Invalid logo URL '{}': {}", logoUrl, ex.getMessage());
			return null;
		}
		String scheme = uri.getScheme();
		if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
			LOGGER.warn("Logo URL '{}' uses unsupported scheme '{}' (only http/https allowed)", logoUrl, scheme);
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
			// Try PDFBox native first (handles JPEG, PNG)
			try {
				return PDImageXObject.createFromByteArray(document, imageData, "logo");
			}
			catch (IOException | IllegalArgumentException ex) {
				// PDFBox doesn't know this format (e.g. WebP) — fall back to ImageIO,
				// which supports WebP when TwelveMonkeys imageio-webp is on the class path.
				BufferedImage bufferedImage = ImageIO.read(new java.io.ByteArrayInputStream(imageData));
				if (bufferedImage == null) {
					LOGGER.warn("Could not decode logo from URL '{}' (unsupported format)", logoUrl);
					return null;
				}
				try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
					ImageIO.write(bufferedImage, "PNG", baos);
					return PDImageXObject.createFromByteArray(document, baos.toByteArray(), "logo");
				}
			}
		}
		catch (IOException ex) {
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
