package com.tiktel.ttelgo.slider.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Slider images: optional explicit URLs, or built from {@link #publicBaseUrl} + {@link #assetUrlPrefix}
 * and files on disk under {@link #filesystemDirectory}.
 */
@Component
@ConfigurationProperties(prefix = "app.slider")
public class SliderProperties {

    /**
     * When non-empty, overrides filesystem-derived URLs (e.g. CDN or S3).
     */
    private List<String> imageUrls = new ArrayList<>();

    /**
     * Absolute path on the server where slider images live (e.g. /home/ubuntu/ttlegoapp_slider).
     */
    private String filesystemDirectory = "/home/ubuntu/ttlegoapp_slider";

    /**
     * URL prefix clients use to load images (must match {@link SliderResourceConfig} handler).
     */
    private String assetUrlPrefix = "/api/slider-assets";

    /**
     * Public origin of this API, used to build full image URLs for mobile apps (no trailing slash).
     * Set {@code APP_PUBLIC_BASE_URL} on the server, e.g. http://34.195.92.171:8080
     */
    private String publicBaseUrl = "http://localhost:8080";

    public List<String> getImageUrls() {
        return imageUrls;
    }

    public void setImageUrls(List<String> imageUrls) {
        this.imageUrls = imageUrls != null ? new ArrayList<>(imageUrls) : new ArrayList<>();
    }

    public String getFilesystemDirectory() {
        return filesystemDirectory;
    }

    public void setFilesystemDirectory(String filesystemDirectory) {
        this.filesystemDirectory = filesystemDirectory;
    }

    public String getAssetUrlPrefix() {
        return assetUrlPrefix;
    }

    public void setAssetUrlPrefix(String assetUrlPrefix) {
        this.assetUrlPrefix = assetUrlPrefix;
    }

    public String getPublicBaseUrl() {
        return publicBaseUrl;
    }

    public void setPublicBaseUrl(String publicBaseUrl) {
        this.publicBaseUrl = publicBaseUrl;
    }
}
