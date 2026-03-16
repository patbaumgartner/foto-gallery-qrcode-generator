package com.fortytwotalents.fotogallery.model;

import java.nio.file.Path;

/**
 * Configuration options for PDF generation.
 */
public record PdfOptions(Path outputPath, int gridColumns, int gridRows, boolean showCuttingLines, String eventName,
		String galleryUrl, String logoUrl) {

	public PdfOptions {
		if (eventName == null) {
			eventName = "";
		}
		if (galleryUrl == null) {
			galleryUrl = "";
		}
		if (logoUrl == null) {
			logoUrl = "";
		}
	}

	public PdfOptions(Path outputPath, int gridColumns, int gridRows, boolean showCuttingLines, String eventName) {
		this(outputPath, gridColumns, gridRows, showCuttingLines, eventName, "", "");
	}

	public static PdfOptions of(Path outputPath, int gridColumns, int gridRows) {
		return new PdfOptions(outputPath, gridColumns, gridRows, false, "", "", "");
	}

	public static PdfOptions of(Path outputPath, int gridColumns, int gridRows, boolean showCuttingLines) {
		return new PdfOptions(outputPath, gridColumns, gridRows, showCuttingLines, "", "", "");
	}

}
