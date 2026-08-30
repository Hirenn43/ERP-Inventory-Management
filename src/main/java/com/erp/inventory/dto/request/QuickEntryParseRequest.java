package com.erp.inventory.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QuickEntryParseRequest {

    @NotBlank(message = "Text is required")
    @Size(max = 1000, message = "Text is too long")
    private String text;
}
