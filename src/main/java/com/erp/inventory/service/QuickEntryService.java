package com.erp.inventory.service;

import com.erp.inventory.dto.request.QuickEntryParseRequest;
import com.erp.inventory.dto.response.QuickEntryResult;

public interface QuickEntryService {
    /** Interpret one quick-note line into a draft entry. Never writes. */
    QuickEntryResult parse(QuickEntryParseRequest request);
}
