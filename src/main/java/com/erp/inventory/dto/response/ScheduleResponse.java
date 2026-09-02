package com.erp.inventory.dto.response;

import com.erp.inventory.entity.enums.ScheduleEffect;
import com.erp.inventory.entity.enums.ScheduleType;
import java.util.Map;
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
public class ScheduleResponse {
    private Long id;
    private Long companyId;
    private String name;
    private Long parentId;
    private Boolean subGroupRequired;
    private ScheduleEffect effectOn;
    private Integer scheduleIndex;
    private Boolean effectOnGp;
    private String description;
    private ScheduleType scheduleType;
    private Map<String, Object> attributes;
}
