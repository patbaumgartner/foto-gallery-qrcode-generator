package com.fortytwotalents.fotogallery.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.picpeak")
public record PicPeakProperties(boolean enabled, String apiUrl, String username, String password, String eventType,
		String eventDate, String customerEmail, String adminEmail, boolean requirePassword, String welcomeMessage,
		int expirationDays, boolean allowUserUploads, boolean feedbackEnabled, boolean allowRatings, boolean allowLikes,
		boolean allowComments, boolean allowFavorites, boolean allowDownloads, boolean disableRightClick,
		boolean enableDevtoolsProtection, boolean useCanvasRendering, boolean watermarkDownloads,
		boolean heroLogoVisible, boolean requireNameEmail, boolean moderateComments,
		boolean showFeedbackToGuests, String headerStyle, String heroDividerStyle, int cssTemplateId,
		String colorTheme, String protectionLevel, String sourceMode, String heroImageAnchor,
		String heroLogoSize, String heroLogoPosition, Integer uploadCategoryId, String externalPath,
		Integer heroPhotoId) {

	public PicPeakProperties {
		if (apiUrl == null) {
			apiUrl = "";
		}
		if (apiUrl.endsWith("/")) {
			apiUrl = apiUrl.substring(0, apiUrl.length() - 1);
		}
		if (username == null) {
			username = "";
		}
		if (password == null) {
			password = "";
		}
		if (eventType == null) {
			eventType = "schulfotos";
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
			headerStyle = "standard";
		}
		if (heroDividerStyle == null) {
			heroDividerStyle = "wave";
		}
		if (expirationDays <= 0) {
			expirationDays = 30;
		}
		if (cssTemplateId <= 0) {
			cssTemplateId = 1;
		}
		if (colorTheme == null) {
			colorTheme = "default";
		}
		if (protectionLevel == null) {
			protectionLevel = "standard";
		}
		if (sourceMode == null) {
			sourceMode = "managed";
		}
		if (heroImageAnchor == null) {
			heroImageAnchor = "center";
		}
		if (heroLogoSize == null) {
			heroLogoSize = "medium";
		}
		if (heroLogoPosition == null) {
			heroLogoPosition = "top";
		}
	}

}
