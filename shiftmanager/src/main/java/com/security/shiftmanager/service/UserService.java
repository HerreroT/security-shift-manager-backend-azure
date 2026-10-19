package com.security.shiftmanager.service;

import com.security.shiftmanager.dto.UserRequest;
import com.security.shiftmanager.dto.UserResponse;
import com.security.shiftmanager.model.User;
import com.security.shiftmanager.repository.UserRepository;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> findAll() {
        return userRepository.findAll(Sort.by("username")).stream()
                .map(UserResponse::from)
                .toList();
    }

    public UserResponse create(UserRequest request) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.findByEmail(email).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User already exists with email " + email);
        }
        if (userRepository.findByUsername(request.username().trim()).isPresent()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "User already exists with username " + request.username());
        }
        User user = new User(
                null,
                request.username().trim(),
                email,
                passwordEncoder.encode(request.password()),
                true,
                request.role());
        return UserResponse.from(userRepository.save(user));
    }

    public UserResponse setActive(Long id, boolean active) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found with id " + id));
        user.setActive(active);
        return UserResponse.from(user);
    }
}
