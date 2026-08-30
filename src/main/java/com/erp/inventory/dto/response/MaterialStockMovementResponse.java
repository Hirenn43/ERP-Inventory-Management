package com.erp.inventory.dto.response;

import com.erp.inventory.entity.enums.MovementDirection;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaterialStockMovementResponse {
    private Long id;
    private Long materialId;
    private String materialCode;
    private Long partyId;
    private String partyName;
    private MovementDirection direction;
    private LocalDate date;
    private BigDecimal quantity;
    private String invoiceNo;
    private String lotNo;
    private BigDecimal price;
    private Map<String, Object> attributes;
}
