package com.fortytwotalents.fotogallery.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GalleryCodeTest {

	@Test
	void validCodeIsAccepted() {
		assertThat(GalleryCode.isValid("XY9G-AB7K-92QF")).isTrue();
		assertThat(GalleryCode.isValid("MN5R-ZZ99-AA11")).isTrue();
		assertThat(GalleryCode.isValid("0000-0000-0000")).isTrue();
		assertThat(GalleryCode.isValid("AAAA-BBBB-CCCC")).isTrue();
	}

	@Test
	void invalidCodesAreRejected() {
		assertThat(GalleryCode.isValid(null)).isFalse();
		assertThat(GalleryCode.isValid("")).isFalse();
		assertThat(GalleryCode.isValid("XY9GAB7K92QF")).isFalse(); // No dashes
		assertThat(GalleryCode.isValid("AB7K-92QF")).isFalse(); // Only two groups
		assertThat(GalleryCode.isValid("XY9G-AB7K-92Q")).isFalse(); // Third group too
																	// short
		assertThat(GalleryCode.isValid("XY9G-AB7K-92QFF")).isFalse(); // Third group too
																		// long
		assertThat(GalleryCode.isValid("xy9g-ab7k-92qf")).isFalse(); // Lowercase
		assertThat(GalleryCode.isValid("XY9G-AB7K-92Q!")).isFalse(); // Special char
		assertThat(GalleryCode.isValid(" XY9G-AB7K-92QF")).isFalse(); // Leading space
	}

	@Test
	void toUrlConcatenatesCorrectly() {
		GalleryCode code = new GalleryCode("XY9G-AB7K-92QF");

		String url = code.toUrl("https://my.site/gallery/");

		assertThat(url).isEqualTo("https://my.site/gallery/XY9G-AB7K-92QF");
	}

	@Test
	void toUrlUsesShareUrlWhenSet() {
		GalleryCode code = new GalleryCode("XY9G-AB7K-92QF", "pass", "https://pics.example.com/gallery/abc123");

		assertThat(code.toUrl("https://my.site/gallery/")).isEqualTo("https://pics.example.com/gallery/abc123");
	}

	@Test
	void toUrlFallsBackToBaseUrlWhenShareUrlEmpty() {
		GalleryCode code = new GalleryCode("XY9G-AB7K-92QF", "pass", "");

		assertThat(code.toUrl("https://my.site/gallery/")).isEqualTo("https://my.site/gallery/XY9G-AB7K-92QF");
	}

	@Test
	void singleArgConstructorSetsEmptyPassword() {
		GalleryCode code = new GalleryCode("XY9G-AB7K-92QF");

		assertThat(code.password()).isEmpty();
	}

	@Test
	void twoArgConstructorStoresPassword() {
		GalleryCode code = new GalleryCode("XY9G-AB7K-92QF", "ABC1234");

		assertThat(code.code()).isEqualTo("XY9G-AB7K-92QF");
		assertThat(code.password()).isEqualTo("ABC1234");
	}

	@Test
	void nullPasswordNormalisedToEmpty() {
		GalleryCode code = new GalleryCode("XY9G-AB7K-92QF", null);

		assertThat(code.password()).isEmpty();
	}

}
