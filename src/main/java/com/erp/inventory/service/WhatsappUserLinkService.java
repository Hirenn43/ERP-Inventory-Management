package com.erp.inventory.service;

import com.erp.inventory.dto.response.PageResponse;
import org.springframework.data.domain.Pageable;
import com.erp.inventory.dto.request.WhatsappUserLinkRequest;
import com.erp.inventory.dto.response.WhatsappUserLinkResponse;

import java.util.List;

public interface WhatsappUserLinkService {
    WhatsappUserLinkResponse create(WhatsappUserLinkRequest request);
    WhatsappUserLinkResponse update(Long id, WhatsappUserLinkRequest request);
    WhatsappUserLinkResponse getById(Long id);
    List<WhatsappUserLinkResponse> getAll();
    PageResponse<WhatsappUserLinkResponse> getPage(Pageable pageable, String q);
    void delete(Long id);
}
