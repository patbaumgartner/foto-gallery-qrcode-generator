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

	// Excluded from password character classes:
	//   digits 0,1,5,8  — visually similar to O/I/S/B
	//   lowercase l     — visually similar to 1 and I
	//   . - _ *         — explicitly excluded for readability
	private static final String PASSWORD_DIGITS = "234679";

	private static final String PASSWORD_UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

	private static final String PASSWORD_LOWERCASE = "abcdefghijkmnopqrstuvwxyz";

	private static final String PASSWORD_SPECIAL = "!@#$%&+";

	private static final String PASSWORD_CHARSET = PASSWORD_UPPERCASE + PASSWORD_LOWERCASE + PASSWORD_DIGITS
			+ PASSWORD_SPECIAL;

	private static final int PASSWORD_LENGTH = 9;

	// Number of required character classes; each must appear at least once in every password
	private static final int REQUIRED_CLASSES = 4;

	static {
		if (PASSWORD_CHARSET.length() < PASSWORD_LENGTH) {
			throw new IllegalStateException("PASSWORD_CHARSET length (" + PASSWORD_CHARSET.length()
					+ ") must be >= PASSWORD_LENGTH (" + PASSWORD_LENGTH + ")");
		}
		if (PASSWORD_LENGTH < REQUIRED_CLASSES) {
			throw new IllegalStateException("PASSWORD_LENGTH (" + PASSWORD_LENGTH
					+ ") must be >= REQUIRED_CLASSES (" + REQUIRED_CLASSES + ")");
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

	String generatePassword() {
		// Guarantee at least one character from each required character class
		List<Character> mandatory = new ArrayList<>(REQUIRED_CLASSES);
		mandatory.add(PASSWORD_DIGITS.charAt(random.nextInt(PASSWORD_DIGITS.length())));
		mandatory.add(PASSWORD_UPPERCASE.charAt(random.nextInt(PASSWORD_UPPERCASE.length())));
		mandatory.add(PASSWORD_LOWERCASE.charAt(random.nextInt(PASSWORD_LOWERCASE.length())));
		mandatory.add(PASSWORD_SPECIAL.charAt(random.nextInt(PASSWORD_SPECIAL.length())));

		// Fill remaining positions from the full charset, excluding already-selected characters
		List<Character> pool = new ArrayList<>(PASSWORD_CHARSET.length());
		for (int i = 0; i < PASSWORD_CHARSET.length(); i++) {
			char c = PASSWORD_CHARSET.charAt(i);
			if (!mandatory.contains(c)) {
				pool.add(c);
			}
		}
		Collections.shuffle(pool, random);

		List<Character> passwordChars = new ArrayList<>(PASSWORD_LENGTH);
		passwordChars.addAll(mandatory);
		for (int i = 0; i < PASSWORD_LENGTH - REQUIRED_CLASSES; i++) {
			passwordChars.add(pool.get(i));
		}

		Collections.shuffle(passwordChars, random);
		StringBuilder sb = new StringBuilder(PASSWORD_LENGTH);
		for (char c : passwordChars) {
			sb.append(c);
		}
		return sb.toString();
	}

}
