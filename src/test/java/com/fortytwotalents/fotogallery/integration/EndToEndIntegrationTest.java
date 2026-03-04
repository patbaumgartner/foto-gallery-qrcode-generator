package com.fortytwotalents.fotogallery.integration;

import com.fortytwotalents.fotogallery.model.GalleryCode;
import com.fortytwotalents.fotogallery.model.PdfOptions;
import com.fortytwotalents.fotogallery.service.CsvReaderService;
import com.fortytwotalents.fotogallery.service.PdfGeneratorService;
import com.fortytwotalents.fotogallery.service.QrCodeGeneratorService;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "app.mode=none")
class EndToEndIntegrationTest {

    @Autowired
    CsvReaderService csvReaderService;

    @Autowired
    QrCodeGeneratorService qrCodeGeneratorService;

    @Autowired
    PdfGeneratorService pdfGeneratorService;

    private static final String BASE_URL = "https://my.site/gallery/";

    @Test
    void fullPipelineProducesValidPdf(@TempDir Path tempDir) throws Exception {
        Path inputPath = tempDir.resolve("input.csv");
        Path outputPath = tempDir.resolve("output.pdf");

        Files.writeString(inputPath, "XY9G-AB7K-92QF\nTK2H-XY3M-88PL\nMN5R-ZZ99-AA11\n", StandardCharsets.UTF_8);

        // Execute the full pipeline manually
        var csvResult = csvReaderService.readCodes(inputPath);
        List<GalleryCode> codes = csvResult.codes();
        LinkedHashMap<GalleryCode, BufferedImage> qrImages = new LinkedHashMap<>();
        int number = 1;
        for (GalleryCode code : codes) {
            qrImages.put(code, qrCodeGeneratorService.generateQrCode(code, BASE_URL, 200, number++));
        }
        int pages = pdfGeneratorService.createPdf(codes, qrImages, BASE_URL,
                PdfOptions.of(outputPath, 3, 4));

        assertThat(outputPath).exists();
        assertThat(pages).isEqualTo(1);

        try (PDDocument doc = Loader.loadPDF(outputPath.toFile())) {
            assertThat(doc.getNumberOfPages()).isEqualTo(1);

            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(doc);
            assertThat(text).contains("XY9G-AB7K-92QF");
            assertThat(text).contains("TK2H-XY3M-88PL");
            assertThat(text).contains("MN5R-ZZ99-AA11");
        }
    }

    @Test
    void fullPipelineWithMultiplePages(@TempDir Path tempDir) throws Exception {
        Path inputPath = tempDir.resolve("input-multi.csv");
        Path outputPath = tempDir.resolve("output-multi.pdf");

        // Write 15 codes → should generate 2 pages (3x4=12 per page)
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 15; i++) {
            sb.append("%c%c%d%d-%c%c%d%d-%c%c%d%d\n".formatted('E' + i % 22, 'F' + i % 22, (4 + i) % 10, (5 + i) % 10,
                    'A' + i % 26, 'B' + i % 26, i % 10, (i + 1) % 10, 'C' + i % 26, 'D' + i % 26, (i + 2) % 10,
                    (i + 3) % 10));
        }
        Files.writeString(inputPath, sb.toString(), StandardCharsets.UTF_8);

        var csvResult2 = csvReaderService.readCodes(inputPath);
        List<GalleryCode> codes2 = csvResult2.codes();
        LinkedHashMap<GalleryCode, BufferedImage> qrImages2 = new LinkedHashMap<>();
        int num = 1;
        for (GalleryCode code : codes2) {
            qrImages2.put(code, qrCodeGeneratorService.generateQrCode(code, BASE_URL, 200, num++));
        }
        int pages = pdfGeneratorService.createPdf(codes2, qrImages2, BASE_URL,
                PdfOptions.of(outputPath, 3, 4));

        assertThat(outputPath).exists();
        assertThat(pages).isEqualTo(2);

        try (PDDocument doc = Loader.loadPDF(outputPath.toFile())) {
            assertThat(doc.getNumberOfPages()).isEqualTo(2);
        }
    }

}
