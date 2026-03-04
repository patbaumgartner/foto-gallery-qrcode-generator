package com.fortytwotalents.fotogallery.model;

import java.nio.file.Path;

/**
 * Configuration options for PDF generation.
 */
public record PdfOptions(
        Path outputPath,
        int gridColumns,
        int gridRows,
        boolean showCuttingLines,
        String eventName) {

    public PdfOptions {
        if (eventName == null) {
            eventName = "";
        }
    }

    public static PdfOptions of(Path outputPath, int gridColumns, int gridRows) {
        return new PdfOptions(outputPath, gridColumns, gridRows, false, "");
    }

    public static PdfOptions of(Path outputPath, int gridColumns, int gridRows, boolean showCuttingLines) {
        return new PdfOptions(outputPath, gridColumns, gridRows, showCuttingLines, "");
    }
}
