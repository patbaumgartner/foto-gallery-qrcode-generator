package com.fortytwotalents.fotogallery.runner;

import com.fortytwotalents.fotogallery.config.AppProperties;
import com.fortytwotalents.fotogallery.service.CodeGeneratorService;
import com.fortytwotalents.fotogallery.service.CsvReaderService;
import com.fortytwotalents.fotogallery.service.CsvWriterService;
import com.fortytwotalents.fotogallery.service.PdfGeneratorService;
import com.fortytwotalents.fotogallery.service.QrCodeGeneratorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Scanner;

@Component
@Order(1)
public class InteractiveRunner implements ApplicationRunner {

	private static final Logger LOGGER = LoggerFactory.getLogger(InteractiveRunner.class);

	private static final String EVENT_CODE_PATTERN = "^[A-Za-z0-9]{4}$";

	private static final String CODE_CHARSET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

	private final SecureRandom random = new SecureRandom();

	private final AppProperties appProperties;

	private final CodeGeneratorService codeGeneratorService;

	private final CsvWriterService csvWriterService;

	private final CsvReaderService csvReaderService;

	private final QrCodeGeneratorService qrCodeGeneratorService;

	private final PdfGeneratorService pdfGeneratorService;

	public InteractiveRunner(AppProperties appProperties, CodeGeneratorService codeGeneratorService,
			CsvWriterService csvWriterService, CsvReaderService csvReaderService,
			QrCodeGeneratorService qrCodeGeneratorService, PdfGeneratorService pdfGeneratorService) {
		this.appProperties = appProperties;
		this.codeGeneratorService = codeGeneratorService;
		this.csvWriterService = csvWriterService;
		this.csvReaderService = csvReaderService;
		this.qrCodeGeneratorService = qrCodeGeneratorService;
		this.pdfGeneratorService = pdfGeneratorService;
	}

	@Override
	public void run(ApplicationArguments args) {
		if (!appProperties.mode().isBlank()) {
			return;
		}

		LOGGER.info("No mode specified. Starting interactive shell...");
		System.out.println("=== Foto Gallery QR Code Generator - Interactive Shell ===");

		try (Scanner scanner = new Scanner(System.in)) {
			String mode = promptMode(scanner);

			String eventCode = appProperties.eventCode();
			int codeCount = appProperties.codeCount();
			String eventName = appProperties.eventName();
			String csvInputPath = appProperties.csvInputPath();
			String csvOutputPath = appProperties.csvOutputPath();
			String outputPath = appProperties.outputPath();
			String baseUrl = appProperties.baseUrl();
			int qrSize = appProperties.qrSize();
			int gridColumns = appProperties.gridColumns();
			int gridRows = appProperties.gridRows();
			boolean showCuttingLines = appProperties.showCuttingLines();
			String galleryUrl = appProperties.galleryUrl();
			String logoUrl = appProperties.logoUrl();
			String galleryCodeLabel = appProperties.galleryCodeLabel();
			String galleryPasswordLabel = appProperties.galleryPasswordLabel();

			if ("generate-codes".equals(mode) || "both".equals(mode)) {
				if (eventCode.isBlank()) {
					eventCode = promptEventCode(scanner);
				}
				codeCount = promptInt(scanner, "Number of codes to generate", codeCount);
				eventName = promptOptional(scanner, "Event name", eventName);
				csvOutputPath = promptOptional(scanner, "CSV output path", csvOutputPath);
			}

			if ("generate-pdf".equals(mode) || "both".equals(mode)) {
				csvInputPath = promptOptional(scanner, "CSV input path", csvInputPath);
				outputPath = promptOptional(scanner, "PDF output path", outputPath);
				baseUrl = promptOptional(scanner, "Base URL (displayed on back of PDF)", baseUrl);
				galleryUrl = promptOptional(scanner, "Gallery URL for QR codes (must start with https://)",
						galleryUrl);
				qrSize = promptInt(scanner, "QR code size (pixels)", qrSize);
				gridColumns = promptInt(scanner, "Grid columns per page", gridColumns);
				gridRows = promptInt(scanner, "Grid rows per page", gridRows);
				showCuttingLines = promptBoolean(scanner, "Show cutting lines", showCuttingLines);
				String logoUrlDefault = logoUrl.isBlank() ? "logo.png" : logoUrl;
				logoUrl = promptOptional(scanner, "Logo URL for back page (JPEG/PNG)", logoUrlDefault);
			}

			try {
				if ("generate-codes".equals(mode) || "both".equals(mode)) {
					AppProperties codeProps = new AppProperties("generate-codes", csvInputPath, csvOutputPath,
							outputPath, baseUrl, qrSize, gridColumns, gridRows, eventCode, codeCount,
							showCuttingLines, eventName, galleryUrl, logoUrl, galleryCodeLabel, galleryPasswordLabel);
					new CodeGeneratorRunner(codeGeneratorService, csvWriterService, codeProps).run();
				}

				if ("generate-pdf".equals(mode) || "both".equals(mode)) {
					AppProperties pdfProps = new AppProperties("generate-pdf", csvInputPath, csvOutputPath, outputPath,
							baseUrl, qrSize, gridColumns, gridRows, eventCode, codeCount, showCuttingLines, eventName,
							galleryUrl, logoUrl, galleryCodeLabel, galleryPasswordLabel);
					new QrCodeGeneratorRunner(csvReaderService, qrCodeGeneratorService, pdfGeneratorService, pdfProps)
						.run();
				}
			}
			catch (IOException ex) {
				LOGGER.error("Operation failed: {}", ex.getMessage());
			}
			catch (IllegalArgumentException ex) {
				LOGGER.error("Invalid input: {}", ex.getMessage());
			}
		}
	}

