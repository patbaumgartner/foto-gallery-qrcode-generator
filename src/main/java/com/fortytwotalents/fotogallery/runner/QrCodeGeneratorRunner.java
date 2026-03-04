package com.fortytwotalents.fotogallery.runner;

import com.fortytwotalents.fotogallery.config.AppProperties;
import com.fortytwotalents.fotogallery.model.GalleryCode;
import com.fortytwotalents.fotogallery.service.CsvReaderService;
import com.fortytwotalents.fotogallery.service.PdfGeneratorService;
import com.fortytwotalents.fotogallery.service.QrCodeGeneratorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;

@Component
@ConditionalOnProperty(name = "app.mode", havingValue = "generate-pdf", matchIfMissing = true)
public class QrCodeGeneratorRunner implements CommandLineRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(QrCodeGeneratorRunner.class);

    private final CsvReaderService csvReaderService;

    private final QrCodeGeneratorService qrCodeGeneratorService;

    private final PdfGeneratorService pdfGeneratorService;

    private final AppProperties appProperties;

    public QrCodeGeneratorRunner(CsvReaderService csvReaderService, QrCodeGeneratorService qrCodeGeneratorService,
            PdfGeneratorService pdfGeneratorService, AppProperties appProperties) {
        this.csvReaderService = csvReaderService;
        this.qrCodeGeneratorService = qrCodeGeneratorService;
        this.pdfGeneratorService = pdfGeneratorService;
        this.appProperties = appProperties;
    }

    @Override
    public void run(String... args) throws IOException {
        Path inputPath = Path.of(appProperties.inputPath());
        Path outputPath = Path.of(appProperties.outputPath());

        LOGGER.atInfo().addArgument(() -> inputPath.toAbsolutePath()).log("Reading gallery codes from: {}");

        List<GalleryCode> codes = csvReaderService.readCodes(inputPath);

        if (codes.isEmpty()) {
            LOGGER.warn("No valid gallery codes found in {}. No PDF generated.", inputPath);
            return;
        }

        LOGGER.atInfo().addArgument(() -> codes.size()).log("Generating QR codes for {} gallery codes...");

        LinkedHashMap<GalleryCode, BufferedImage> qrImages = new LinkedHashMap<>();
        for (GalleryCode code : codes) {
            BufferedImage qrImage = qrCodeGeneratorService.generateQrCode(code, appProperties.baseUrl(),
                    appProperties.qrSize());
            qrImages.put(code, qrImage);
        }

        int pages = pdfGeneratorService.createPdf(codes, qrImages, appProperties.baseUrl(), outputPath,
                appProperties.gridColumns(), appProperties.gridRows(), appProperties.showCuttingLines());

        LOGGER.atInfo()
                .addArgument(() -> codes.size())
                .addArgument(pages)
                .addArgument(() -> outputPath.toAbsolutePath())
                .log("Done! Generated PDF with {} QR codes on {} page(s): {}");
    }

}
