package com.tiktel.ttelgo.slider.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/**
 * Exposes slider files from the server disk at {@link SliderProperties#getFilesystemDirectory()}
 * under {@link SliderProperties#getAssetUrlPrefix()} (e.g. {@code /api/slider-assets/slider1.jpeg}).
 */
@Configuration
public class SliderResourceConfig implements WebMvcConfigurer {

    private final SliderProperties sliderProperties;

    public SliderResourceConfig(SliderProperties sliderProperties) {
        this.sliderProperties = sliderProperties;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String dir = sliderProperties.getFilesystemDirectory();
        if (dir == null || dir.isBlank()) {
            return;
        }
        Path absolute = Path.of(dir).toAbsolutePath().normalize();
        String location = "file:" + absolute + (absolute.toString().endsWith("/") ? "" : "/");

        String prefix = sliderProperties.getAssetUrlPrefix();
        if (prefix == null || prefix.isBlank()) {
            prefix = "/api/slider-assets";
        }
        if (!prefix.startsWith("/")) {
            prefix = "/" + prefix;
        }
        if (!prefix.endsWith("/")) {
            prefix = prefix + "/";
        }

        registry.addResourceHandler(prefix + "**")
                .addResourceLocations(location)
                .setCacheControl(CacheControl.maxAge(1, TimeUnit.DAYS).cachePublic());
    }
}
