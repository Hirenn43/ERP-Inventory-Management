package com.erp.inventory.service.impl;

import com.erp.inventory.dto.request.BrokerRequest;
import com.erp.inventory.dto.response.BrokerResponse;
import com.erp.inventory.entity.Company;
import com.erp.inventory.entity.Broker;
import com.erp.inventory.exception.DuplicateResourceException;
import com.erp.inventory.exception.ResourceNotFoundException;
import com.erp.inventory.repository.CompanyRepository;
import com.erp.inventory.repository.BrokerRepository;
import com.erp.inventory.service.BrokerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BrokerServiceImpl implements BrokerService {

    private final BrokerRepository brokerRepository;
    private final CompanyRepository companyRepository;

    @Override
    @Transactional
    public BrokerResponse create(BrokerRequest request) {
        if (brokerRepository.existsByNameIgnoreCase(request.getName())) {
            throw new DuplicateResourceException("Broker already exists with name: " + request.getName());
        }
        Broker entity = Broker.builder()
                .company(resolveCompany(request.getCompanyId()))
                .name(request.getName())
                .address1(request.getAddress1())
                .address2(request.getAddress2())
                .address3(request.getAddress3())
                .address4(request.getAddress4())
                .phone1(request.getPhone1())
                .contactPerson1(request.getContactPerson1())
                .phone2(request.getPhone2())
                .contactPerson2(request.getContactPerson2())
                .email(request.getEmail())
                .brokeragePct(request.getBrokeragePct())
                .blacklisted(request.getBlacklisted())
                .attributes(request.getAttributes() != null ? request.getAttributes() : new HashMap<>())
                .build();
        return toResponse(brokerRepository.save(entity));
    }

    @Override
    @Transactional
    public BrokerResponse update(Long id, BrokerRequest request) {
        Broker entity = findEntity(id);
        if (brokerRepository.existsByNameIgnoreCaseAndIdNot(request.getName(), id)) {
            throw new DuplicateResourceException("Broker already exists with name: " + request.getName());
        }
        entity.setCompany(resolveCompany(request.getCompanyId()));
        entity.setName(request.getName());
        entity.setAddress1(request.getAddress1());
        entity.setAddress2(request.getAddress2());
        entity.setAddress3(request.getAddress3());
        entity.setAddress4(request.getAddress4());
        entity.setPhone1(request.getPhone1());
        entity.setContactPerson1(request.getContactPerson1());
        entity.setPhone2(request.getPhone2());
        entity.setContactPerson2(request.getContactPerson2());
        entity.setEmail(request.getEmail());
        entity.setBrokeragePct(request.getBrokeragePct());
        entity.setBlacklisted(request.getBlacklisted());
        if (request.getAttributes() != null) {
            entity.setAttributes(request.getAttributes());
        }
        return toResponse(entity);
    }

    @Override
    public BrokerResponse getById(Long id) {
        return toResponse(findEntity(id));
    }

    @Override
    public List<BrokerResponse> getAll() {
        return brokerRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public void delete(Long id) {
        brokerRepository.delete(findEntity(id));
    }

    private Company resolveCompany(Long companyId) {
        if (companyId == null) {
            return null;
        }
        return companyRepository.findById(companyId)
                .orElseThrow(() -> ResourceNotFoundException.of("Company", companyId));
    }

    private Broker findEntity(Long id) {
        return brokerRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Broker", id));
    }

    private BrokerResponse toResponse(Broker entity) {
        return BrokerResponse.builder()
                .id(entity.getId())
                .companyId(entity.getCompany() != null ? entity.getCompany().getId() : null)
                .name(entity.getName())
                .address1(entity.getAddress1())
                .address2(entity.getAddress2())
                .address3(entity.getAddress3())
                .address4(entity.getAddress4())
                .phone1(entity.getPhone1())
                .contactPerson1(entity.getContactPerson1())
                .phone2(entity.getPhone2())
                .contactPerson2(entity.getContactPerson2())
                .email(entity.getEmail())
                .brokeragePct(entity.getBrokeragePct())
                .blacklisted(entity.getBlacklisted())
                .attributes(entity.getAttributes())
                .build();
    }
}
