package com.security.shiftmanager.dto;

import com.security.shiftmanager.model.Guard;

public record GuardResponse(
        Long id,
        String firstName,
        String lastName,
        String dni,
        String phone,
        boolean active
) {

    public static GuardResponse from(Guard guard) {
        return new GuardResponse(
                guard.getId(),
                guard.getFirstName(),
                guard.getLastName(),
                guard.getDni(),
                guard.getPhone(),
                guard.isActive());
    }
}
