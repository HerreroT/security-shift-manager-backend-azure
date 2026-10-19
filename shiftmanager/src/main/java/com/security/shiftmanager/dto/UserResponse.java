package com.security.shiftmanager.dto;

import com.security.shiftmanager.model.Role;
import com.security.shiftmanager.model.User;

public record UserResponse(
        Long id,
        String username,
        String email,
        Role role,
        boolean active
) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getEmail(), user.getRole(), user.isActive());
    }
}
