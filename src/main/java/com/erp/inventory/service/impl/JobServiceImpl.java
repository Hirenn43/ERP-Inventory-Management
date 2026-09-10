package com.erp.inventory.service.impl;

import com.erp.inventory.dto.request.JobRequest;
import com.erp.inventory.dto.response.JobResponse;
import com.erp.inventory.entity.Company;
import com.erp.inventory.entity.Job;
import com.erp.inventory.exception.DuplicateResourceException;
import com.erp.inventory.exception.ResourceNotFoundException;
import com.erp.inventory.repository.CompanyRepository;
import com.erp.inventory.repository.JobRepository;
import com.erp.inventory.service.JobService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;

@Service
@RequiredArgsConstructor
public class JobServiceImpl implements JobService {

    private final JobRepository jobRepository;
    private final CompanyRepository companyRepository;

    @Override
    @Transactional
    public JobResponse create(JobRequest request) {
        if (jobRepository.existsByNameIgnoreCase(request.getName())) {
            throw new DuplicateResourceException("Job already exists with name: " + request.getName());
        }
        Job entity = Job.builder()
                .company(resolveCompany(request.getCompanyId()))
                .name(request.getName())
                .description(request.getDescription())
                .rate(request.getRate())
                .finishedGoodsType(request.getFinishedGoodsType())
                .defaultRepeatIssue(request.getDefaultRepeatIssue())
                .attributes(request.getAttributes() != null ? request.getAttributes() : new HashMap<>())
                .build();
        return toResponse(jobRepository.save(entity));
    }

    @Override
    @Transactional
    public JobResponse update(Long id, JobRequest request) {
        Job entity = findEntity(id);
        if (jobRepository.existsByNameIgnoreCaseAndIdNot(request.getName(), id)) {
            throw new DuplicateResourceException("Job already exists with name: " + request.getName());
        }
        entity.setCompany(resolveCompany(request.getCompanyId()));
        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
        entity.setRate(request.getRate());
        entity.setFinishedGoodsType(request.getFinishedGoodsType());
        entity.setDefaultRepeatIssue(request.getDefaultRepeatIssue());
        if (request.getAttributes() != null) {
            entity.setAttributes(request.getAttributes());
        }
        return toResponse(entity);
    }

    @Override
    public JobResponse getById(Long id) {
        return toResponse(findEntity(id));
    }

    @Override
    public List<JobResponse> getAll() {
        return jobRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public void delete(Long id) {
        jobRepository.delete(findEntity(id));
    }

    private Company resolveCompany(Long companyId) {
        if (companyId == null) {
            return null;
        }
        return companyRepository.findById(companyId)
                .orElseThrow(() -> ResourceNotFoundException.of("Company", companyId));
    }

    private Job findEntity(Long id) {
        return jobRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Job", id));
    }

    private JobResponse toResponse(Job entity) {
        return JobResponse.builder()
                .id(entity.getId())
                .companyId(entity.getCompany() != null ? entity.getCompany().getId() : null)
                .name(entity.getName())
                .description(entity.getDescription())
                .rate(entity.getRate())
                .finishedGoodsType(entity.getFinishedGoodsType())
                .defaultRepeatIssue(entity.getDefaultRepeatIssue())
                .attributes(entity.getAttributes())
                .build();
    }
}
