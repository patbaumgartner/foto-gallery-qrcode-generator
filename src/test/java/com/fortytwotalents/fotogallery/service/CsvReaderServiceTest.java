package com.fortytwotalents.fotogallery.service;

import com.fortytwotalents.fotogallery.model.CsvReadResult;
import com.fortytwotalents.fotogallery.model.GalleryCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CsvReaderServiceTest {

	private CsvReaderService service;

	@TempDir
	Path tempDir;

	@BeforeEach
	void setUp() {
		service = new CsvReaderService();
	}

	@Test
	void readsValidCodes() throws Exception {
		Path csv = writeCsv("XY9G-AB7K-92QF\nTK2H-XY3M-88PL\nMN5R-ZZ99-AA11\n");

		CsvReadResult result = service.readCodes(csv);

		assertThat(result.codes()).hasSize(3);
		assertThat(result.codes()).extracting(GalleryCode::code)
			.containsExactly("XY9G-AB7K-92QF", "TK2H-XY3M-88PL", "MN5R-ZZ99-AA11");
		assertThat(result.eventName()).isEmpty();
	}

	@Test
	void skipsBlankLines() throws Exception {
		Path csv = writeCsv("XY9G-AB7K-92QF\n\n\nTK2H-XY3M-88PL\n\n");

		CsvReadResult result = service.readCodes(csv);

		assertThat(result.codes()).hasSize(2);
		assertThat(result.codes()).extracting(GalleryCode::code).containsExactly("XY9G-AB7K-92QF", "TK2H-XY3M-88PL");
	}

	@Test
	void trimsWhitespace() throws Exception {
		Path csv = writeCsv("  XY9G-AB7K-92QF  \n  TK2H-XY3M-88PL\t\n");

		CsvReadResult result = service.readCodes(csv);

		assertThat(result.codes()).hasSize(2);
		assertThat(result.codes()).extracting(GalleryCode::code).containsExactly("XY9G-AB7K-92QF", "TK2H-XY3M-88PL");
	}

	@Test
	void handlesBom() throws Exception {
		// UTF-8 BOM + content
		byte[] bom = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
		byte[] content = "XY9G-AB7K-92QF\n".getBytes(StandardCharsets.UTF_8);
		byte[] withBom = new byte[bom.length + content.length];
		System.arraycopy(bom, 0, withBom, 0, bom.length);
		System.arraycopy(content, 0, withBom, bom.length, content.length);

		Path csv = tempDir.resolve("bom.csv");
		Files.write(csv, withBom);

		CsvReadResult result = service.readCodes(csv);

		assertThat(result.codes()).hasSize(1);
		assertThat(result.codes().getFirst().code()).isEqualTo("XY9G-AB7K-92QF");
	}

	@Test
	void skipsInvalidCodes() throws Exception {
		Path csv = writeCsv("XY9G-AB7K-92QF\ninvalid\n123\nTK2H-XY3M-88PL\n");

		CsvReadResult result = service.readCodes(csv);

		assertThat(result.codes()).hasSize(2);
		assertThat(result.codes()).extracting(GalleryCode::code).containsExactly("XY9G-AB7K-92QF", "TK2H-XY3M-88PL");
	}

	@Test
	void deduplicatesCodes() throws Exception {
		Path csv = writeCsv("XY9G-AB7K-92QF\nTK2H-XY3M-88PL\nXY9G-AB7K-92QF\n");

		CsvReadResult result = service.readCodes(csv);

		assertThat(result.codes()).hasSize(2);
		assertThat(result.codes()).extracting(GalleryCode::code).containsExactly("XY9G-AB7K-92QF", "TK2H-XY3M-88PL");
	}

	@Test
	void returnsEmptyListForEmptyFile() throws Exception {
		Path csv = writeCsv("");

		CsvReadResult result = service.readCodes(csv);

		assertThat(result.codes()).isEmpty();
		assertThat(result.eventName()).isEmpty();
	}

	@Test
	void throwsExceptionForMissingFile() {
		Path missing = tempDir.resolve("nonexistent.csv");

		assertThatThrownBy(() -> service.readCodes(missing)).isInstanceOf(IOException.class)
			.hasMessageContaining("CSV file not found");
	}

	@Test
	void readsEventNameFromHeaderCsv() throws Exception {
		Path csv = writeCsv("Number,Code,Event Name\n1,XY9G-AB7K-92QF,My Event\n2,TK2H-XY3M-88PL,My Event\n");

		CsvReadResult result = service.readCodes(csv);

		assertThat(result.codes()).hasSize(2);
		assertThat(result.codes()).extracting(GalleryCode::code).containsExactly("XY9G-AB7K-92QF", "TK2H-XY3M-88PL");
		assertThat(result.eventName()).isEqualTo("My Event");
	}

	@Test
	void readsPasswordFromHeaderCsv() throws Exception {
		Path csv = writeCsv(
				"Number,Code,Event Name,Password\n1,XY9G-AB7K-92QF,My Event,ABC1234\n2,TK2H-XY3M-88PL,My Event,DEF5678\n");

		CsvReadResult result = service.readCodes(csv);

		assertThat(result.codes()).hasSize(2);
		assertThat(result.codes()).extracting(GalleryCode::password).containsExactly("ABC1234", "DEF5678");
	}

	@Test
	void passwordDefaultsToEmptyWhenColumnMissing() throws Exception {
		Path csv = writeCsv("Number,Code,Event Name\n1,XY9G-AB7K-92QF,My Event\n");

		CsvReadResult result = service.readCodes(csv);

		assertThat(result.codes()).hasSize(1);
		assertThat(result.codes().getFirst().password()).isEmpty();
	}

	private Path writeCsv(String content) throws IOException {
		Path csv = tempDir.resolve("test.csv");
		Files.writeString(csv, content, StandardCharsets.UTF_8);
		return csv;
	}

}
