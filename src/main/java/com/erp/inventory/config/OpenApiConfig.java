package com.erp.inventory.config;

import com.erp.inventory.exception.ApiError;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashMap;
import java.util.Map;

@Configuration
@OpenAPIDefinition(
        info = @Info(title = "Inventory Management API", version = "1.0", description = "ERP Inventory Management System REST API")
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
@SecurityRequirement(name = "bearerAuth")
public class OpenApiConfig {

    /** Documents the shared {@link ApiError} error shape on every operation (400/401/403/404/409). */
    @Bean
    public OpenApiCustomizer standardErrorResponses() {
        Map<String, String> errors = new LinkedHashMap<>();
        errors.put("400", "Validation failed or malformed request");
        errors.put("401", "Missing or invalid authentication");
        errors.put("403", "Authenticated but not permitted");
        errors.put("404", "Resource not found");
        errors.put("409", "Conflict - duplicate, stale version, or still referenced");

        return openApi -> {
            ModelConverters.getInstance().read(ApiError.class)
                    .forEach((name, schema) -> openApi.getComponents().addSchemas(name, schema));

            Content errorContent = new Content().addMediaType("application/json",
                    new MediaType().schema(new Schema<>().$ref("#/components/schemas/ApiError")));

            openApi.getPaths().values().forEach(pathItem -> pathItem.readOperations().forEach(operation -> {
                errors.forEach((code, description) -> {
                    if (operation.getResponses().get(code) == null) {
                        operation.getResponses().addApiResponse(code,
                                new ApiResponse().description(description).content(errorContent));
                    }
                });
            }));
        };
    }
}
