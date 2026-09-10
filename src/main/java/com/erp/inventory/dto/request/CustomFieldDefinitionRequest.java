package com.erp.inventory.dto.request;

import com.erp.inventory.entity.enums.FieldDataType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CustomFieldDefinitionRequest {

    @NotBlank(message = "Resource is required")
    @Size(max = 60)
    private String resource;

    @NotBlank(message = "Field key is required")
    @Pattern(regexp = "[a-zA-Z][a-zA-Z0-9]*", message = "Field key must start with a letter and contain only letters and digits")
    @Size(max = 60)
    private String fieldKey;

    @NotBlank(message = "Label is required")
    @Size(max = 120)
    private String label;

    @NotNull(message = "Data type is required")
    private FieldDataType dataType;

    @Positive(message = "Max length must be positive")
    private Integer maxLength;

    private BigDecimal minValue;
    private BigDecimal maxValue;

    private boolean required;
    private boolean uniqueValue;

    @Size(max = 60)
    private String section;

    private Integer displayOrder;

    private List<@NotBlank String> options;

    private Boolean active;
}
