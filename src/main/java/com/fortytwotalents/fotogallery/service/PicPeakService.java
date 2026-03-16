package com.fortytwotalents.fotogallery.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortytwotalents.fotogallery.config.PicPeakProperties;
import com.fortytwotalents.fotogallery.model.GalleryCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class PicPeakService {

	private static final Logger LOGGER = LoggerFactory.getLogger(PicPeakService.class);

	private final PicPeakProperties picPeakProperties;

	private final ObjectMapper objectMapper;

	private final HttpClient httpClient;

	public PicPeakService(PicPeakProperties picPeakProperties) {
		this.picPeakProperties = picPeakProperties;
		this.objectMapper = new ObjectMapper();
		this.httpClient = HttpClient.newHttpClient();
	}

	public List<GalleryCode> enrichWithShareLinks(List<GalleryCode> codes, String eventName) {
		if (!picPeakProperties.enabled()) {
			return codes;
		}

		LOGGER.info("PicPeak integration enabled. Creating {} gallery events...", codes.size());

		String token = login();
		if (token == null) {
			LOGGER.error("PicPeak login failed. Returning codes without share links.");
			return codes;
		}

		List<GalleryCode> enrichedCodes = new ArrayList<>(codes.size());
		int number = 1;
		for (GalleryCode code : codes) {
			String shareLink = createEvent(token, code, eventName, number);
			if (shareLink != null && !shareLink.isBlank()) {
				enrichedCodes.add(new GalleryCode(code.code(), code.password(), shareLink));
				LOGGER.atInfo().addArgument(number).addArgument(shareLink).log("Created PicPeak event #{}: {}");
			}
			else {
				enrichedCodes.add(code);
				LOGGER.warn("Failed to create PicPeak event for code #{}: {}", number, code.code());
			}
			number++;
		}

		return enrichedCodes;
	}

	private String login() {
		try {
			ObjectNode body = objectMapper.createObjectNode();
			body.put("username", picPeakProperties.username());
			body.put("password", picPeakProperties.password());
			body.putNull("recaptchaToken");
			String requestBody = objectMapper.writeValueAsString(body);

			HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(picPeakProperties.apiUrl() + "/api/auth/admin/login"))
				.header("Content-Type", "application/json")
				.header("Accept", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(requestBody))
				.build();

			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

			if (response.statusCode() != 200) {
				LOGGER.error("PicPeak login failed with status {}: {}", response.statusCode(), response.body());
				return null;
			}

			// Try to extract token from Set-Cookie header
			String tokenFromCookie = response.headers()
				.allValues("Set-Cookie")
				.stream()
				.filter(c -> c.startsWith("admin_token="))
				.map(c -> c.split(";")[0].substring("admin_token=".length()))
				.findFirst()
				.orElse(null);
			if (tokenFromCookie != null) {
				return tokenFromCookie;
			}

			// Try to extract from response body
			JsonNode responseJson = objectMapper.readTree(response.body());
			for (String field : List.of("token", "admin_token", "access_token", "jwt")) {
				JsonNode node = responseJson.get(field);
				if (node != null && !node.isNull()) {
					return node.asText();
				}
			}

			LOGGER.error("Could not extract admin token from PicPeak login response: {}", response.body());
			return null;
		}
		catch (Exception ex) {
			LOGGER.error("PicPeak login error: {}", ex.getMessage());
			return null;
		}
	}

	private String createEvent(String token, GalleryCode code, String eventName, int number) {
		try {
			String galleryEventName = eventName.isBlank() ? code.code() : eventName + " #" + number;
			String eventDate = picPeakProperties.eventDate().isBlank() ? LocalDate.now().toString()
					: picPeakProperties.eventDate();

			ObjectNode body = objectMapper.createObjectNode();
			body.put("event_type", picPeakProperties.eventType());
			body.put("event_name", galleryEventName);
			body.put("event_date", eventDate);
			body.put("customer_name", galleryEventName);
			body.put("customer_email", picPeakProperties.customerEmail());
			body.put("admin_email", picPeakProperties.adminEmail().isBlank() ? picPeakProperties.customerEmail()
					: picPeakProperties.adminEmail());
			body.put("require_password", picPeakProperties.requirePassword());
			body.put("password", code.password());
			body.put("welcome_message", picPeakProperties.welcomeMessage());
			body.putNull("color_theme");
			body.put("header_style", picPeakProperties.headerStyle());
			body.put("hero_divider_style", picPeakProperties.heroDividerStyle());
			body.put("expiration_days", picPeakProperties.expirationDays());
			body.put("allow_user_uploads", picPeakProperties.allowUserUploads());
			body.putNull("upload_category_id");
			body.put("css_template_id", picPeakProperties.cssTemplateId());
			body.put("feedback_enabled", picPeakProperties.feedbackEnabled());
			body.put("allow_ratings", picPeakProperties.allowRatings());
			body.put("allow_likes", picPeakProperties.allowLikes());
			body.put("allow_comments", picPeakProperties.allowComments());
			body.put("allow_favorites", picPeakProperties.allowFavorites());
			body.put("require_name_email", picPeakProperties.requireNameEmail());
			body.put("moderate_comments", picPeakProperties.moderateComments());
			body.put("show_feedback_to_guests", picPeakProperties.showFeedbackToGuests());
			String requestBody = objectMapper.writeValueAsString(body);

			HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(picPeakProperties.apiUrl() + "/api/admin/events"))
				.header("Content-Type", "application/json")
				.header("Accept", "application/json")
				.header("Cookie", "admin_token=" + token)
				.POST(HttpRequest.BodyPublishers.ofString(requestBody))
				.build();

			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

			if (response.statusCode() != 200 && response.statusCode() != 201) {
				LOGGER.error("PicPeak event creation failed with status {}: {}", response.statusCode(),
						response.body());
				return null;
			}

			JsonNode responseJson = objectMapper.readTree(response.body());
			JsonNode shareLinkNode = responseJson.get("share_link");
			if (shareLinkNode != null && !shareLinkNode.isNull()) {
				return shareLinkNode.asText();
			}

			LOGGER.error("Could not find share_link in PicPeak response: {}", response.body());
			return null;
		}
		catch (Exception ex) {
			LOGGER.error("PicPeak event creation error: {}", ex.getMessage());
			return null;
		}
	}

}
