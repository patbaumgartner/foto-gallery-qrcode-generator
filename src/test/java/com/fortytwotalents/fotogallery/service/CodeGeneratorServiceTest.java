package com.fortytwotalents.fotogallery.service;

import com.fortytwotalents.fotogallery.model.GalleryCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

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

		assertThat(codes).doesNotHaveDuplicates();
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
				code -> assertThat(code.password()).matches("^[A-Za-z0-9!@#$%&*+\\-_.]+$"));
	}

	@Test
	void generatesUniquePasswords() {
		List<GalleryCode> codes = service.generateCodes("XY9G", 50);

		assertThat(codes).extracting(GalleryCode::password).doesNotHaveDuplicates();
	}

}
