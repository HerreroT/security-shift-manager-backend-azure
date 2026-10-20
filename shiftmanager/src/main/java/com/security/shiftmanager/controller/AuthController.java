package com.security.shiftmanager.controller;

import com.security.shiftmanager.dto.LoginRequest;
import com.security.shiftmanager.dto.LoginResponse;
import com.security.shiftmanager.dto.UserResponse;
import com.security.shiftmanager.security.JwtService;
import com.security.shiftmanager.security.SecurityUser;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthController(AuthenticationManager authenticationManager, JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        try {
            var authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
            SecurityUser securityUser = (SecurityUser) authentication.getPrincipal();
            var user = securityUser.getUser();
            String token = jwtService.generateToken(user.getEmail());
            log.info("Login succeeded for user id={} role={}", user.getId(), user.getRole());
            return new LoginResponse(token, user.getId(), user.getUsername(), user.getEmail(), user.getRole());
        } catch (InternalAuthenticationServiceException e) {
            // The user could not be loaded (e.g. the database is down): not a credentials problem.
            log.error("Login could not check the credentials", e);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Service temporarily unavailable");
        } catch (AuthenticationException e) {
            // Same answer for a wrong password, an unknown email or a deactivated user,
            // so the response does not reveal which accounts exist.
            log.warn("Login failed: {}", e.getClass().getSimpleName());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }
    }

    /** Returns the user that owns the token. Useful to check that a token is still valid. */
    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal SecurityUser securityUser) {
        return UserResponse.from(securityUser.getUser());
    }
}
