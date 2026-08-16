package com.erp.inventory.dto.response;

import com.erp.inventory.entity.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesOrderResponse {
    private Long id;
    private String orderNumber;
    private String customerName;
    private String warehouseName;
    private OrderStatus status;
    private BigDecimal totalAmount;
    private List<SalesOrderItemResponse> items;
    private String createdBy;
    private LocalDateTime createdAt;
}
