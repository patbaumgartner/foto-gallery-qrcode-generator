package com.fortytwotalents.fotogallery.runner;

import com.fortytwotalents.fotogallery.config.AppProperties;
import com.fortytwotalents.fotogallery.model.GalleryCode;
import com.fortytwotalents.fotogallery.service.CodeGeneratorService;
import com.fortytwotalents.fotogallery.service.CsvWriterService;
import com.fortytwotalents.fotogallery.service.PicPeakService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

@Component
public class CodeGeneratorRunner implements CommandLineRunner {

	private static final Logger LOGGER = LoggerFactory.getLogger(CodeGeneratorRunner.class);

	private final CodeGeneratorService codeGeneratorService;

	private final CsvWriterService csvWriterService;

	private final PicPeakService picPeakService;

	private final AppProperties appProperties;

	@Autowired
	public CodeGeneratorRunner(CodeGeneratorService codeGeneratorService, CsvWriterService csvWriterService,
			PicPeakService picPeakService, AppProperties appProperties) {
		this.codeGeneratorService = codeGeneratorService;
		this.csvWriterService = csvWriterService;
		this.picPeakService = picPeakService;
		this.appProperties = appProperties;
	}

	// Backward-compatible constructor for tests and InteractiveRunner (PicPeak disabled)
	CodeGeneratorRunner(CodeGeneratorService codeGeneratorService, CsvWriterService csvWriterService,
			AppProperties appProperties) {
		this(codeGeneratorService, csvWriterService, null, appProperties);
	}

	@Override
	public void run(String... args) throws IOException {
		if (!"generate-codes".equals(appProperties.mode())) {
			return;
		}

		String eventCode = appProperties.eventCode();
		int codeCount = appProperties.codeCount();
		Path outputPath = Path.of(appProperties.csvOutputPath());

		if (eventCode.isBlank()) {
			LOGGER.error("Event code is required. Set --app.event-code=XXXX or pass as first argument.");
			return;
		}

		LOGGER.info("Generating {} gallery codes with event prefix '{}'...", codeCount, eventCode);

		List<GalleryCode> codes = codeGeneratorService.generateCodes(eventCode, codeCount);

		if (picPeakService != null) {
			codes = picPeakService.enrichWithShareLinks(codes, appProperties.eventName());
		}

		csvWriterService.writeCodes(codes, outputPath, appProperties.eventName(), appProperties.baseUrl());

		int finalCount = codes.size();
		LOGGER.atInfo()
			.addArgument(() -> finalCount)
			.addArgument(() -> outputPath.toAbsolutePath())
			.log("Done! Generated {} codes written to: {}");
	}

}
