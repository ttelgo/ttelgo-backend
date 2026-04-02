package com.tiktel.ttelgo.slider.application;

import com.tiktel.ttelgo.slider.config.SliderProperties;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SliderService {

    /** Filenames under {@code app.slider.filesystem-directory} (matches your server folder). */
    private static final List<String> SLIDER_FILENAMES = List.of(
            "slider1.jpeg",
            "slider2.jpeg",
            "slider3.jpeg",
            "slider4.jpeg"
    );

    private final SliderProperties sliderProperties;

    public SliderService(SliderProperties sliderProperties) {
        this.sliderProperties = sliderProperties;
    }

    /**
     * Returns immutable list of slider image URLs for the mobile app.
     * Uses {@code app.slider.image-urls} when set; otherwise builds URLs from {@code publicBaseUrl}
     * and {@code assetUrlPrefix} pointing at files served from {@code filesystemDirectory}.
     */
    public List<String> getSliderImageUrls() {
        List<String> configured = sliderProperties.getImageUrls();
        if (configured != null && !configured.isEmpty()) {
            return List.copyOf(configured);
        }
        return SLIDER_FILENAMES.stream()
                .map(this::buildFilesystemAssetUrl)
                .toList();
    }

    private String buildFilesystemAssetUrl(String filename) {
        String base = trimTrailingSlashes(sliderProperties.getPublicBaseUrl());
        String prefix = trimSlashes(sliderProperties.getAssetUrlPrefix());
        return base + "/" + prefix + "/" + filename;
    }

    private static String trimTrailingSlashes(String s) {
        if (s == null || s.isEmpty()) {
            return "";
        }
        int end = s.length();
        while (end > 0 && s.charAt(end - 1) == '/') {
            end--;
        }
        return s.substring(0, end);
    }

    private static String trimSlashes(String s) {
        if (s == null || s.isEmpty()) {
            return "";
        }
        int start = 0;
        while (start < s.length() && s.charAt(start) == '/') {
            start++;
        }
        int end = s.length();
        while (end > start && s.charAt(end - 1) == '/') {
            end--;
        }
        return s.substring(start, end);
    }
}
