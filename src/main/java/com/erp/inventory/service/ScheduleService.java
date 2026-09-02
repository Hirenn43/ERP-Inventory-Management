package com.erp.inventory.service;

import com.erp.inventory.dto.request.ScheduleRequest;
import com.erp.inventory.dto.response.ScheduleResponse;

import java.util.List;

public interface ScheduleService {
    ScheduleResponse create(ScheduleRequest request);
    ScheduleResponse update(Long id, ScheduleRequest request);
    ScheduleResponse getById(Long id);
    List<ScheduleResponse> getAll();
    void delete(Long id);
}
