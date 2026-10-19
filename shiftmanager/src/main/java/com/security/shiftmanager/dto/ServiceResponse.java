package com.security.shiftmanager.dto;

import com.security.shiftmanager.model.Service;

public record ServiceResponse(
        Long id,
        String name,
        String location,
        String description,
        boolean active
) {

    public static ServiceResponse from(Service service) {
        return new ServiceResponse(
                service.getId(),
                service.getName(),
                service.getLocation(),
                service.getDescription(),
                service.isActive());
    }
}
