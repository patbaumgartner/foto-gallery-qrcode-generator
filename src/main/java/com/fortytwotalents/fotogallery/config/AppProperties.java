package com.fortytwotalents.fotogallery.config;

import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app")
public record AppProperties(String mode, String csvInputPath, String csvOutputPath, String outputPath,
		String baseUrl, @Positive int qrSize, @Positive int gridColumns, @Positive int gridRows,
		String eventCode, @Positive int codeCount, boolean showCuttingLines, String eventName, String galleryUrl,
		String logoUrl) {
	public AppProperties {
		if (mode == null) {
			mode = "";
		}
		if (csvInputPath == null || csvInputPath.isBlank()) {
			csvInputPath = "codes.csv";
		}
		if (csvOutputPath == null || csvOutputPath.isBlank()) {
			csvOutputPath = "codes.csv";
		}
		if (outputPath == null || outputPath.isBlank()) {
			outputPath = "qr-codes.pdf";
		}
		if (baseUrl == null || baseUrl.isBlank()) {
			baseUrl = "https://my.site";
		}
		if (qrSize <= 0) {
			qrSize = 200;
		}
		if (gridColumns <= 0) {
			gridColumns = 3;
		}
		if (gridRows <= 0) {
			gridRows = 4;
		}
		if (eventCode == null) {
			eventCode = "";
		}
		if (codeCount <= 0) {
			codeCount = 50;
		}
		if (eventName == null) {
			eventName = "";
		}
		if (galleryUrl == null) {
			galleryUrl = "";
		}
		if (!galleryUrl.isBlank() && !galleryUrl.startsWith("https://")) {
			throw new IllegalArgumentException("app.gallery-url must start with https://: " + galleryUrl);
		}
		if (galleryUrl.isBlank()) {
			galleryUrl = "https://my.site/gallery?code=";
		}
		if (logoUrl == null) {
			logoUrl = "";
		}
	}
}
