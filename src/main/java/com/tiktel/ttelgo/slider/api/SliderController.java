package com.tiktel.ttelgo.slider.api;

import com.tiktel.ttelgo.slider.api.dto.SliderImagesResponse;
import com.tiktel.ttelgo.slider.application.SliderService;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api")
public class SliderController {

    private final SliderService sliderService;

    public SliderController(SliderService sliderService) {
        this.sliderService = sliderService;
    }

    @GetMapping("/slider-images")
    public ResponseEntity<SliderImagesResponse> getSliderImages() {
        SliderImagesResponse body = new SliderImagesResponse(sliderService.getSliderImageUrls());
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(5, TimeUnit.MINUTES).cachePublic())
                .body(body);
    }
}
