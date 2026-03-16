package com.fortytwotalents.fotogallery.service;

import com.fortytwotalents.fotogallery.model.CsvReadResult;
import com.fortytwotalents.fotogallery.model.GalleryCode;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
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

	public CsvReadResult readCodes(Path csvFile) throws IOException {
		if (!Files.exists(csvFile)) {
			throw new IOException("CSV file not found: " + csvFile.toAbsolutePath());
		}

		LinkedHashSet<String> seenCodes = new LinkedHashSet<>();
		List<GalleryCode> codes = new ArrayList<>();
		String eventName = "";

		// Detect if the CSV has a header row by peeking at the first line
		boolean hasHeader = hasHeaderRow(csvFile);

		CSVFormat.Builder formatBuilder = CSVFormat.DEFAULT.builder().setIgnoreEmptyLines(true).setTrim(true);
		if (hasHeader) {
			formatBuilder.setSkipHeaderRecord(true).setHeader();
		}
		CSVFormat format = formatBuilder.get();

		try (Reader reader = Files.newBufferedReader(csvFile, StandardCharsets.UTF_8);
				CSVParser parser = format.parse(reader)) {

			boolean hasCodeColumn = hasHeader && parser.getHeaderNames().contains("Code");
			boolean hasEventNameColumn = hasHeader && parser.getHeaderNames().contains("Event Name");
			boolean hasPasswordColumn = hasHeader && parser.getHeaderNames().contains("Password");

			for (CSVRecord record : parser) {
				if (record.size() == 0) {
					continue;
				}

				String rawCode = hasCodeColumn ? record.get("Code").trim() : record.get(0).trim();

				// Extract event name from first data row if available
				if (hasEventNameColumn && eventName.isEmpty()) {
					eventName = record.get("Event Name").trim();
				}

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

				String password = hasPasswordColumn ? record.get("Password").trim() : "";
				codes.add(new GalleryCode(rawCode, password));
			}
		}

		LOGGER.atInfo().addArgument(() -> codes.size()).addArgument(csvFile).log("Read {} valid gallery codes from {}");
		return new CsvReadResult(codes, eventName);
	}

	private boolean hasHeaderRow(Path csvFile) throws IOException {
		try (BufferedReader br = Files.newBufferedReader(csvFile, StandardCharsets.UTF_8)) {
			String firstLine = br.readLine();
			if (firstLine == null) {
				return false;
			}
			// Strip BOM if present
			if (firstLine.startsWith("\uFEFF")) {
				firstLine = firstLine.substring(1);
			}
			return firstLine.startsWith("Number,");
		}
	}

}
