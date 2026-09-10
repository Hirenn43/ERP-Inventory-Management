package com.erp.inventory.dto.request;

import com.erp.inventory.entity.enums.ScheduleEffect;
import com.erp.inventory.entity.enums.ScheduleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ScheduleRequest {

    private Long companyId;

    @NotBlank(message = "Schedule name is required")
    @Size(max = 120, message = "Schedule name must be at most 120 characters")
    private String name;

    private Long parentId;

    private Boolean subGroupRequired;

    private ScheduleEffect effectOn;

    private Integer scheduleIndex;

    private Boolean effectOnGp;

    @Size(max = 255, message = "Description must be at most 255 characters")
    private String description;

    private ScheduleType scheduleType;

    private Map<String, Object> attributes;
}
