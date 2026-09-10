package com.erp.inventory.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * Adds a {@code Location} header to every {@code 201 Created} JSON response whose body exposes a
 * {@code Long getId()}, so callers get {@code Location: <request path>/<new id>} without each
 * controller having to build it. No-op for non-201 responses or bodies without an id.
 */
@RestControllerAdvice
public class CreatedLocationAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        if (body == null || !(response instanceof ServletServerHttpResponse servletResponse)) {
            return body;
        }
        HttpServletResponse raw = servletResponse.getServletResponse();
        if (raw.getStatus() != HttpStatus.CREATED.value() || raw.getHeader(HttpHeaders.LOCATION) != null) {
            return body;
        }
        Long id = extractId(body);
        if (id != null) {
            String path = request.getURI().getRawPath();
            raw.setHeader(HttpHeaders.LOCATION, path + (path.endsWith("/") ? "" : "/") + id);
        }
        return body;
    }

    private Long extractId(Object body) {
        try {
            Object value = body.getClass().getMethod("getId").invoke(body);
            return (value instanceof Long id) ? id : null;
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }
}
