package com.fortytwotalents.fotogallery.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.picpeak")
public record PicPeakProperties(boolean enabled, String apiUrl, String username, String password, String eventType,
		String eventDate, String customerEmail, String adminEmail, boolean requirePassword, String welcomeMessage,
		int expirationDays, boolean allowUserUploads, boolean feedbackEnabled, boolean allowRatings, boolean allowLikes,
		boolean allowComments, boolean allowFavorites, boolean requireNameEmail, boolean moderateComments,
		boolean showFeedbackToGuests, String headerStyle, String heroDividerStyle, int cssTemplateId) {

	public PicPeakProperties {
		if (apiUrl == null) {
			apiUrl = "";
		}
		if (username == null) {
			username = "";
		}
		if (password == null) {
			password = "";
		}
		if (eventType == null) {
			eventType = "schule";
		}
		if (eventDate == null) {
			eventDate = "";
		}
		if (customerEmail == null) {
			customerEmail = "";
		}
		if (adminEmail == null) {
			adminEmail = "";
		}
		if (welcomeMessage == null) {
			welcomeMessage = "";
		}
		if (headerStyle == null) {
			headerStyle = "minimal";
		}
		if (heroDividerStyle == null) {
			heroDividerStyle = "wave";
		}
		if (expirationDays <= 0) {
			expirationDays = 30;
		}
		if (cssTemplateId <= 0) {
			cssTemplateId = 2;
		}
	}

}
