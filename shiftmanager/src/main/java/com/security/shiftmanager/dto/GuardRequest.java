package com.security.shiftmanager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record GuardRequest(
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @NotBlank @Pattern(regexp = "\\d{7,8}", message = "must have 7 or 8 digits") String dni,
        @NotBlank @Size(max = 30) String phone,
        Boolean active
) {
}
