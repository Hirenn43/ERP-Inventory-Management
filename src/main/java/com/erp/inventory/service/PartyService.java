package com.erp.inventory.service;

import com.erp.inventory.dto.response.PageResponse;
import org.springframework.data.domain.Pageable;
import com.erp.inventory.dto.request.PartyRequest;
import com.erp.inventory.dto.response.PartyResponse;

import java.util.List;

public interface PartyService {
    PartyResponse create(PartyRequest request);
    PartyResponse update(Long id, PartyRequest request);
    PartyResponse getById(Long id);
    List<PartyResponse> getAll();
    PageResponse<PartyResponse> getPage(Pageable pageable, String q);
    void delete(Long id);
}
