package com.fortytwotalents.fotogallery.runner;

import com.fortytwotalents.fotogallery.config.AppProperties;
import com.fortytwotalents.fotogallery.model.GalleryCode;
import com.fortytwotalents.fotogallery.service.CodeGeneratorService;
import com.fortytwotalents.fotogallery.service.CsvWriterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

	private final AppProperties appProperties;

	public CodeGeneratorRunner(CodeGeneratorService codeGeneratorService, CsvWriterService csvWriterService,
			AppProperties appProperties) {
		this.codeGeneratorService = codeGeneratorService;
		this.csvWriterService = csvWriterService;
		this.appProperties = appProperties;
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
		csvWriterService.writeCodes(codes, outputPath, appProperties.eventName(), appProperties.baseUrl());

		LOGGER.atInfo()
			.addArgument(() -> codes.size())
			.addArgument(() -> outputPath.toAbsolutePath())
			.log("Done! Generated {} codes written to: {}");
	}

}
