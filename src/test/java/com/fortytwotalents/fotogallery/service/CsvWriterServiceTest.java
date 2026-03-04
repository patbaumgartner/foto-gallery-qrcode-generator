package com.fortytwotalents.fotogallery.service;

import com.fortytwotalents.fotogallery.model.GalleryCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CsvWriterServiceTest {

	private CsvWriterService service;

	@TempDir
	Path tempDir;

	@BeforeEach
	void setUp() {
		service = new CsvWriterService();
	}

	@Test
	void writesCodesToCsvFile() throws Exception {
		List<GalleryCode> codes = List.of(new GalleryCode("XY9G-AB7K-92QF"), new GalleryCode("XY9G-TK2H-88PL"),
				new GalleryCode("XY9G-MN5R-AA11"));
		Path output = tempDir.resolve("output.csv");

		service.writeCodes(codes, output);

		assertThat(output).exists();
		List<String> lines = Files.readAllLines(output, StandardCharsets.UTF_8);
		assertThat(lines).hasSize(3);
		assertThat(lines.get(0)).isEqualTo("XY9G-AB7K-92QF");
		assertThat(lines.get(1)).isEqualTo("XY9G-TK2H-88PL");
		assertThat(lines.get(2)).isEqualTo("XY9G-MN5R-AA11");
	}

	@Test
	void writesEmptyFile() throws Exception {
		Path output = tempDir.resolve("empty.csv");

		service.writeCodes(List.of(), output);

		assertThat(output).exists();
		assertThat(Files.readAllLines(output, StandardCharsets.UTF_8)).isEmpty();
	}

	@Test
	void outputIsReadableByReaderService() throws Exception {
		CsvReaderService readerService = new CsvReaderService();
		List<GalleryCode> codes = List.of(new GalleryCode("XY9G-AB7K-92QF"), new GalleryCode("XY9G-TK2H-88PL"));
		Path output = tempDir.resolve("roundtrip.csv");

		service.writeCodes(codes, output);
		List<GalleryCode> readCodes = readerService.readCodes(output);

		assertThat(readCodes).isEqualTo(codes);
	}

}
