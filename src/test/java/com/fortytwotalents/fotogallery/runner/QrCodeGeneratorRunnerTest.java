package com.fortytwotalents.fotogallery.runner;

import com.fortytwotalents.fotogallery.config.AppProperties;
import com.fortytwotalents.fotogallery.model.CsvReadResult;
import com.fortytwotalents.fotogallery.model.GalleryCode;
import com.fortytwotalents.fotogallery.model.PdfOptions;
import com.fortytwotalents.fotogallery.service.CsvReaderService;
import com.fortytwotalents.fotogallery.service.PdfGeneratorService;
import com.fortytwotalents.fotogallery.service.QrCodeGeneratorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QrCodeGeneratorRunnerTest {

	@Mock
	CsvReaderService csvReaderService;

	@Mock
	QrCodeGeneratorService qrCodeGeneratorService;

	@Mock
	PdfGeneratorService pdfGeneratorService;

	QrCodeGeneratorRunner runner;

	@BeforeEach
	void setUp() {
		AppProperties props = new AppProperties("generate-pdf", "codes.csv", "codes.csv", "qr-codes.pdf",
				"https://my.site", 200, 3, 4, "", 50, false, "", "https://my.site/gallery?code=", "", "", "");
		runner = new QrCodeGeneratorRunner(csvReaderService, qrCodeGeneratorService, pdfGeneratorService, props);
	}

	@Test
	void orchestratesServicesCorrectly() throws Exception {
		GalleryCode code = new GalleryCode("XY9G-AB7K-92QF");
		BufferedImage mockImage = new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB);

		when(csvReaderService.readCodes(any(Path.class))).thenReturn(new CsvReadResult(List.of(code), "Test Event"));
		when(qrCodeGeneratorService.generateQrCode(any(GalleryCode.class), anyString(), anyInt(), anyInt()))
			.thenReturn(mockImage);
		when(pdfGeneratorService.createPdf(any(), any(), any(PdfOptions.class))).thenReturn(1);

		runner.run();

		verify(csvReaderService).readCodes(any(Path.class));
		verify(qrCodeGeneratorService).generateQrCode(eq(code), anyString(), anyInt(), anyInt());
		verify(pdfGeneratorService).createPdf(any(), any(), any(PdfOptions.class));
	}

	@Test
	void skipsGenerationWhenNoCodesFound() throws Exception {
		when(csvReaderService.readCodes(any(Path.class))).thenReturn(new CsvReadResult(List.of(), ""));

		runner.run();

		verify(csvReaderService).readCodes(any(Path.class));
		verify(qrCodeGeneratorService, never()).generateQrCode(any(), anyString(), anyInt(), anyInt());
		verify(pdfGeneratorService, never()).createPdf(any(), any(), any(PdfOptions.class));
	}

	@Test
	void usesPropertiesForPaths() throws Exception {
		AppProperties props = new AppProperties("generate-pdf", "custom-input.csv", "custom-input.csv",
				"custom-output.pdf", "https://my.site", 200, 3, 4, "", 50, false, "", "https://my.site/gallery?code=",
				"", "", "");
		runner = new QrCodeGeneratorRunner(csvReaderService, qrCodeGeneratorService, pdfGeneratorService, props);

		GalleryCode code = new GalleryCode("XY9G-AB7K-92QF");
		BufferedImage mockImage = new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB);

		when(csvReaderService.readCodes(any(Path.class))).thenReturn(new CsvReadResult(List.of(code), ""));
		when(qrCodeGeneratorService.generateQrCode(any(), anyString(), anyInt(), anyInt())).thenReturn(mockImage);
		when(pdfGeneratorService.createPdf(any(), any(), any(PdfOptions.class))).thenReturn(1);

		runner.run();

		verify(csvReaderService).readCodes(Path.of("custom-input.csv"));
		verify(pdfGeneratorService).createPdf(any(), any(), any(PdfOptions.class));
	}

}
