package com.fortytwotalents.fotogallery.model;

import java.nio.file.Path;

/**
 * Configuration options for PDF generation.
 */
public record PdfOptions(Path outputPath, int gridColumns, int gridRows, boolean showCuttingLines, String eventName,
		String baseUrl, String logoUrl, String galleryCodeLabel, String galleryPasswordLabel) {

	public PdfOptions {
		if (eventName == null) {
			eventName = "";
		}
		if (baseUrl == null) {
			baseUrl = "";
		}
		if (logoUrl == null) {
			logoUrl = "";
		}
		if (galleryCodeLabel == null || galleryCodeLabel.isBlank()) {
			galleryCodeLabel = "GALERIE-CODE";
		}
		if (galleryPasswordLabel == null || galleryPasswordLabel.isBlank()) {
			galleryPasswordLabel = "GALERIE-PASSWORT";
		}
	}

	public PdfOptions(Path outputPath, int gridColumns, int gridRows, boolean showCuttingLines, String eventName) {
		this(outputPath, gridColumns, gridRows, showCuttingLines, eventName, "", "", "GALERIE-CODE",
				"GALERIE-PASSWORT");
	}

	public PdfOptions(Path outputPath, int gridColumns, int gridRows, boolean showCuttingLines, String eventName,
			String baseUrl, String logoUrl) {
		this(outputPath, gridColumns, gridRows, showCuttingLines, eventName, baseUrl, logoUrl, "GALERIE-CODE",
				"GALERIE-PASSWORT");
	}

	public static PdfOptions of(Path outputPath, int gridColumns, int gridRows) {
		return new PdfOptions(outputPath, gridColumns, gridRows, false, "", "", "", "GALERIE-CODE",
				"GALERIE-PASSWORT");
	}

	public static PdfOptions of(Path outputPath, int gridColumns, int gridRows, boolean showCuttingLines) {
		return new PdfOptions(outputPath, gridColumns, gridRows, showCuttingLines, "", "", "", "GALERIE-CODE",
				"GALERIE-PASSWORT");
	}

}
