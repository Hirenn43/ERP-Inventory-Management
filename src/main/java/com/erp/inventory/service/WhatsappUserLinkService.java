package com.erp.inventory.service;

import com.erp.inventory.dto.request.WhatsappUserLinkRequest;
import com.erp.inventory.dto.response.WhatsappUserLinkResponse;

import java.util.List;

public interface WhatsappUserLinkService {
    WhatsappUserLinkResponse create(WhatsappUserLinkRequest request);
    WhatsappUserLinkResponse update(Long id, WhatsappUserLinkRequest request);
    WhatsappUserLinkResponse getById(Long id);
    List<WhatsappUserLinkResponse> getAll();
    void delete(Long id);
}
