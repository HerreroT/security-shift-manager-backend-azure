package com.security.shiftmanager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ServiceRequest(
        @NotBlank @Size(max = 150) String name,
        @NotBlank @Size(max = 255) String location,
        @Size(max = 500) String description,
        Boolean active
) {
}