	String promptMode(Scanner scanner) {
		System.out.println("\nWhat would you like to do?");
		System.out.println("  1) generate-codes - Generate gallery access codes");
		System.out.println("  2) generate-pdf   - Generate QR code PDF from existing codes");
		System.out.println("  3) both           - Generate codes and then produce QR code PDF");

		String mode = "";
		while (!isValidMode(mode)) {
			System.out.print("Enter choice (1/2/3 or name) [both]: ");
			String input = scanner.nextLine().trim();
			if (input.isBlank()) {
				mode = "both";
			}
			else if ("1".equals(input) || "generate-codes".equals(input)) {
				mode = "generate-codes";
			}
			else if ("2".equals(input) || "generate-pdf".equals(input)) {
				mode = "generate-pdf";
			}
			else if ("3".equals(input) || "both".equals(input)) {
				mode = "both";
			}
			else {
				System.out.println("Invalid choice. Please enter 1, 2, 3, generate-codes, generate-pdf, or both.");
			}
		}
		return mode;
	}

	String promptRequired(Scanner scanner, String label) {
		String value = "";
		while (value.isBlank()) {
			System.out.print(label + ": ");
			value = scanner.nextLine().trim();
			if (value.isBlank()) {
				System.out.println("This field is required.");
			}
		}
		return value;
	}

	String promptEventCode(Scanner scanner) {
		String randomDefault = generateRandomEventCode();
		String value = "";
		while (!value.matches(EVENT_CODE_PATTERN)) {
			System.out.print("Event code (4-char prefix, e.g. XY9G) [" + randomDefault + "]: ");
			value = scanner.nextLine().trim();
			if (value.isBlank()) {
				return randomDefault;
			}
			else if (!value.matches(EVENT_CODE_PATTERN)) {
				System.out.println("Event code must be exactly 4 alphanumeric characters (A-Z, 0-9), got: '" + value
						+ "'");
			}
		}
		return value.toUpperCase();
	}

	String promptOptional(Scanner scanner, String label, String defaultValue) {
		System.out.print(label + " [" + defaultValue + "]: ");
		String input = scanner.nextLine().trim();
		return input.isBlank() ? defaultValue : input;
	}

	boolean promptBoolean(Scanner scanner, String label, boolean defaultValue) {
		String defaultStr = defaultValue ? "yes" : "no";
		System.out.print(label + " (yes/no) [" + defaultStr + "]: ");
		String input = scanner.nextLine().trim().toLowerCase();
		if (input.isBlank()) {
			return defaultValue;
		}
		if ("yes".equals(input) || "y".equals(input) || "true".equals(input)) {
			return true;
		}
		if ("no".equals(input) || "n".equals(input) || "false".equals(input)) {
			return false;
		}
		System.out.println("Invalid input, using default: " + defaultStr);
		return defaultValue;
	}

	int promptInt(Scanner scanner, String label, int defaultValue) {
		System.out.print(label + " [" + defaultValue + "]: ");
		String input = scanner.nextLine().trim();
		if (input.isBlank()) {
			return defaultValue;
		}
		try {
			int value = Integer.parseInt(input);
			if (value <= 0) {
				System.out.println("Value must be positive, using default: " + defaultValue);
				return defaultValue;
			}
			return value;
		}
		catch (NumberFormatException ex) {
			System.out.println("Invalid number, using default: " + defaultValue);
			return defaultValue;
		}
	}

	private String generateRandomEventCode() {
		StringBuilder sb = new StringBuilder(4);
		for (int i = 0; i < 4; i++) {
			sb.append(CODE_CHARSET.charAt(random.nextInt(CODE_CHARSET.length())));
		}
		return sb.toString();
	}

	private boolean isValidMode(String mode) {
		return "generate-codes".equals(mode) || "generate-pdf".equals(mode) || "both".equals(mode);
	}

}
