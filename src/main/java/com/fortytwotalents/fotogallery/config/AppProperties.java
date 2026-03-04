package com.fortytwotalents.fotogallery.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(String mode, String csvInputPath, String csvOutputPath, String outputPath, String baseUrl,
        int qrSize, int gridColumns, int gridRows, String eventCode, int codeCount, boolean showCuttingLines,
        String eventName) {
    public AppProperties {
        if (mode == null || mode.isBlank()) {
            mode = "generate-pdf";
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
