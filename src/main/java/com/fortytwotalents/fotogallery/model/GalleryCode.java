package com.fortytwotalents.fotogallery.model;

import java.util.regex.Pattern;

public record GalleryCode(String code, String password, String shareUrl) {

	private static final Pattern CODE_PATTERN = Pattern.compile("^[A-Z0-9]{4}-[A-Z0-9]{4}-[A-Z0-9]{4}$");

	public GalleryCode(String code, String password) {
		this(code, password, "");
	}

	public GalleryCode(String code) {
		this(code, "", "");
	}

	public GalleryCode {
		if (password == null) {
			password = "";
		}
		if (shareUrl == null) {
			shareUrl = "";
		}
	}

	public static boolean isValid(String code) {
		return code != null && CODE_PATTERN.matcher(code).matches();
	}

	public String toUrl(String galleryUrl) {
		if (!shareUrl.isBlank()) {
			return shareUrl;
		}
		return galleryUrl + code;
	}

}
