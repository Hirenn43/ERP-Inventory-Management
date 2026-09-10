package com.erp.inventory.service;

import com.erp.inventory.dto.request.CustomFieldDefinitionRequest;
import com.erp.inventory.dto.response.CustomFieldDefinitionResponse;
import com.erp.inventory.dto.response.PageResponse;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface CustomFieldDefinitionService {

    CustomFieldDefinitionResponse create(CustomFieldDefinitionRequest request);

    CustomFieldDefinitionResponse update(Long id, CustomFieldDefinitionRequest request);

    CustomFieldDefinitionResponse getById(Long id);

    List<CustomFieldDefinitionResponse> getAll();

    PageResponse<CustomFieldDefinitionResponse> getPage(Pageable pageable, String q);

    void delete(Long id);

    /** Definitions for one resource, ordered. {@code includeInactive=false} is what the
     *  Details forms consume; {@code true} is for the Master Management screen. */
    List<CustomFieldDefinitionResponse> getByResource(String resource, boolean includeInactive);

    /** Bulk-create the given definitions, skipping keys that already exist for the resource. */
    List<CustomFieldDefinitionResponse> seedDefaults(String resource, List<CustomFieldDefinitionRequest> defs);
}
