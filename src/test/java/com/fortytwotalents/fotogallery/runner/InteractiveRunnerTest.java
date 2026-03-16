package com.fortytwotalents.fotogallery.runner;

import com.fortytwotalents.fotogallery.config.AppProperties;
import com.fortytwotalents.fotogallery.model.GalleryCode;
import com.fortytwotalents.fotogallery.service.CodeGeneratorService;
import com.fortytwotalents.fotogallery.service.CsvReaderService;
import com.fortytwotalents.fotogallery.service.CsvWriterService;
import com.fortytwotalents.fotogallery.service.PdfGeneratorService;
import com.fortytwotalents.fotogallery.service.QrCodeGeneratorService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;

import java.io.ByteArrayInputStream;
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

	@Test
	void skipsWhenModeIsAlreadySet() throws Exception {
		AppProperties props = new AppProperties("generate-codes", "codes.csv", "codes.csv", "qr-codes.pdf",
				"https://my.site/gallery/", 200, 3, 4, "XY9G", 50, false, "", "", "");
		InteractiveRunner runner = new InteractiveRunner(props, codeGeneratorService, csvWriterService, csvReaderService,
				qrCodeGeneratorService, pdfGeneratorService);

		runner.run(new DefaultApplicationArguments());

		verify(codeGeneratorService, never()).generateCodes(anyString(), anyInt());
	}

	@Test
	void promptModeAcceptsNumericChoice() {
		AppProperties props = new AppProperties("", "codes.csv", "codes.csv", "qr-codes.pdf",
				"https://my.site/gallery/", 200, 3, 4, "", 50, false, "", "", "");
		InteractiveRunner runner = new InteractiveRunner(props, codeGeneratorService, csvWriterService, csvReaderService,
				qrCodeGeneratorService, pdfGeneratorService);

		Scanner scanner = scannerFrom("1\n");
		assertThat(runner.promptMode(scanner)).isEqualTo("generate-codes");

		scanner = scannerFrom("2\n");
		assertThat(runner.promptMode(scanner)).isEqualTo("generate-pdf");

		scanner = scannerFrom("3\n");
		assertThat(runner.promptMode(scanner)).isEqualTo("both");
	}

	@Test
	void promptModeAcceptsNamedChoice() {
		AppProperties props = new AppProperties("", "codes.csv", "codes.csv", "qr-codes.pdf",
				"https://my.site/gallery/", 200, 3, 4, "", 50, false, "", "", "");
		InteractiveRunner runner = new InteractiveRunner(props, codeGeneratorService, csvWriterService, csvReaderService,
				qrCodeGeneratorService, pdfGeneratorService);

		Scanner scanner = scannerFrom("generate-codes\n");
		assertThat(runner.promptMode(scanner)).isEqualTo("generate-codes");

		scanner = scannerFrom("generate-pdf\n");
		assertThat(runner.promptMode(scanner)).isEqualTo("generate-pdf");

		scanner = scannerFrom("both\n");
		assertThat(runner.promptMode(scanner)).isEqualTo("both");
	}

	@Test
	void promptModeDefaultsToBothOnBlankInput() {
		AppProperties props = new AppProperties("", "codes.csv", "codes.csv", "qr-codes.pdf",
				"https://my.site/gallery/", 200, 3, 4, "", 50, false, "", "", "");
		InteractiveRunner runner = new InteractiveRunner(props, codeGeneratorService, csvWriterService, csvReaderService,
				qrCodeGeneratorService, pdfGeneratorService);

		Scanner scanner = scannerFrom("\n");
		assertThat(runner.promptMode(scanner)).isEqualTo("both");
	}

	@Test
	void promptModeRetriesOnInvalidInput() {
		AppProperties props = new AppProperties("", "codes.csv", "codes.csv", "qr-codes.pdf",
				"https://my.site/gallery/", 200, 3, 4, "", 50, false, "", "", "");
		InteractiveRunner runner = new InteractiveRunner(props, codeGeneratorService, csvWriterService, csvReaderService,
				qrCodeGeneratorService, pdfGeneratorService);

		Scanner scanner = scannerFrom("invalid\n2\n");
		assertThat(runner.promptMode(scanner)).isEqualTo("generate-pdf");
	}

	@Test
	void promptRequiredReturnsValue() {
		AppProperties props = new AppProperties("", "codes.csv", "codes.csv", "qr-codes.pdf",
				"https://my.site/gallery/", 200, 3, 4, "", 50, false, "", "", "");
		InteractiveRunner runner = new InteractiveRunner(props, codeGeneratorService, csvWriterService, csvReaderService,
				qrCodeGeneratorService, pdfGeneratorService);

		Scanner scanner = scannerFrom("XY9G\n");
		assertThat(runner.promptRequired(scanner, "Event code")).isEqualTo("XY9G");
	}

	@Test
	void promptRequiredRetriesOnBlankInput() {
		AppProperties props = new AppProperties("", "codes.csv", "codes.csv", "qr-codes.pdf",
				"https://my.site/gallery/", 200, 3, 4, "", 50, false, "", "", "");
		InteractiveRunner runner = new InteractiveRunner(props, codeGeneratorService, csvWriterService, csvReaderService,
				qrCodeGeneratorService, pdfGeneratorService);

		Scanner scanner = scannerFrom("\nAB1C\n");
		assertThat(runner.promptRequired(scanner, "Event code")).isEqualTo("AB1C");
	}

	@Test
	void promptEventCodeAcceptsValidCode() {
		AppProperties props = new AppProperties("", "codes.csv", "codes.csv", "qr-codes.pdf",
				"https://my.site/gallery/", 200, 3, 4, "", 50, false, "", "", "");
		InteractiveRunner runner = new InteractiveRunner(props, codeGeneratorService, csvWriterService, csvReaderService,
				qrCodeGeneratorService, pdfGeneratorService);

		Scanner scanner = scannerFrom("XY9G\n");
		assertThat(runner.promptEventCode(scanner)).isEqualTo("XY9G");
	}

	@Test
	void promptEventCodeNormalizesToUpperCase() {
		AppProperties props = new AppProperties("", "codes.csv", "codes.csv", "qr-codes.pdf",
				"https://my.site/gallery/", 200, 3, 4, "", 50, false, "", "", "");
		InteractiveRunner runner = new InteractiveRunner(props, codeGeneratorService, csvWriterService, csvReaderService,
				qrCodeGeneratorService, pdfGeneratorService);

		Scanner scanner = scannerFrom("ab1c\n");
		assertThat(runner.promptEventCode(scanner)).isEqualTo("AB1C");
	}

	@Test
	void promptEventCodeRetriesOnTooShort() {
		AppProperties props = new AppProperties("", "codes.csv", "codes.csv", "qr-codes.pdf",
				"https://my.site/gallery/", 200, 3, 4, "", 50, false, "", "", "");
		InteractiveRunner runner = new InteractiveRunner(props, codeGeneratorService, csvWriterService, csvReaderService,
				qrCodeGeneratorService, pdfGeneratorService);

		Scanner scanner = scannerFrom("abc\nXY9G\n");
		assertThat(runner.promptEventCode(scanner)).isEqualTo("XY9G");
	}

	@Test
	void promptEventCodeRetriesOnTooLong() {
		AppProperties props = new AppProperties("", "codes.csv", "codes.csv", "qr-codes.pdf",
				"https://my.site/gallery/", 200, 3, 4, "", 50, false, "", "", "");
		InteractiveRunner runner = new InteractiveRunner(props, codeGeneratorService, csvWriterService, csvReaderService,
				qrCodeGeneratorService, pdfGeneratorService);

		Scanner scanner = scannerFrom("ABCDE\nXY9G\n");
		assertThat(runner.promptEventCode(scanner)).isEqualTo("XY9G");
	}

	@Test
	void promptEventCodeRetriesOnBlankInput() {
		AppProperties props = new AppProperties("", "codes.csv", "codes.csv", "qr-codes.pdf",
				"https://my.site/gallery/", 200, 3, 4, "", 50, false, "", "", "");
		InteractiveRunner runner = new InteractiveRunner(props, codeGeneratorService, csvWriterService, csvReaderService,
				qrCodeGeneratorService, pdfGeneratorService);

		Scanner scanner = scannerFrom("\nXY9G\n");
		assertThat(runner.promptEventCode(scanner)).isEqualTo("XY9G");
	}

	@Test
	void promptOptionalReturnsDefaultOnBlankInput() {
		AppProperties props = new AppProperties("", "codes.csv", "codes.csv", "qr-codes.pdf",
				"https://my.site/gallery/", 200, 3, 4, "", 50, false, "", "", "");
		InteractiveRunner runner = new InteractiveRunner(props, codeGeneratorService, csvWriterService, csvReaderService,
				qrCodeGeneratorService, pdfGeneratorService);

		Scanner scanner = scannerFrom("\n");
		assertThat(runner.promptOptional(scanner, "Event name", "default-name")).isEqualTo("default-name");
	}

	@Test
	void promptOptionalReturnsProvidedValue() {
		AppProperties props = new AppProperties("", "codes.csv", "codes.csv", "qr-codes.pdf",
				"https://my.site/gallery/", 200, 3, 4, "", 50, false, "", "", "");
		InteractiveRunner runner = new InteractiveRunner(props, codeGeneratorService, csvWriterService, csvReaderService,
				qrCodeGeneratorService, pdfGeneratorService);

		Scanner scanner = scannerFrom("My Event\n");
		assertThat(runner.promptOptional(scanner, "Event name", "default-name")).isEqualTo("My Event");
	}

	@Test
	void promptIntReturnsDefaultOnBlankInput() {
		AppProperties props = new AppProperties("", "codes.csv", "codes.csv", "qr-codes.pdf",
				"https://my.site/gallery/", 200, 3, 4, "", 50, false, "", "", "");
		InteractiveRunner runner = new InteractiveRunner(props, codeGeneratorService, csvWriterService, csvReaderService,
				qrCodeGeneratorService, pdfGeneratorService);

		Scanner scanner = scannerFrom("\n");
		assertThat(runner.promptInt(scanner, "Code count", 50)).isEqualTo(50);
	}

	@Test
	void promptIntReturnsProvidedValue() {
		AppProperties props = new AppProperties("", "codes.csv", "codes.csv", "qr-codes.pdf",
				"https://my.site/gallery/", 200, 3, 4, "", 50, false, "", "", "");
		InteractiveRunner runner = new InteractiveRunner(props, codeGeneratorService, csvWriterService, csvReaderService,
				qrCodeGeneratorService, pdfGeneratorService);

		Scanner scanner = scannerFrom("100\n");
		assertThat(runner.promptInt(scanner, "Code count", 50)).isEqualTo(100);
	}

	@Test
	void promptIntReturnsDefaultOnInvalidInput() {
		AppProperties props = new AppProperties("", "codes.csv", "codes.csv", "qr-codes.pdf",
				"https://my.site/gallery/", 200, 3, 4, "", 50, false, "", "", "");
		InteractiveRunner runner = new InteractiveRunner(props, codeGeneratorService, csvWriterService, csvReaderService,
				qrCodeGeneratorService, pdfGeneratorService);

		Scanner scanner = scannerFrom("not-a-number\n");
		assertThat(runner.promptInt(scanner, "Code count", 50)).isEqualTo(50);
	}

	@Test
	void runsGenerateCodesWhenModeBlankAndUserSelectsGenerateCodes() throws Exception {
		AppProperties props = new AppProperties("", "codes.csv", "codes.csv", "qr-codes.pdf",
				"https://my.site/gallery/", 200, 3, 4, "", 50, false, "", "", "");
		InteractiveRunner runner = new InteractiveRunner(props, codeGeneratorService, csvWriterService, csvReaderService,
				qrCodeGeneratorService, pdfGeneratorService);

		when(codeGeneratorService.generateCodes(anyString(), anyInt()))
			.thenReturn(List.of(new GalleryCode("XY9G-AB7K-92QF")));

		// User selects "generate-codes", enters event code "XY9G", accepts all defaults
		ByteArrayInputStream input = new ByteArrayInputStream(
				"1\nXY9G\n\n\n\n".getBytes(StandardCharsets.UTF_8));
		System.setIn(input);

		runner.run(new DefaultApplicationArguments());

		verify(codeGeneratorService).generateCodes(anyString(), anyInt());
		verify(csvWriterService).writeCodes(any(), any(), any());
	}

	@Test
	void doesNotAskForEventCodeIfAlreadyProvided() throws Exception {
		AppProperties props = new AppProperties("", "codes.csv", "codes.csv", "qr-codes.pdf",
				"https://my.site/gallery/", 200, 3, 4, "XY9G", 50, false, "", "", "");
		InteractiveRunner runner = new InteractiveRunner(props, codeGeneratorService, csvWriterService, csvReaderService,
				qrCodeGeneratorService, pdfGeneratorService);

		when(codeGeneratorService.generateCodes(anyString(), anyInt()))
			.thenReturn(List.of(new GalleryCode("XY9G-AB7K-92QF")));

		// User selects "generate-codes", no event code prompt needed, accepts all defaults
		ByteArrayInputStream input = new ByteArrayInputStream(
				"1\n\n\n\n".getBytes(StandardCharsets.UTF_8));
		System.setIn(input);

		runner.run(new DefaultApplicationArguments());

		verify(codeGeneratorService).generateCodes("XY9G", 50);
	}

	@Test
	void handlesIoExceptionGracefullyWhenCsvFileMissing() throws Exception {
		AppProperties props = new AppProperties("", "missing.csv", "missing.csv", "qr-codes.pdf",
				"https://my.site/gallery/", 200, 3, 4, "", 50, false, "", "", "");
		InteractiveRunner runner = new InteractiveRunner(props, codeGeneratorService, csvWriterService, csvReaderService,
				qrCodeGeneratorService, pdfGeneratorService);

		when(csvReaderService.readCodes(any())).thenThrow(new java.io.IOException("CSV file not found: missing.csv"));

		// User selects "generate-pdf", accepts all defaults (csvInputPath, outputPath, baseUrl, qrSize, gridColumns, gridRows, showCuttingLines, galleryUrl, logoUrl)
		ByteArrayInputStream input = new ByteArrayInputStream("2\n\n\n\n\n\n\n\n\n\n".getBytes(StandardCharsets.UTF_8));
		System.setIn(input);

		// Should not throw — IOException is caught and logged as an error
		runner.run(new DefaultApplicationArguments());
	}

	@Test
	void promptBooleanReturnsTrueForYesInput() {
		AppProperties props = new AppProperties("", "codes.csv", "codes.csv", "qr-codes.pdf",
				"https://my.site/gallery/", 200, 3, 4, "", 50, false, "", "", "");
		InteractiveRunner runner = new InteractiveRunner(props, codeGeneratorService, csvWriterService,
				csvReaderService, qrCodeGeneratorService, pdfGeneratorService);

		assertThat(runner.promptBoolean(scannerFrom("yes\n"), "Show cutting lines", false)).isTrue();
		assertThat(runner.promptBoolean(scannerFrom("y\n"), "Show cutting lines", false)).isTrue();
		assertThat(runner.promptBoolean(scannerFrom("true\n"), "Show cutting lines", false)).isTrue();
	}

	@Test
	void promptBooleanReturnsFalseForNoInput() {
		AppProperties props = new AppProperties("", "codes.csv", "codes.csv", "qr-codes.pdf",
				"https://my.site/gallery/", 200, 3, 4, "", 50, true, "", "", "");
		InteractiveRunner runner = new InteractiveRunner(props, codeGeneratorService, csvWriterService,
				csvReaderService, qrCodeGeneratorService, pdfGeneratorService);

		assertThat(runner.promptBoolean(scannerFrom("no\n"), "Show cutting lines", true)).isFalse();
		assertThat(runner.promptBoolean(scannerFrom("n\n"), "Show cutting lines", true)).isFalse();
		assertThat(runner.promptBoolean(scannerFrom("false\n"), "Show cutting lines", true)).isFalse();
	}

	@Test
	void promptBooleanReturnsDefaultOnBlankInput() {
		AppProperties props = new AppProperties("", "codes.csv", "codes.csv", "qr-codes.pdf",
				"https://my.site/gallery/", 200, 3, 4, "", 50, false, "", "", "");
		InteractiveRunner runner = new InteractiveRunner(props, codeGeneratorService, csvWriterService,
				csvReaderService, qrCodeGeneratorService, pdfGeneratorService);

		assertThat(runner.promptBoolean(scannerFrom("\n"), "Show cutting lines", false)).isFalse();
		assertThat(runner.promptBoolean(scannerFrom("\n"), "Show cutting lines", true)).isTrue();
	}

	@Test
	void promptBooleanReturnsDefaultOnInvalidInput() {
		AppProperties props = new AppProperties("", "codes.csv", "codes.csv", "qr-codes.pdf",
				"https://my.site/gallery/", 200, 3, 4, "", 50, false, "", "", "");
		InteractiveRunner runner = new InteractiveRunner(props, codeGeneratorService, csvWriterService,
				csvReaderService, qrCodeGeneratorService, pdfGeneratorService);

		assertThat(runner.promptBoolean(scannerFrom("maybe\n"), "Show cutting lines", true)).isTrue();
	}

	private static Scanner scannerFrom(String text) {
		return new Scanner(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)));
	}

}
