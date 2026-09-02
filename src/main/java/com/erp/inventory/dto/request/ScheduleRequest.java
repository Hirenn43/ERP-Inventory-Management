package com.erp.inventory.dto.request;

import com.erp.inventory.entity.enums.ScheduleEffect;
import com.erp.inventory.entity.enums.ScheduleType;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ScheduleRequest {

    private Long companyId;

    @NotBlank(message = "Schedule name is required")
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
