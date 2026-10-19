package com.security.shiftmanager.dto;

public class LoginResponse {

    private final String token;
    private final Long id;
    private final String username;
    private final String email;

    public LoginResponse(String token, Long id, String username, String email) {
        this.token = token;
        this.id = id;
        this.username = username;
        this.email = email;
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
}
