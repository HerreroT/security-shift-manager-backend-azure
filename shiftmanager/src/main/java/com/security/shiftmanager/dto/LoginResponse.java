package com.security.shiftmanager.dto;

import com.security.shiftmanager.model.Role;

public class LoginResponse {

    private final String token;
    private final Long id;
    private final String username;
    private final String email;
    private final Role role;

    public LoginResponse(String token, Long id, String username, String email, Role role) {
        this.token = token;
        this.id = id;
        this.username = username;
        this.email = email;
        this.role = role;
    }

    public String getToken() {
        return token;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public Role getRole() {
        return role;
    }
}
