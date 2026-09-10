package com.erp.inventory.service;

import com.erp.inventory.dto.response.PageResponse;
import org.springframework.data.domain.Pageable;
import com.erp.inventory.dto.request.JobRequest;
import com.erp.inventory.dto.response.JobResponse;

import java.util.List;

public interface JobService {
    JobResponse create(JobRequest request);
    JobResponse update(Long id, JobRequest request);
    JobResponse getById(Long id);
    List<JobResponse> getAll();
    PageResponse<JobResponse> getPage(Pageable pageable, String q);
    void delete(Long id);
}
