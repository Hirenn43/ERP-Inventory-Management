package com.erp.inventory.dto.response;

import com.erp.inventory.entity.enums.MovementType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockMovementResponse {
    private Long id;
    private String productName;
    private String warehouseName;
    private MovementType type;
    private Integer quantity;
    private String remarks;
    private String performedBy;
    private LocalDateTime createdAt;
}
