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
		List<GalleryCode> codes = List.of(new GalleryCode("XY9G-AB7K-92QF", "ABC1234"),
				new GalleryCode("XY9G-TK2H-88PL", "DEF5678"), new GalleryCode("XY9G-MN5R-AA11", "GHJ9012"));
		Path output = tempDir.resolve("output.csv");

		service.writeCodes(codes, output, "My Event", "https://my.site/gallery/");

		assertThat(output).exists();
		List<String> lines = Files.readAllLines(output, StandardCharsets.UTF_8);
		assertThat(lines).hasSize(4);
		assertThat(lines.get(0)).isEqualTo("Number,Code,Password,Event Name,URL");
		assertThat(lines.get(1)).isEqualTo("1,XY9G-AB7K-92QF,ABC1234,My Event,https://my.site/gallery/XY9G-AB7K-92QF");
		assertThat(lines.get(2)).isEqualTo("2,XY9G-TK2H-88PL,DEF5678,My Event,https://my.site/gallery/XY9G-TK2H-88PL");
		assertThat(lines.get(3)).isEqualTo("3,XY9G-MN5R-AA11,GHJ9012,My Event,https://my.site/gallery/XY9G-MN5R-AA11");
	}

	@Test
	void writesEmptyFile() throws Exception {
		Path output = tempDir.resolve("empty.csv");

		service.writeCodes(List.of(), output, "", "https://my.site/gallery/");

		assertThat(output).exists();
		List<String> lines = Files.readAllLines(output, StandardCharsets.UTF_8);
		assertThat(lines).hasSize(1);
		assertThat(lines.get(0)).isEqualTo("Number,Code,Password,Event Name,URL");
	}

	@Test
	void outputIsReadableByReaderService() throws Exception {
		CsvReaderService readerService = new CsvReaderService();
		List<GalleryCode> codes = List.of(new GalleryCode("XY9G-AB7K-92QF", "PW12345"),
				new GalleryCode("XY9G-TK2H-88PL", "PW67890"));
		Path output = tempDir.resolve("roundtrip.csv");

		service.writeCodes(codes, output, "Test Event", "https://my.site/gallery/");
		var result = readerService.readCodes(output);

		List<GalleryCode> expected = List.of(
				new GalleryCode("XY9G-AB7K-92QF", "PW12345", "https://my.site/gallery/XY9G-AB7K-92QF"),
				new GalleryCode("XY9G-TK2H-88PL", "PW67890", "https://my.site/gallery/XY9G-TK2H-88PL"));
		assertThat(result.codes()).isEqualTo(expected);
		assertThat(result.eventName()).isEqualTo("Test Event");
	}

}
