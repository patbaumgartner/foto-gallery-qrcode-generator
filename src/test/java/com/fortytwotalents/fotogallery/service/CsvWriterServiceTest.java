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

        service.writeCodes(codes, output, "My Event");

        assertThat(output).exists();
        List<String> lines = Files.readAllLines(output, StandardCharsets.UTF_8);
        assertThat(lines).hasSize(4);
        assertThat(lines.get(0)).isEqualTo("Number,Code,Event Name");
        assertThat(lines.get(1)).isEqualTo("1,XY9G-AB7K-92QF,My Event");
        assertThat(lines.get(2)).isEqualTo("2,XY9G-TK2H-88PL,My Event");
        assertThat(lines.get(3)).isEqualTo("3,XY9G-MN5R-AA11,My Event");
    }

    @Test
    void writesEmptyFile() throws Exception {
        Path output = tempDir.resolve("empty.csv");

        service.writeCodes(List.of(), output, "");

        assertThat(output).exists();
        List<String> lines = Files.readAllLines(output, StandardCharsets.UTF_8);
        assertThat(lines).hasSize(1);
        assertThat(lines.get(0)).isEqualTo("Number,Code,Event Name");
    }

    @Test
    void outputIsReadableByReaderService() throws Exception {
        CsvReaderService readerService = new CsvReaderService();
        List<GalleryCode> codes = List.of(new GalleryCode("XY9G-AB7K-92QF"), new GalleryCode("XY9G-TK2H-88PL"));
        Path output = tempDir.resolve("roundtrip.csv");

        service.writeCodes(codes, output, "Test Event");
        var result = readerService.readCodes(output);

        assertThat(result.codes()).isEqualTo(codes);
        assertThat(result.eventName()).isEqualTo("Test Event");
    }

}
