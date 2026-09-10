package com.erp.inventory.dto.response;

import com.erp.inventory.entity.enums.FieldDataType;
import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomFieldDefinitionResponse {
    private Long id;
    private String resource;
    private String fieldKey;
    private String label;
    private FieldDataType dataType;
    private Integer maxLength;
    private BigDecimal minValue;
    private BigDecimal maxValue;
    private boolean required;
    private boolean uniqueValue;
    private String section;
    private int displayOrder;
    private List<String> options;
    private boolean active;
}
