package com.fortytwotalents.fotogallery.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app")
public record AppProperties(String mode, String csvInputPath, String csvOutputPath, String outputPath,
		@NotBlank String baseUrl, @Positive int qrSize, @Positive int gridColumns, @Positive int gridRows,
		String eventCode, @Positive int codeCount, boolean showCuttingLines, String eventName) {
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
			baseUrl = "https://my.site/gallery/";
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
		if (!baseUrl.endsWith("/")) {
			baseUrl = baseUrl + "/";
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
	}
}
