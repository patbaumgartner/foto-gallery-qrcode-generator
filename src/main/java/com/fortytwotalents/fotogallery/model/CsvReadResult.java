package com.fortytwotalents.fotogallery.model;

import java.util.List;

public record CsvReadResult(List<GalleryCode> codes, String eventName) {
}
