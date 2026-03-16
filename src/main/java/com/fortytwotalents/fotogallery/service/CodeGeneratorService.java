package com.fortytwotalents.fotogallery.service;

import com.fortytwotalents.fotogallery.model.GalleryCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

@Service
public class CodeGeneratorService {

	private static final Logger LOGGER = LoggerFactory.getLogger(CodeGeneratorService.class);

	private static final String CHARSET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

	private static final int GROUP_LENGTH = 4;

	// Excluded from PASSWORD_CHARSET:
	//   digits 0,1,5,8  — visually similar to O/I/S/B
	//   lowercase l     — visually similar to 1 and I
	//   . - _ *         — explicitly excluded for readability
	private static final String PASSWORD_CHARSET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz234679!@#$%&+";

	private static final int PASSWORD_LENGTH = 9;

	static {
		if (PASSWORD_CHARSET.length() < PASSWORD_LENGTH) {
			throw new IllegalStateException("PASSWORD_CHARSET length (" + PASSWORD_CHARSET.length()
					+ ") must be >= PASSWORD_LENGTH (" + PASSWORD_LENGTH + ")");
		}
	}

	private final SecureRandom random = new SecureRandom();

	public List<GalleryCode> generateCodes(String eventCode, int count) {
		if (eventCode == null || eventCode.isBlank()) {
			throw new IllegalArgumentException("Event code must not be empty");
		}
		eventCode = eventCode.trim().toUpperCase();
		if (!eventCode.matches("^[A-Z0-9]{4}$")) {
			throw new IllegalArgumentException(
					"Event code must be exactly 4 alphanumeric characters, got: '" + eventCode + "'");
		}
		if (count <= 0) {
			throw new IllegalArgumentException("Code count must be positive, got: " + count);
		}

		LinkedHashSet<String> uniqueCodes = new LinkedHashSet<>();
		int maxAttempts = count * 10;
		int attempts = 0;

		while (uniqueCodes.size() < count && attempts < maxAttempts) {
			String group2 = randomGroup();
			String group3 = randomGroup();
			String code = eventCode + "-" + group2 + "-" + group3;
			uniqueCodes.add(code);
			attempts++;
		}

		if (uniqueCodes.size() < count) {
			LOGGER.warn("Could only generate {} unique codes out of {} requested", uniqueCodes.size(), count);
		}

		List<GalleryCode> codes = new ArrayList<>();
		for (String code : uniqueCodes) {
			codes.add(new GalleryCode(code, generatePassword()));
		}

		LOGGER.atInfo()
			.addArgument(() -> codes.size())
			.addArgument(eventCode)
			.log("Generated {} unique gallery codes with event prefix '{}'");
		return codes;
	}

	private String randomGroup() {
		StringBuilder sb = new StringBuilder(GROUP_LENGTH);
		for (int i = 0; i < GROUP_LENGTH; i++) {
			sb.append(CHARSET.charAt(random.nextInt(CHARSET.length())));
		}
		return sb.toString();
	}

	private String generatePassword() {
		List<Character> available = new ArrayList<>(PASSWORD_CHARSET.length());
		for (int i = 0; i < PASSWORD_CHARSET.length(); i++) {
			available.add(PASSWORD_CHARSET.charAt(i));
		}
		Collections.shuffle(available, random);
		StringBuilder sb = new StringBuilder(PASSWORD_LENGTH);
		for (int i = 0; i < PASSWORD_LENGTH; i++) {
			sb.append(available.get(i));
		}
		return sb.toString();
	}

}
