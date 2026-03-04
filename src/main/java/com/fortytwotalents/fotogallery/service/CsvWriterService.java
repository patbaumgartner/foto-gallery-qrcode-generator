package com.fortytwotalents.fotogallery.service;

import com.fortytwotalents.fotogallery.model.GalleryCode;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Service
public class CsvWriterService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CsvWriterService.class);

    public void writeCodes(List<GalleryCode> codes, Path outputPath) throws IOException {
        CSVFormat format = CSVFormat.DEFAULT.builder().get();

        try (Writer writer = Files.newBufferedWriter(outputPath, StandardCharsets.UTF_8);
                CSVPrinter printer = new CSVPrinter(writer, format)) {
            for (GalleryCode code : codes) {
                printer.printRecord(code.code());
            }
        }

        LOGGER.atInfo().addArgument(() -> codes.size()).addArgument(outputPath).log("Wrote {} gallery codes to {}");
    }

}
