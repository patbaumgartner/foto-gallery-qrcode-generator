package com.fortytwotalents.fotogallery.service;

import com.fortytwotalents.fotogallery.model.GalleryCode;
import com.fortytwotalents.fotogallery.model.PdfOptions;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PdfGeneratorServiceTest {

	private PdfGeneratorService pdfService;

	private QrCodeGeneratorService qrService;

	private static final String GALLERY_URL = "https://my.site/gallery?code=";

	@TempDir
	Path tempDir;

	@BeforeEach
	void setUp() {
		pdfService = new PdfGeneratorService();
		qrService = new QrCodeGeneratorService();
	}

	@Test
	void generatesSinglePagePdfForFewCodes() throws Exception {
		List<GalleryCode> codes = createCodes("XY9G-AB7K-92QF", "TK2H-XY3M-88PL", "MN5R-ZZ99-AA11");
		LinkedHashMap<GalleryCode, BufferedImage> qrImages = generateQrImages(codes);
		Path output = tempDir.resolve("test-output.pdf");

		int pages = pdfService.createPdf(codes, qrImages, PdfOptions.of(output, 3, 4));

		assertThat(pages).isEqualTo(1);
		assertThat(output).exists();

		try (PDDocument doc = Loader.loadPDF(output.toFile())) {
			// 1 front page + 1 back page (back is always generated)
			assertThat(doc.getNumberOfPages()).isEqualTo(2);

			PDFTextStripper stripper = new PDFTextStripper();
			String text = stripper.getText(doc);
			assertThat(text).contains("XY9G-AB7K-92QF");
			assertThat(text).contains("TK2H-XY3M-88PL");
			assertThat(text).contains("MN5R-ZZ99-AA11");
		}
	}

	@Test
	void generatesMultiplePages() throws Exception {
		// 15 codes with 3x4 grid = 12 per page -> 2 front pages + 2 back pages
		List<GalleryCode> codes = createNumberedCodes(15);
		LinkedHashMap<GalleryCode, BufferedImage> qrImages = generateQrImages(codes);
		Path output = tempDir.resolve("multi-page.pdf");

		int pages = pdfService.createPdf(codes, qrImages, PdfOptions.of(output, 3, 4));

		assertThat(pages).isEqualTo(2);

		try (PDDocument doc = Loader.loadPDF(output.toFile())) {
			assertThat(doc.getNumberOfPages()).isEqualTo(4);
		}
	}

	@Test
	void generatesExactlyOneFullPage() throws Exception {
		// Exactly 12 codes with 3x4 grid = 1 full front page + 1 back page
		List<GalleryCode> codes = createNumberedCodes(12);
		LinkedHashMap<GalleryCode, BufferedImage> qrImages = generateQrImages(codes);
		Path output = tempDir.resolve("full-page.pdf");

		int pages = pdfService.createPdf(codes, qrImages, PdfOptions.of(output, 3, 4));

		assertThat(pages).isEqualTo(1);

		try (PDDocument doc = Loader.loadPDF(output.toFile())) {
			assertThat(doc.getNumberOfPages()).isEqualTo(2);
		}
	}

	@Test
	void singleCodeProducesValidPdf() throws Exception {
		List<GalleryCode> codes = createCodes("XY9G-AB7K-92QF");
		LinkedHashMap<GalleryCode, BufferedImage> qrImages = generateQrImages(codes);
		Path output = tempDir.resolve("single.pdf");

		int pages = pdfService.createPdf(codes, qrImages, PdfOptions.of(output, 3, 4));

		assertThat(pages).isEqualTo(1);
		assertThat(output).exists();
		assertThat(output.toFile().length()).isGreaterThan(0);
	}

	@Test
	void pdfUsesA4PageSize() throws Exception {
		List<GalleryCode> codes = createCodes("XY9G-AB7K-92QF");
		LinkedHashMap<GalleryCode, BufferedImage> qrImages = generateQrImages(codes);
		Path output = tempDir.resolve("a4-test.pdf");

		pdfService.createPdf(codes, qrImages, PdfOptions.of(output, 3, 4));

		try (PDDocument doc = Loader.loadPDF(output.toFile())) {
			PDPage page = doc.getPage(0);
			PDRectangle mediaBox = page.getMediaBox();
			assertThat(mediaBox.getWidth()).isEqualTo(PDRectangle.A4.getWidth());
			assertThat(mediaBox.getHeight()).isEqualTo(PDRectangle.A4.getHeight());
		}
	}

	@Test
	void pdfWithCuttingLinesIsLarger() throws Exception {
		List<GalleryCode> codes = createNumberedCodes(6);
		LinkedHashMap<GalleryCode, BufferedImage> qrImages = generateQrImages(codes);

		Path withoutLines = tempDir.resolve("no-lines.pdf");
		Path withLines = tempDir.resolve("with-lines.pdf");

		pdfService.createPdf(codes, qrImages, PdfOptions.of(withoutLines, 3, 4, false));
		pdfService.createPdf(codes, qrImages, PdfOptions.of(withLines, 3, 4, true));

		assertThat(withLines.toFile().length()).isGreaterThan(withoutLines.toFile().length());
	}

	@Test
	void pdfWithoutCuttingLinesFlagDefaultsToNoCuttingLines() throws Exception {
		List<GalleryCode> codes = createCodes("XY9G-AB7K-92QF");
		LinkedHashMap<GalleryCode, BufferedImage> qrImages = generateQrImages(codes);

		Path defaultCall = tempDir.resolve("default.pdf");
		Path explicitFalse = tempDir.resolve("explicit-false.pdf");

		pdfService.createPdf(codes, qrImages, PdfOptions.of(defaultCall, 3, 4));
		pdfService.createPdf(codes, qrImages, PdfOptions.of(explicitFalse, 3, 4, false));

		assertThat(defaultCall.toFile().length()).isEqualTo(explicitFalse.toFile().length());
	}

	@Test
	void pdfContainsEventNameWhenProvided() throws Exception {
		List<GalleryCode> codes = createCodes("XY9G-AB7K-92QF", "TK2H-XY3M-88PL");
		LinkedHashMap<GalleryCode, BufferedImage> qrImages = generateQrImages(codes);
		Path output = tempDir.resolve("event-name.pdf");

		PdfOptions options = new PdfOptions(output, 3, 4, false, "My Photo Event");
		int pages = pdfService.createPdf(codes, qrImages, options);

		assertThat(pages).isEqualTo(1);
		assertThat(output).exists();

		try (PDDocument doc = Loader.loadPDF(output.toFile())) {
			PDFTextStripper stripper = new PDFTextStripper();
			String text = stripper.getText(doc);
			assertThat(text).contains("My Photo Event");
			assertThat(text).contains("XY9G-AB7K-92QF");
			assertThat(text).contains("TK2H-XY3M-88PL");
		}
	}

	@Test
	void backPagesContainPasswordAndBaseUrl() throws Exception {
		List<GalleryCode> codes = createCodesWithPasswords("XY9G-AB7K-92QF", "PW12345", "TK2H-XY3M-88PL", "PW67890");
		LinkedHashMap<GalleryCode, BufferedImage> qrImages = generateQrImages(codes);
		Path output = tempDir.resolve("back-pages.pdf");

		PdfOptions options = new PdfOptions(output, 3, 4, false, "My Event", "https://gallery.example.com", "");
		int pages = pdfService.createPdf(codes, qrImages, options);

		// createPdf returns number of front pages only
		assertThat(pages).isEqualTo(1);

		try (PDDocument doc = Loader.loadPDF(output.toFile())) {
			// 1 front page + 1 back page
			assertThat(doc.getNumberOfPages()).isEqualTo(2);

			PDFTextStripper stripper = new PDFTextStripper();
			String text = stripper.getText(doc);
			// Passwords should appear on back page
			assertThat(text).contains("PW12345");
			assertThat(text).contains("PW67890");
			// Base URL should appear on back page
			assertThat(text).contains("gallery.example.com");
		}
	}

	@Test
	void pdfContainsGalleryCodeLabelOnFrontPage() throws Exception {
		List<GalleryCode> codes = createCodes("XY9G-AB7K-92QF", "TK2H-XY3M-88PL");
		LinkedHashMap<GalleryCode, BufferedImage> qrImages = generateQrImages(codes);
		Path output = tempDir.resolve("gallery-code-label.pdf");

		int pages = pdfService.createPdf(codes, qrImages, PdfOptions.of(output, 3, 4));

		assertThat(pages).isEqualTo(1);

		try (PDDocument doc = Loader.loadPDF(output.toFile())) {
			PDFTextStripper stripper = new PDFTextStripper();
			String text = stripper.getText(doc);
			assertThat(text).contains("GALLERY CODE");
			assertThat(text).contains("XY9G-AB7K-92QF");
			assertThat(text).contains("TK2H-XY3M-88PL");
		}
	}

	@Test
	void pdfContainsGalleryCodeLabelWithEventName() throws Exception {
		List<GalleryCode> codes = createCodes("XY9G-AB7K-92QF");
		LinkedHashMap<GalleryCode, BufferedImage> qrImages = generateQrImages(codes);
		Path output = tempDir.resolve("gallery-code-label-with-event.pdf");

		PdfOptions options = new PdfOptions(output, 3, 4, false, "My Photo Event");
		pdfService.createPdf(codes, qrImages, options);

		try (PDDocument doc = Loader.loadPDF(output.toFile())) {
			PDFTextStripper stripper = new PDFTextStripper();
			String text = stripper.getText(doc);
			assertThat(text).contains("GALLERY CODE");
			assertThat(text).contains("My Photo Event");
			assertThat(text).contains("XY9G-AB7K-92QF");
		}
	}

	@Test
	void backPagesAreAlwaysGenerated() throws Exception {
		List<GalleryCode> codes = createCodes("XY9G-AB7K-92QF");
		LinkedHashMap<GalleryCode, BufferedImage> qrImages = generateQrImages(codes);
		Path output = tempDir.resolve("always-back.pdf");

		PdfOptions options = new PdfOptions(output, 3, 4, false, "", "", "");
		pdfService.createPdf(codes, qrImages, options);

		try (PDDocument doc = Loader.loadPDF(output.toFile())) {
			// back page is always generated regardless of baseUrl / logoUrl
			assertThat(doc.getNumberOfPages()).isEqualTo(2);
		}
	}

	@Test
	void backPagesRespectMultipleQrCodePages() throws Exception {
		// 15 codes with 3x4 grid = 2 front pages → 2 front + 2 back = 4 pages total
		List<GalleryCode> codes = createNumberedCodesWithPasswords(15);
		LinkedHashMap<GalleryCode, BufferedImage> qrImages = generateQrImages(codes);
		Path output = tempDir.resolve("multi-back.pdf");

		PdfOptions options = new PdfOptions(output, 3, 4, false, "", "https://gallery.example.com", "");
		int pages = pdfService.createPdf(codes, qrImages, options);

		assertThat(pages).isEqualTo(2);

		try (PDDocument doc = Loader.loadPDF(output.toFile())) {
			assertThat(doc.getNumberOfPages()).isEqualTo(4);
		}
	}

	private List<GalleryCode> createCodes(String... codeStrings) {
		List<GalleryCode> codes = new ArrayList<>();
		for (String c : codeStrings) {
			codes.add(new GalleryCode(c));
		}
		return codes;
	}

	private List<GalleryCode> createCodesWithPasswords(String... codeAndPasswordPairs) {
		List<GalleryCode> codes = new ArrayList<>();
		for (int i = 0; i < codeAndPasswordPairs.length; i += 2) {
			codes.add(new GalleryCode(codeAndPasswordPairs[i], codeAndPasswordPairs[i + 1]));
		}
		return codes;
	}

	private List<GalleryCode> createNumberedCodes(int count) {
		List<GalleryCode> codes = new ArrayList<>();
		for (int i = 0; i < count; i++) {
			String code = "%s%s%s%s-%s%s%s%s-%s%s%s%s".formatted((char) ('E' + i % 22), (char) ('F' + i % 22),
					(char) ('4' + i % 6), (char) ('5' + i % 6), (char) ('A' + i % 26), (char) ('B' + i % 26),
					(char) ('0' + i % 10), (char) ('1' + i % 10), (char) ('C' + i % 26), (char) ('D' + i % 26),
					(char) ('2' + i % 8), (char) ('3' + i % 8));
			codes.add(new GalleryCode(code));
		}
		return codes;
	}

	private List<GalleryCode> createNumberedCodesWithPasswords(int count) {
		List<GalleryCode> codes = new ArrayList<>();
		for (int i = 0; i < count; i++) {
			String code = "%s%s%s%s-%s%s%s%s-%s%s%s%s".formatted((char) ('E' + i % 22), (char) ('F' + i % 22),
					(char) ('4' + i % 6), (char) ('5' + i % 6), (char) ('A' + i % 26), (char) ('B' + i % 26),
					(char) ('0' + i % 10), (char) ('1' + i % 10), (char) ('C' + i % 26), (char) ('D' + i % 26),
					(char) ('2' + i % 8), (char) ('3' + i % 8));
			codes.add(new GalleryCode(code, "PW" + String.format("%05d", i)));
		}
		return codes;
	}

	private LinkedHashMap<GalleryCode, BufferedImage> generateQrImages(List<GalleryCode> codes) {
		LinkedHashMap<GalleryCode, BufferedImage> images = new LinkedHashMap<>();
		int number = 1;
		for (GalleryCode code : codes) {
			images.put(code, qrService.generateQrCode(code, GALLERY_URL, 200, number++));
		}
		return images;
	}

}
