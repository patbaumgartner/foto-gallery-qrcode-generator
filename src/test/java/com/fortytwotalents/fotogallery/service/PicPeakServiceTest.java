package com.fortytwotalents.fotogallery.service;

import com.fortytwotalents.fotogallery.config.PicPeakProperties;
import com.fortytwotalents.fotogallery.model.GalleryCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PicPeakServiceTest {

	private static final String SHARE_LINK = "https://pics.example.com/gallery/abc123def456";

	private static final String LOGIN_SUCCESS_BODY = "{\"token\":\"test-token-abc\"}";

	private static final String EVENT_SUCCESS_BODY = "{\"share_link\":\"" + SHARE_LINK + "\"}";

	private static final String EVENT_FAILURE_BODY = "{\"password\":[\"invalid password\"]}";

	private static final HttpHeaders EMPTY_HEADERS = HttpHeaders.of(Map.of(), (k, v) -> true);

	@Mock
	private CodeGeneratorService codeGeneratorService;

	@Mock
	private HttpClient httpClient;

	private PicPeakProperties enabledProperties;

	private PicPeakService service;

	@BeforeEach
	void setUp() {
		enabledProperties = picPeakProps(true);
		service = new PicPeakService(enabledProperties, codeGeneratorService, httpClient);
	}

	@Test
	void returnsCodesUnchangedWhenDisabled() {
		service = new PicPeakService(picPeakProps(false), codeGeneratorService, httpClient);

		List<GalleryCode> codes = List.of(new GalleryCode("XY9G-AB7K-92QF", "pass1"));

		List<GalleryCode> result = service.enrichWithShareLinks(codes, "My Event");

		assertThat(result).isSameAs(codes);
	}

	@Test
	void returnsCodesUnchangedWhenLoginFails() throws Exception {
		HttpResponse<String> loginFailResponse = stubResponse(401, "Unauthorized");
		doReturn(loginFailResponse).when(httpClient).send(any(), any());

		List<GalleryCode> codes = List.of(new GalleryCode("XY9G-AB7K-92QF", "pass1"));

		List<GalleryCode> result = service.enrichWithShareLinks(codes, "My Event");

		assertThat(result).isSameAs(codes);
	}

	@Test
	void enrichesCodesWithShareLinksOnSuccess() throws Exception {
		HttpResponse<String> loginOk = stubLoginResponse(200, LOGIN_SUCCESS_BODY);
		HttpResponse<String> eventOk = stubResponse(201, EVENT_SUCCESS_BODY);
		doReturn(loginOk).doReturn(eventOk).when(httpClient).send(any(), any());

		List<GalleryCode> codes = List.of(new GalleryCode("XY9G-AB7K-92QF", "pass1"));

		List<GalleryCode> result = service.enrichWithShareLinks(codes, "My Event");

		assertThat(result).hasSize(1);
		assertThat(result.get(0).code()).isEqualTo("XY9G-AB7K-92QF");
		assertThat(result.get(0).password()).isEqualTo("pass1");
		assertThat(result.get(0).shareUrl()).isEqualTo(SHARE_LINK);
	}

	@Test
	void retriesWithNewPasswordOnEventCreationFailure() throws Exception {
		HttpResponse<String> loginOk = stubLoginResponse(200, LOGIN_SUCCESS_BODY);
		HttpResponse<String> eventFail = stubResponse(422, EVENT_FAILURE_BODY);
		HttpResponse<String> eventOk = stubResponse(201, EVENT_SUCCESS_BODY);

		when(codeGeneratorService.generatePassword()).thenReturn("newGeneratedPw");

		// 1: login, 2: event attempt 1 (fail), 3: event attempt 2 (success)
		doReturn(loginOk).doReturn(eventFail).doReturn(eventOk).when(httpClient).send(any(), any());

		List<GalleryCode> codes = List.of(new GalleryCode("XY9G-AB7K-92QF", "badPass!"));

		List<GalleryCode> result = service.enrichWithShareLinks(codes, "My Event");

		assertThat(result).hasSize(1);
		assertThat(result.get(0).code()).isEqualTo("XY9G-AB7K-92QF");
		assertThat(result.get(0).password()).isEqualTo("newGeneratedPw");
		assertThat(result.get(0).shareUrl()).isEqualTo(SHARE_LINK);
		verify(codeGeneratorService, times(1)).generatePassword();
	}

	@Test
	void throwsWhenAllRetriesAreExhausted() throws Exception {
		HttpResponse<String> loginOk = stubLoginResponse(200, LOGIN_SUCCESS_BODY);
		HttpResponse<String> eventFail = stubResponse(422, EVENT_FAILURE_BODY);

		when(codeGeneratorService.generatePassword()).thenReturn("retry1").thenReturn("retry2");

		// login + 3 consecutive failed event attempts
		doReturn(loginOk).doReturn(eventFail).doReturn(eventFail).doReturn(eventFail)
				.when(httpClient)
				.send(any(), any());

		List<GalleryCode> codes = List.of(new GalleryCode("XY9G-AB7K-92QF", "badPass!"));

		assertThatThrownBy(() -> service.enrichWithShareLinks(codes, "My Event"))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("XY9G-AB7K-92QF")
				.hasMessageContaining("3 attempts");
	}

	@SuppressWarnings("unchecked")
	private static HttpResponse<String> stubResponse(int status, String body) {
		HttpResponse<String> response = mock(HttpResponse.class);
		when(response.statusCode()).thenReturn(status);
		when(response.body()).thenReturn(body);
		return response;
	}

	@SuppressWarnings("unchecked")
	private static HttpResponse<String> stubLoginResponse(int status, String body) {
		HttpResponse<String> response = mock(HttpResponse.class);
		when(response.statusCode()).thenReturn(status);
		when(response.body()).thenReturn(body);
		when(response.headers()).thenReturn(EMPTY_HEADERS);
		return response;
	}

	private static PicPeakProperties picPeakProps(boolean enabled) {
		String apiUrl = enabled ? "https://pics.example.com" : "";
		String username = enabled ? "admin" : "";
		String password = enabled ? "secret" : "";
		return new PicPeakProperties(enabled, apiUrl, username, password, "schulfotos", "",
				"test@example.com", "", true, "", 30, false, true, true, true, false, false, false,
				true, true, true, false, false, false, false, false, "standard", "wave", 1,
				"default", "standard", "managed", "center", "medium", "top", null, null, null);
	}

}
