package com.fortytwotalents.fotogallery.service;

import com.fortytwotalents.fotogallery.model.GalleryCode;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

@Service
public class CsvReaderService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CsvReaderService.class);

    public List<GalleryCode> readCodes(Path csvFile) throws IOException {
        if (!Files.exists(csvFile)) {
            throw new IOException("CSV file not found: " + csvFile.toAbsolutePath());
        }

        LinkedHashSet<String> seenCodes = new LinkedHashSet<>();
        List<GalleryCode> codes = new ArrayList<>();

        CSVFormat format = CSVFormat.DEFAULT.builder().setIgnoreEmptyLines(true).setTrim(true).get();

        try (Reader reader = Files.newBufferedReader(csvFile, StandardCharsets.UTF_8);
                CSVParser parser = format.parse(reader)) {

            for (CSVRecord record : parser) {
                if (record.size() == 0) {
                    continue;
                }

                String rawCode = record.get(0).trim();

                // Skip BOM if present on first record
                if (rawCode.startsWith("\uFEFF")) {
                    rawCode = rawCode.substring(1);
                }

                if (rawCode.isBlank()) {
                    continue;
                }

                if (!GalleryCode.isValid(rawCode)) {
                    LOGGER.warn("Skipping invalid gallery code at line {}: '{}'", record.getRecordNumber(), rawCode);
                    continue;
                }

                if (!seenCodes.add(rawCode)) {
                    LOGGER.warn("Skipping duplicate gallery code at line {}: '{}'", record.getRecordNumber(), rawCode);
                    continue;
                }

                codes.add(new GalleryCode(rawCode));
            }
        }

        LOGGER.atInfo().addArgument(() -> codes.size()).addArgument(csvFile).log("Read {} valid gallery codes from {}");
        return codes;
    }

}
