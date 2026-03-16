package com.fortytwotalents.fotogallery.runner;

import com.fortytwotalents.fotogallery.config.AppProperties;
import com.fortytwotalents.fotogallery.model.GalleryCode;
import com.fortytwotalents.fotogallery.service.CodeGeneratorService;
import com.fortytwotalents.fotogallery.service.CsvWriterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CodeGeneratorRunnerTest {

	@Mock
	CodeGeneratorService codeGeneratorService;

	@Mock
	CsvWriterService csvWriterService;

	CodeGeneratorRunner runner;

	@BeforeEach
	void setUp() {
		AppProperties props = new AppProperties("generate-codes", "codes.csv", "codes.csv", "qr-codes.pdf",
				"https://my.site/gallery/", 200, 3, 4, "XY9G", 50, false, "", "", "");
		runner = new CodeGeneratorRunner(codeGeneratorService, csvWriterService, props);
	}

	@Test
	void orchestratesCodeGeneration() throws Exception {
		List<GalleryCode> codes = List.of(new GalleryCode("XY9G-AB7K-92QF"));
		when(codeGeneratorService.generateCodes(anyString(), anyInt())).thenReturn(codes);

		runner.run();

		verify(codeGeneratorService).generateCodes("XY9G", 50);
		verify(csvWriterService).writeCodes(codes, Path.of("codes.csv"), "");
	}

	@Test
	void usesPropertiesForConfig() throws Exception {
		AppProperties props = new AppProperties("generate-codes", "custom.csv", "custom.csv", "qr-codes.pdf",
				"https://my.site/gallery/", 200, 3, 4, "AB1C", 25, false, "", "", "");
		runner = new CodeGeneratorRunner(codeGeneratorService, csvWriterService, props);

		List<GalleryCode> codes = List.of(new GalleryCode("AB1C-XY7K-92QF"));
		when(codeGeneratorService.generateCodes(anyString(), anyInt())).thenReturn(codes);

		runner.run();

		verify(codeGeneratorService).generateCodes("AB1C", 25);
		verify(csvWriterService).writeCodes(codes, Path.of("custom.csv"), "");
	}

	@Test
	void doesNothingWhenEventCodeIsEmpty() throws Exception {
		AppProperties props = new AppProperties("generate-codes", "codes.csv", "codes.csv", "qr-codes.pdf",
				"https://my.site/gallery/", 200, 3, 4, "", 50, false, "", "", "");
		runner = new CodeGeneratorRunner(codeGeneratorService, csvWriterService, props);

		runner.run();

		verify(codeGeneratorService, never()).generateCodes(any(), anyInt());
		verify(csvWriterService, never()).writeCodes(any(), any(), any());
	}

}
