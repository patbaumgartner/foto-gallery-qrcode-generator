package com.fortytwotalents.fotogallery.service;

import com.fortytwotalents.fotogallery.model.GalleryCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CodeGeneratorServiceTest {

	private CodeGeneratorService service;

	@BeforeEach
	void setUp() {
		service = new CodeGeneratorService();
	}

	@Test
	void generatesRequestedNumberOfCodes() {
		List<GalleryCode> codes = service.generateCodes("XY9G", 10);

		assertThat(codes).hasSize(10);
	}

	@Test
	void allCodesStartWithEventPrefix() {
		List<GalleryCode> codes = service.generateCodes("XY9G", 20);

		assertThat(codes).allSatisfy(code -> assertThat(code.code()).startsWith("XY9G-"));
	}

	@Test
	void allCodesMatchFormat() {
		List<GalleryCode> codes = service.generateCodes("AB1C", 50);

		assertThat(codes).allSatisfy(code -> assertThat(GalleryCode.isValid(code.code())).isTrue());
	}

	@Test
	void generatesUniqueCodes() {
		List<GalleryCode> codes = service.generateCodes("XY9G", 100);

		assertThat(codes).extracting(GalleryCode::code).doesNotHaveDuplicates();
	}

	@Test
	void acceptsLowercaseEventCode() {
		List<GalleryCode> codes = service.generateCodes("xy9g", 5);

		assertThat(codes).allSatisfy(code -> assertThat(code.code()).startsWith("XY9G-"));
	}

	@Test
	void rejectsEmptyEventCode() {
		assertThatThrownBy(() -> service.generateCodes("", 10)).isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("must not be empty");
	}

	@Test
	void rejectsNullEventCode() {
		assertThatThrownBy(() -> service.generateCodes(null, 10)).isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("must not be empty");
	}

	@Test
	void rejectsInvalidEventCode() {
		assertThatThrownBy(() -> service.generateCodes("TOOLONG", 10)).isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("exactly 4 alphanumeric");
	}

	@Test
	void rejectsZeroCount() {
		assertThatThrownBy(() -> service.generateCodes("XY9G", 0)).isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("positive");
	}

	@Test
	void generatesPasswordForEachCode() {
		List<GalleryCode> codes = service.generateCodes("XY9G", 10);

		assertThat(codes).allSatisfy(code -> {
			assertThat(code.password()).isNotBlank();
			assertThat(code.password()).hasSize(9);
		});
	}

	@Test
	void passwordsMatchRequiredPattern() {
		List<GalleryCode> codes = service.generateCodes("XY9G", 50);

		assertThat(codes).allSatisfy(
				code -> assertThat(code.password()).matches("^[A-Za-km-z234679!@#$%&+]+$"));
	}

	@Test
	void passwordsHaveNoRepeatedCharacters() {
		List<GalleryCode> codes = service.generateCodes("XY9G", 50);

		assertThat(codes).allSatisfy(code -> {
			String pw = code.password();
			assertThat(pw.chars().distinct().count()).isEqualTo(pw.length());
		});
	}

	@Test
	void passwordsDoNotContainAmbiguousOrExcludedCharacters() {
		List<GalleryCode> codes = service.generateCodes("XY9G", 50);

		assertThat(codes).allSatisfy(code -> assertThat(code.password()).doesNotContain("0", "1", "5", "8", "l", ".", "-", "_", "*"));
	}

	@Test
	void passwordsAlwaysContainAtLeastOneDigit() {
		List<GalleryCode> codes = service.generateCodes("XY9G", 200);

		assertThat(codes).allSatisfy(code -> assertThat(code.password()).containsPattern("[234679]"));
	}

	@Test
	void passwordsAlwaysContainAtLeastOneUppercaseLetter() {
		List<GalleryCode> codes = service.generateCodes("XY9G", 200);

		assertThat(codes).allSatisfy(code -> assertThat(code.password()).containsPattern("[A-Z]"));
	}

	@Test
	void passwordsAlwaysContainAtLeastOneLowercaseLetter() {
		List<GalleryCode> codes = service.generateCodes("XY9G", 200);

		assertThat(codes).allSatisfy(code -> assertThat(code.password()).containsPattern("[a-km-z]"));
	}

	@Test
	void passwordsAlwaysContainAtLeastOneSpecialCharacter() {
		List<GalleryCode> codes = service.generateCodes("XY9G", 200);

		assertThat(codes).allSatisfy(code -> assertThat(code.password()).containsPattern("[!@#$%&+]"));
	}

	@Test
	void generatesUniquePasswords() {
		List<GalleryCode> codes = service.generateCodes("XY9G", 50);

		assertThat(codes).extracting(GalleryCode::password).doesNotHaveDuplicates();
	}

	@Test
	void passwordRetriesOnCollisionUntilUnique() {
		// Subclass overrides generatePassword() to return the same value for the first
		// N calls, forcing collisions and exercising the retry loop.
		AtomicInteger callCount = new AtomicInteger();
		CodeGeneratorService collidingService = new CodeGeneratorService() {
			@Override
			String generatePassword() {
				// Return a fixed password for the first 3 calls, then delegate to the real
				// implementation to produce unique passwords for subsequent codes.
				return callCount.getAndIncrement() < 3 ? "FIXED-PW!" : super.generatePassword();
			}
		};

		List<GalleryCode> codes = collidingService.generateCodes("XY9G", 3);

		assertThat(codes).extracting(GalleryCode::password).doesNotHaveDuplicates();
	}

}
