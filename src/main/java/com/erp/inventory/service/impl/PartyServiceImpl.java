package com.erp.inventory.service.impl;

import com.erp.inventory.dto.response.PageResponse;
import com.erp.inventory.repository.spec.TextSearchSpec;
import org.springframework.data.domain.Pageable;
import com.erp.inventory.dto.request.PartyRequest;
import com.erp.inventory.dto.response.PartyResponse;
import com.erp.inventory.entity.Area;
import com.erp.inventory.entity.Broker;
import com.erp.inventory.entity.Company;
import com.erp.inventory.entity.Party;
import com.erp.inventory.entity.Schedule;
import com.erp.inventory.exception.DuplicateResourceException;
import com.erp.inventory.exception.ResourceNotFoundException;
import com.erp.inventory.repository.AreaRepository;
import com.erp.inventory.repository.BrokerRepository;
import com.erp.inventory.repository.CompanyRepository;
import com.erp.inventory.repository.PartyRepository;
import com.erp.inventory.repository.ScheduleRepository;
import com.erp.inventory.security.TenantContext;
import com.erp.inventory.service.attributes.AttributeValidator;
import com.erp.inventory.service.PartyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PartyServiceImpl implements PartyService {

    private final PartyRepository partyRepository;
    private final AttributeValidator attributeValidator;
    private final CompanyRepository companyRepository;
    private final ScheduleRepository scheduleRepository;
    private final BrokerRepository brokerRepository;
    private final AreaRepository areaRepository;

    @Override
    @Transactional
    public PartyResponse create(PartyRequest request) {
        if (partyRepository.existsByNameIgnoreCase(request.getName())) {
            throw new DuplicateResourceException("Party already exists with name: " + request.getName());
        }
        Party party = Party.builder()
                .company(resolveCompany(request.getCompanyId()))
                .name(request.getName())
                .address(request.getAddress())
                .mobile(request.getMobile())
                .email(request.getEmail())
                .roles(request.getRoles())
                .gstin(request.getGstin())
                .pan(request.getPan())
                .stateCode(request.getStateCode())
                .registrationType(request.getRegistrationType())
                .city(request.getCity())
                .state(request.getState())
                .pinCode(request.getPinCode())
                .contactPersonName(request.getContactPersonName())
                .openingBalance(request.getOpeningBalance())
                .creditLimitAmount(request.getCreditLimitAmount())
                .creditLimitDays(request.getCreditLimitDays())
                .blacklisted(request.getBlacklisted())
                .closed(request.getClosed())
                .schedule(resolveSchedule(request.getScheduleId()))
                .broker(resolveBroker(request.getBrokerId()))
                .area(resolveArea(request.getAreaId()))
                .attributes(attributeValidator.validate("parties", request.getAttributes(), null))
                .build();
        return toResponse(partyRepository.save(party));
    }

    @Override
    @Transactional
    public PartyResponse update(Long id, PartyRequest request) {
        Party party = findEntity(id);
        if (partyRepository.existsByNameIgnoreCaseAndIdNot(request.getName(), id)) {
            throw new DuplicateResourceException("Party already exists with name: " + request.getName());
        }
        party.setCompany(resolveCompany(request.getCompanyId()));
        party.setName(request.getName());
        party.setAddress(request.getAddress());
        party.setMobile(request.getMobile());
        party.setEmail(request.getEmail());
        party.setRoles(request.getRoles());
        party.setGstin(request.getGstin());
        party.setPan(request.getPan());
        party.setStateCode(request.getStateCode());
        party.setRegistrationType(request.getRegistrationType());
        party.setCity(request.getCity());
        party.setState(request.getState());
        party.setPinCode(request.getPinCode());
        party.setContactPersonName(request.getContactPersonName());
        party.setOpeningBalance(request.getOpeningBalance());
        party.setCreditLimitAmount(request.getCreditLimitAmount());
        party.setCreditLimitDays(request.getCreditLimitDays());
        party.setBlacklisted(request.getBlacklisted());
        party.setClosed(request.getClosed());
        party.setSchedule(resolveSchedule(request.getScheduleId()));
        party.setBroker(resolveBroker(request.getBrokerId()));
        party.setArea(resolveArea(request.getAreaId()));
        if (request.getAttributes() != null) {
            party.setAttributes(attributeValidator.validate("parties", request.getAttributes(), id));
        }
        return toResponse(party);
    }

    @Override
    public PartyResponse getById(Long id) {
        return toResponse(findEntity(id));
    }

    @Override
    public List<PartyResponse> getAll() {
        return partyRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    public PageResponse<PartyResponse> getPage(Pageable pageable, String q) {
        return PageResponse.of(partyRepository.findAll(TextSearchSpec.contains(q, "name"), pageable).map(this::toResponse));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        partyRepository.delete(findEntity(id));
    }

    private Company resolveCompany(Long ignoredFromRequest) {
        // Tenant is derived from the authenticated caller, never from the request body.
        Long tenantId = TenantContext.get();
        return tenantId == null ? null : companyRepository.getReferenceById(tenantId);
    }

    private Schedule resolveSchedule(Long scheduleId) {
        if (scheduleId == null) {
            return null;
        }
        return scheduleRepository.findOneById(scheduleId)
                .orElseThrow(() -> ResourceNotFoundException.of("Schedule", scheduleId));
    }

    private Broker resolveBroker(Long brokerId) {
        if (brokerId == null) {
            return null;
        }
        return brokerRepository.findOneById(brokerId)
                .orElseThrow(() -> ResourceNotFoundException.of("Broker", brokerId));
    }

    private Area resolveArea(Long areaId) {
        if (areaId == null) {
            return null;
        }
        return areaRepository.findOneById(areaId)
                .orElseThrow(() -> ResourceNotFoundException.of("Area", areaId));
    }

    private Party findEntity(Long id) {
        return partyRepository.findOneById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Party", id));
    }

    private PartyResponse toResponse(Party party) {
        return PartyResponse.builder()
                .id(party.getId())
                .companyId(party.getCompany() != null ? party.getCompany().getId() : null)
                .name(party.getName())
                .address(party.getAddress())
                .mobile(party.getMobile())
                .email(party.getEmail())
                .roles(party.getRoles())
                .gstin(party.getGstin())
                .pan(party.getPan())
                .stateCode(party.getStateCode())
                .registrationType(party.getRegistrationType())
                .city(party.getCity())
                .state(party.getState())
                .pinCode(party.getPinCode())
                .contactPersonName(party.getContactPersonName())
                .openingBalance(party.getOpeningBalance())
                .creditLimitAmount(party.getCreditLimitAmount())
                .creditLimitDays(party.getCreditLimitDays())
                .blacklisted(party.getBlacklisted())
                .closed(party.getClosed())
                .scheduleId(party.getSchedule() != null ? party.getSchedule().getId() : null)
                .brokerId(party.getBroker() != null ? party.getBroker().getId() : null)
                .areaId(party.getArea() != null ? party.getArea().getId() : null)
                .attributes(party.getAttributes())
                .build();
    }
}
