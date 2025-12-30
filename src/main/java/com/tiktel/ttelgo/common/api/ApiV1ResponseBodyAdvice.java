package com.tiktel.ttelgo.common.api;

import com.tiktel.ttelgo.common.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.util.Map;

/**
 * Ensures a consistent response structure for all /api/v1/** endpoints.
 * If a controller returns a raw object (non-ApiResponse), we wrap it into ApiResponse.success().
 */
@RestControllerAdvice
public class ApiV1ResponseBodyAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(
            Object body,
            MethodParameter returnType,
            MediaType selectedContentType,
            Class<? extends HttpMessageConverter<?>> selectedConverterType,
            ServerHttpRequest request,
            ServerHttpResponse response
    ) {
        if (!isV1Request(request)) {
            return body;
        }

        if (body == null) {
            return ApiResponse.success(null);
        }

        if (body instanceof ApiResponse<?>) {
            return body;
        }

        // Avoid breaking String responses (StringHttpMessageConverter expects raw string)
        if (body instanceof String) {
            return body;
        }

        // If someone returns a map with typical error keys from filters, keep it as-is
        if (body instanceof Map<?, ?>) {
            return ApiResponse.success(body);
        }

        return ApiResponse.success(body);
    }

    private boolean isV1Request(ServerHttpRequest request) {
        if (request instanceof ServletServerHttpRequest servlet) {
            HttpServletRequest r = servlet.getServletRequest();
            String uri = r.getRequestURI();
            return uri != null && uri.startsWith("/api/v1/");
        }
        return false;
    }
}


