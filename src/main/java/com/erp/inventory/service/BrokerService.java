package com.erp.inventory.service;

import com.erp.inventory.dto.response.PageResponse;
import org.springframework.data.domain.Pageable;
import com.erp.inventory.dto.request.BrokerRequest;
import com.erp.inventory.dto.response.BrokerResponse;

import java.util.List;

public interface BrokerService {
    BrokerResponse create(BrokerRequest request);
    BrokerResponse update(Long id, BrokerRequest request);
    BrokerResponse getById(Long id);
    List<BrokerResponse> getAll();
    PageResponse<BrokerResponse> getPage(Pageable pageable, String q);
    void delete(Long id);
}
