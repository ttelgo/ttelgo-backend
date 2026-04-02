package com.tiktel.ttelgo.slider.api.dto;

import java.util.List;

/**
 * JSON shape: { "images": [ "https://...", ... ] }
 */
public record SliderImagesResponse(List<String> images) {
}
