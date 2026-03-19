package com.fortytwotalents.fotogallery.runner;

import com.fortytwotalents.fotogallery.config.AppProperties;
import com.fortytwotalents.fotogallery.model.CsvReadResult;
import com.fortytwotalents.fotogallery.model.GalleryCode;
import com.fortytwotalents.fotogallery.model.PdfOptions;
import com.fortytwotalents.fotogallery.service.CodeGeneratorService;
import com.fortytwotalents.fotogallery.service.CsvReaderService;
import com.fortytwotalents.fotogallery.service.CsvWriterService;
import com.fortytwotalents.fotogallery.service.PdfGeneratorService;
import com.fortytwotalents.fotogallery.service.PicPeakService;
import com.fortytwotalents.fotogallery.service.QrCodeGeneratorService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Scanner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InteractiveRunnerTest {

	@Mock
	CodeGeneratorService codeGeneratorService;

	@Mock
	CsvWriterService csvWriterService;

	@Mock
	CsvReaderService csvReaderService;

	@Mock
	QrCodeGeneratorService qrCodeGeneratorService;

	@Mock
	PdfGeneratorService pdfGeneratorService;

	@Mock
	PicPeakService picPeakService;

	@Test
	void skipsWhenModeIsAlreadySet() throws Exception {
		AppProperties props = new AppProperties("generate-codes", "codes.csv", "codes.csv", "qr-codes.pdf",
				"https://my.site", 200, 3, 4, "XY9G", 50, false, "", "", "", null, null);
		InteractiveRunner runner = new InteractiveRunner(props, codeGeneratorService, csvWriterService,
				csvReaderService, qrCodeGeneratorService, pdfGeneratorService);

		runner.run(new DefaultApplicationArguments());

		verify(codeGeneratorService, never()).generateCodes(anyString(), anyInt());
	}

	@Test
	void promptModeAcceptsNumericChoice() {
		AppProperties props = new AppProperties("", "codes.csv", "codes.csv", "qr-codes.pdf", "https://my.site", 200, 3,
				4, "", 50, false, "", "", "", null, null);
		InteractiveRunner runner = new InteractiveRunner(props, codeGeneratorService, csvWriterService,
				csvReaderService, qrCodeGeneratorService, pdfGeneratorService);

		Scanner scanner = scannerFrom("1\n");
		assertThat(runner.promptMode(scanner)).isEqualTo("generate-codes");

		scanner = scannerFrom("2\n");
		assertThat(runner.promptMode(scanner)).isEqualTo("generate-pdf");

		scanner = scannerFrom("3\n");
		assertThat(runner.promptMode(scanner)).isEqualTo("both");
	}

	@Test
	void promptModeAcceptsNamedChoice() {
		AppProperties props = new AppProperties("", "codes.csv", "codes.csv", "qr-codes.pdf", "https://my.site", 200, 3,
				4, "", 50, false, "", "", "", null, null);
		InteractiveRunner runner = new InteractiveRunner(props, codeGeneratorService, csvWriterService,
				csvReaderService, qrCodeGeneratorService, pdfGeneratorService);

		Scanner scanner = scannerFrom("generate-codes\n");
		assertThat(runner.promptMode(scanner)).isEqualTo("generate-codes");

		scanner = scannerFrom("generate-pdf\n");
		assertThat(runner.promptMode(scanner)).isEqualTo("generate-pdf");

		scanner = scannerFrom("both\n");
		assertThat(runner.promptMode(scanner)).isEqualTo("both");
	}

	@Test
	void promptModeDefaultsToBothOnBlankInput() {
		AppProperties props = new AppProperties("", "codes.csv", "codes.csv", "qr-codes.pdf", "https://my.site", 200, 3,
				4, "", 50, false, "", "", "", null, null);
		InteractiveRunner runner = new InteractiveRunner(props, codeGeneratorService, csvWriterService,
				csvReaderService, qrCodeGeneratorService, pdfGeneratorService);

		Scanner scanner = scannerFrom("\n");
		assertThat(runner.promptMode(scanner)).isEqualTo("both");
	}

	@Test
	void promptModeRetriesOnInvalidInput() {
		AppProperties props = new AppProperties("", "codes.csv", "codes.csv", "qr-codes.pdf", "https://my.site", 200, 3,
				4, "", 50, false, "", "", "", null, null);
		InteractiveRunner runner = new InteractiveRunner(props, codeGeneratorService, csvWriterService,
				csvReaderService, qrCodeGeneratorService, pdfGeneratorService);

		Scanner scanner = scannerFrom("invalid\n2\n");
		assertThat(runner.promptMode(scanner)).isEqualTo("generate-pdf");
	}

	@Test
	void promptRequiredReturnsValue() {
		AppProperties props = new AppProperties("", "codes.csv", "codes.csv", "qr-codes.pdf", "https://my.site", 200, 3,
				4, "", 50, false, "", "", "", null, null);
		InteractiveRunner runner = new InteractiveRunner(props, codeGeneratorService, csvWriterService,
				csvReaderService, qrCodeGeneratorService, pdfGeneratorService);

		Scanner scanner = scannerFrom("XY9G\n");
		assertThat(runner.promptRequired(scanner, "Event code")).isEqualTo("XY9G");
	}

	@Test
	void promptRequiredRetriesOnBlankInput() {
		AppProperties props = new AppProperties("", "codes.csv", "codes.csv", "qr-codes.pdf", "https://my.site", 200, 3,
				4, "", 50, false, "", "", "", null, null);
		InteractiveRunner runner = new InteractiveRunner(props, codeGeneratorService, csvWriterService,
				csvReaderService, qrCodeGeneratorService, pdfGeneratorService);

		Scanner scanner = scannerFrom("\nAB1C\n");
		assertThat(runner.promptRequired(scanner, "Event code")).isEqualTo("AB1C");
	}

	@Test
	void promptEventCodeAcceptsValidCode() {
		AppProperties props = new AppProperties("", "codes.csv", "codes.csv", "qr-codes.pdf", "https://my.site", 200, 3,
				4, "", 50, false, "", "", "", null, null);
		InteractiveRunner runner = new InteractiveRunner(props, codeGeneratorService, csvWriterService,
				csvReaderService, qrCodeGeneratorService, pdfGeneratorService);

		Scanner scanner = scannerFrom("XY9G\n");
		assertThat(runner.promptEventCode(scanner)).isEqualTo("XY9G");
	}

	@Test
	void promptEventCodeNormalizesToUpperCase() {
		AppProperties props = new AppProperties("", "codes.csv", "codes.csv", "qr-codes.pdf", "https://my.site", 200, 3,
				4, "", 50, false, "", "", "", null, null);
		InteractiveRunner runner = new InteractiveRunner(props, codeGeneratorService, csvWriterService,
				csvReaderService, qrCodeGeneratorService, pdfGeneratorService);

		Scanner scanner = scannerFrom("ab1c\n");
		assertThat(runner.promptEventCode(scanner)).isEqualTo("AB1C");
	}

	@Test
	void promptEventCodeRetriesOnTooShort() {
		AppProperties props = new AppProperties("", "codes.csv", "codes.csv", "qr-codes.pdf", "https://my.site", 200, 3,
				4, "", 50, false, "", "", "", null, null);
		InteractiveRunner runner = new InteractiveRunner(props, codeGeneratorService, csvWriterService,
				csvReaderService, qrCodeGeneratorService, pdfGeneratorService);

		Scanner scanner = scannerFrom("abc\nXY9G\n");
		assertThat(runner.promptEventCode(scanner)).isEqualTo("XY9G");
	}

	@Test
	void promptEventCodeRetriesOnTooLong() {
		AppProperties props = new AppProperties("", "codes.csv", "codes.csv", "qr-codes.pdf", "https://my.site", 200, 3,
				4, "", 50, false, "", "", "", null, null);
		InteractiveRunner runner = new InteractiveRunner(props, codeGeneratorService, csvWriterService,
				csvReaderService, qrCodeGeneratorService, pdfGeneratorService);

		Scanner scanner = scannerFrom("ABCDE\nXY9G\n");
		assertThat(runner.promptEventCode(scanner)).isEqualTo("XY9G");
	}

	@Test
	void promptEventCodeUsesRandomDefaultOnBlankInput() {
		AppProperties props = new AppProperties("", "codes.csv", "codes.csv", "qr-codes.pdf", "https://my.site", 200, 3,
				4, "", 50, false, "", "", "", null, null);
		InteractiveRunner runner = new InteractiveRunner(props, codeGeneratorService, csvWriterService,
				csvReaderService, qrCodeGeneratorService, pdfGeneratorService);

		Scanner scanner = scannerFrom("\n");
		String result = runner.promptEventCode(scanner);
		assertThat(result).matches("[A-Z0-9]{4}");
	}

	@Test
	void promptOptionalReturnsDefaultOnBlankInput() {
		AppProperties props = new AppProperties("", "codes.csv", "codes.csv", "qr-codes.pdf", "https://my.site", 200, 3,
				4, "", 50, false, "", "", "", null, null);
		InteractiveRunner runner = new InteractiveRunner(props, codeGeneratorService, csvWriterService,
				csvReaderService, qrCodeGeneratorService, pdfGeneratorService);

		Scanner scanner = scannerFrom("\n");
		assertThat(runner.promptOptional(scanner, "Event name", "default-name")).isEqualTo("default-name");
	}

	@Test
	void promptOptionalReturnsProvidedValue() {
		AppProperties props = new AppProperties("", "codes.csv", "codes.csv", "qr-codes.pdf", "https://my.site", 200, 3,
				4, "", 50, false, "", "", "", null, null);
		InteractiveRunner runner = new InteractiveRunner(props, codeGeneratorService, csvWriterService,
				csvReaderService, qrCodeGeneratorService, pdfGeneratorService);

		Scanner scanner = scannerFrom("My Event\n");
		assertThat(runner.promptOptional(scanner, "Event name", "default-name")).isEqualTo("My Event");
	}

	@Test
	void promptIntReturnsDefaultOnBlankInput() {
		AppProperties props = new AppProperties("", "codes.csv", "codes.csv", "qr-codes.pdf", "https://my.site", 200, 3,
				4, "", 50, false, "", "", "", null, null);
		InteractiveRunner runner = new InteractiveRunner(props, codeGeneratorService, csvWriterService,
				csvReaderService, qrCodeGeneratorService, pdfGeneratorService);

		Scanner scanner = scannerFrom("\n");
		assertThat(runner.promptInt(scanner, "Code count", 50)).isEqualTo(50);
	}

	@Test
	void promptIntReturnsProvidedValue() {
		AppProperties props = new AppProperties("", "codes.csv", "codes.csv", "qr-codes.pdf", "https://my.site", 200, 3,
				4, "", 50, false, "", "", "", null, null);
		InteractiveRunner runner = new InteractiveRunner(props, codeGeneratorService, csvWriterService,
				csvReaderService, qrCodeGeneratorService, pdfGeneratorService);

		Scanner scanner = scannerFrom("100\n");
		assertThat(runner.promptInt(scanner, "Code count", 50)).isEqualTo(100);
	}

	@Test
	void promptIntReturnsDefaultOnInvalidInput() {
		AppProperties props = new AppProperties("", "codes.csv", "codes.csv", "qr-codes.pdf", "https://my.site", 200, 3,
				4, "", 50, false, "", "", "", null, null);
		InteractiveRunner runner = new InteractiveRunner(props, codeGeneratorService, csvWriterService,
				csvReaderService, qrCodeGeneratorService, pdfGeneratorService);

		Scanner scanner = scannerFrom("not-a-number\n");
		assertThat(runner.promptInt(scanner, "Code count", 50)).isEqualTo(50);
	}

	@Test
	void runsGenerateCodesWhenModeBlankAndUserSelectsGenerateCodes() throws Exception {
		AppProperties props = new AppProperties("", "codes.csv", "codes.csv", "qr-codes.pdf", "https://my.site", 200, 3,
				4, "", 50, false, "", "", "", null, null);
		InteractiveRunner runner = new InteractiveRunner(props, codeGeneratorService, csvWriterService,
				csvReaderService, qrCodeGeneratorService, pdfGeneratorService);

		when(codeGeneratorService.generateCodes(anyString(), anyInt()))
			.thenReturn(List.of(new GalleryCode("XY9G-AB7K-92QF")));

		// User selects "generate-codes", enters event code "XY9G", accepts all defaults
		ByteArrayInputStream input = new ByteArrayInputStream("1\nXY9G\n\n\n\n".getBytes(StandardCharsets.UTF_8));
		System.setIn(input);

		runner.run(new DefaultApplicationArguments());

		verify(codeGeneratorService).generateCodes(anyString(), anyInt());
		verify(csvWriterService).writeCodes(any(), any(), any(), any());
	}

	@Test
	void doesNotAskForEventCodeIfAlreadyProvided() throws Exception {
		AppProperties props = new AppProperties("", "codes.csv", "codes.csv", "qr-codes.pdf", "https://my.site", 200, 3,
				4, "XY9G", 50, false, "", "", "", null, null);
		InteractiveRunner runner = new InteractiveRunner(props, codeGeneratorService, csvWriterService,
				csvReaderService, qrCodeGeneratorService, pdfGeneratorService);

		when(codeGeneratorService.generateCodes(anyString(), anyInt()))
			.thenReturn(List.of(new GalleryCode("XY9G-AB7K-92QF")));

		// User selects "generate-codes", no event code prompt needed, accepts all
		// defaults
		ByteArrayInputStream input = new ByteArrayInputStream("1\n\n\n\n".getBytes(StandardCharsets.UTF_8));
		System.setIn(input);

		runner.run(new DefaultApplicationArguments());

		verify(codeGeneratorService).generateCodes("XY9G", 50);
	}

	@Test
	void handlesIoExceptionGracefullyWhenCsvFileMissing() throws Exception {
		AppProperties props = new AppProperties("", "missing.csv", "missing.csv", "qr-codes.pdf", "https://my.site",
				200, 3, 4, "", 50, false, "", "", "", null, null);
		InteractiveRunner runner = new InteractiveRunner(props, codeGeneratorService, csvWriterService,
				csvReaderService, qrCodeGeneratorService, pdfGeneratorService);

		when(csvReaderService.readCodes(any())).thenThrow(new IOException("CSV file not found: missing.csv"));

		// User selects "generate-pdf", accepts all defaults (csvInputPath, outputPath,
		// baseUrl, qrSize, gridColumns, gridRows, showCuttingLines, galleryUrl,
		// logoUrl)
		ByteArrayInputStream input = new ByteArrayInputStream("2\n\n\n\n\n\n\n\n\n\n".getBytes(StandardCharsets.UTF_8));
		System.setIn(input);

		// Should not throw — IOException is caught and logged as an error
		runner.run(new DefaultApplicationArguments());
	}

	@Test
	void promptBooleanReturnsTrueForYesInput() {
		AppProperties props = new AppProperties("", "codes.csv", "codes.csv", "qr-codes.pdf", "https://my.site", 200, 3,
				4, "", 50, false, "", "", "", null, null);
		InteractiveRunner runner = new InteractiveRunner(props, codeGeneratorService, csvWriterService,
				csvReaderService, qrCodeGeneratorService, pdfGeneratorService);

		assertThat(runner.promptBoolean(scannerFrom("yes\n"), "Show cutting lines", false)).isTrue();
		assertThat(runner.promptBoolean(scannerFrom("y\n"), "Show cutting lines", false)).isTrue();
		assertThat(runner.promptBoolean(scannerFrom("true\n"), "Show cutting lines", false)).isTrue();
	}

	@Test
	void promptBooleanReturnsFalseForNoInput() {
		AppProperties props = new AppProperties("", "codes.csv", "codes.csv", "qr-codes.pdf", "https://my.site", 200, 3,
				4, "", 50, true, "", "", "", null, null);
		InteractiveRunner runner = new InteractiveRunner(props, codeGeneratorService, csvWriterService,
				csvReaderService, qrCodeGeneratorService, pdfGeneratorService);

		assertThat(runner.promptBoolean(scannerFrom("no\n"), "Show cutting lines", true)).isFalse();
		assertThat(runner.promptBoolean(scannerFrom("n\n"), "Show cutting lines", true)).isFalse();
		assertThat(runner.promptBoolean(scannerFrom("false\n"), "Show cutting lines", true)).isFalse();
	}

	@Test
	void promptBooleanReturnsDefaultOnBlankInput() {
		AppProperties props = new AppProperties("", "codes.csv", "codes.csv", "qr-codes.pdf", "https://my.site", 200, 3,
				4, "", 50, false, "", "", "", null, null);
		InteractiveRunner runner = new InteractiveRunner(props, codeGeneratorService, csvWriterService,
				csvReaderService, qrCodeGeneratorService, pdfGeneratorService);

		assertThat(runner.promptBoolean(scannerFrom("\n"), "Show cutting lines", false)).isFalse();
		assertThat(runner.promptBoolean(scannerFrom("\n"), "Show cutting lines", true)).isTrue();
	}

	@Test
	void promptBooleanReturnsDefaultOnInvalidInput() {
		AppProperties props = new AppProperties("", "codes.csv", "codes.csv", "qr-codes.pdf", "https://my.site", 200, 3,
				4, "", 50, false, "", "", "", null, null);
		InteractiveRunner runner = new InteractiveRunner(props, codeGeneratorService, csvWriterService,
				csvReaderService, qrCodeGeneratorService, pdfGeneratorService);

		assertThat(runner.promptBoolean(scannerFrom("maybe\n"), "Show cutting lines", true)).isTrue();
	}

	@Test
	void logoUrlDefaultsToLogoWhenNotConfigured() throws Exception {
		AppProperties props = new AppProperties("", "codes.csv", "codes.csv", "qr-codes.pdf", "https://my.site", 200, 3,
				4, "", 50, false, "", "", "", null, null);
		InteractiveRunner runner = new InteractiveRunner(props, codeGeneratorService, csvWriterService,
				csvReaderService, qrCodeGeneratorService, pdfGeneratorService);

		when(csvReaderService.readCodes(any()))
			.thenReturn(new CsvReadResult(List.of(new GalleryCode("XY9G-AB7K-92QF")), "Test Event"));
		when(qrCodeGeneratorService.generateQrCode(any(), anyString(), anyInt(), anyInt()))
			.thenReturn(new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB));
		when(pdfGeneratorService.createPdf(any(), any(), any())).thenReturn(1);

		// User selects "generate-pdf", accepts all defaults (csvInputPath, outputPath,
		// baseUrl, qrSize, gridColumns, gridRows, showCuttingLines, galleryUrl,
		// logoUrl)
		ByteArrayInputStream input = new ByteArrayInputStream("2\n\n\n\n\n\n\n\n\n\n".getBytes(StandardCharsets.UTF_8));
		System.setIn(input);

		runner.run(new DefaultApplicationArguments());

		ArgumentCaptor<PdfOptions> captor = ArgumentCaptor.forClass(PdfOptions.class);
		verify(pdfGeneratorService).createPdf(any(), any(), captor.capture());
		assertThat(captor.getValue().logoUrl()).isEqualTo("logo.png");
	}

	@Test
	void promptsForGalleryCreationAndCallsPicPeakWhenUserSaysYes() throws Exception {
		AppProperties props = new AppProperties("", "codes.csv", "codes.csv", "qr-codes.pdf", "https://my.site", 200, 3,
				4, "XY9G", 50, false, "", "", "", null, null);
		InteractiveRunner runner = new InteractiveRunner(props, codeGeneratorService, csvWriterService,
				csvReaderService, qrCodeGeneratorService, pdfGeneratorService, picPeakService);

		when(codeGeneratorService.generateCodes(anyString(), anyInt()))
			.thenReturn(List.of(new GalleryCode("XY9G-AB7K-92QF")));
		when(picPeakService.enrichWithShareLinks(any(), anyString()))
			.thenReturn(List.of(new GalleryCode("XY9G-AB7K-92QF", "pass", "https://share.link")));

		// Mode: generate-codes, accepts code count/event name/csv defaults, says yes to PicPeak
		ByteArrayInputStream input = new ByteArrayInputStream("1\n\n\n\nyes\n".getBytes(StandardCharsets.UTF_8));
		System.setIn(input);

		runner.run(new DefaultApplicationArguments());

		verify(picPeakService).enrichWithShareLinks(any(), any());
	}

	@Test
	void skipsGalleryCreationWhenUserSaysNo() throws Exception {
		AppProperties props = new AppProperties("", "codes.csv", "codes.csv", "qr-codes.pdf", "https://my.site", 200, 3,
				4, "XY9G", 50, false, "", "", "", null, null);
		InteractiveRunner runner = new InteractiveRunner(props, codeGeneratorService, csvWriterService,
				csvReaderService, qrCodeGeneratorService, pdfGeneratorService, picPeakService);

		when(codeGeneratorService.generateCodes(anyString(), anyInt()))
			.thenReturn(List.of(new GalleryCode("XY9G-AB7K-92QF")));

		// Mode: generate-codes, accepts code count/event name/csv defaults, says no to PicPeak
		ByteArrayInputStream input = new ByteArrayInputStream("1\n\n\n\nno\n".getBytes(StandardCharsets.UTF_8));
		System.setIn(input);

		runner.run(new DefaultApplicationArguments());

		verify(picPeakService, never()).enrichWithShareLinks(any(), any());
	}

	@Test
	void skipsGalleryCreationPromptForGeneratePdfMode() throws Exception {
		AppProperties props = new AppProperties("", "codes.csv", "codes.csv", "qr-codes.pdf", "https://my.site", 200, 3,
				4, "", 50, false, "", "", "", null, null);
		InteractiveRunner runner = new InteractiveRunner(props, codeGeneratorService, csvWriterService,
				csvReaderService, qrCodeGeneratorService, pdfGeneratorService, picPeakService);

		when(csvReaderService.readCodes(any()))
			.thenReturn(new CsvReadResult(List.of(new GalleryCode("XY9G-AB7K-92QF")), "Test Event"));
		when(qrCodeGeneratorService.generateQrCode(any(), anyString(), anyInt(), anyInt()))
			.thenReturn(new java.awt.image.BufferedImage(10, 10, java.awt.image.BufferedImage.TYPE_INT_RGB));
		when(pdfGeneratorService.createPdf(any(), any(), any())).thenReturn(1);

		// Mode: generate-pdf — no PicPeak prompt expected
		ByteArrayInputStream input = new ByteArrayInputStream("2\n\n\n\n\n\n\n\n\n\n".getBytes(StandardCharsets.UTF_8));
		System.setIn(input);

		runner.run(new DefaultApplicationArguments());

		verify(picPeakService, never()).enrichWithShareLinks(any(), any());
	}

	private static Scanner scannerFrom(String text) {
		return new Scanner(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)));
	}

}
