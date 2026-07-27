package com.portx.routesystem.service;

import com.portx.routesystem.dto.DispatcherRequest;
import com.portx.routesystem.dto.DispatcherResponse;
import com.portx.routesystem.entity.User;
import com.portx.routesystem.entity.UserRole;
import com.portx.routesystem.exception.ResourceNotFoundException;
import com.portx.routesystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * DispatcherService — Business logic layer for managing Dispatcher user accounts.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DispatcherService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Retrieves all users with role ROLE_DISPATCHER.
     */
    public List<DispatcherResponse> getAllDispatchers() {
        return userRepository.findAll().stream()
                .filter(u -> u.getRole() == UserRole.DISPATCHER)
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves dispatcher by primary key ID.
     */
    public DispatcherResponse getDispatcherById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dispatcher User", id));
        return mapToResponse(user);
    }

    /**
     * Creates a new Dispatcher user account.
     */
    @Transactional
    public DispatcherResponse createDispatcher(DispatcherRequest req) {
        String username = req.getUsername().trim();
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists: " + username);
        }

        String rawPassword = (req.getPassword() != null && !req.getPassword().trim().isEmpty()) ? req.getPassword().trim() : "admin123";

        User user = User.builder()
                .fullName(req.getFullName().trim())
                .username(username)
                .email(req.getEmail().trim())
                .password(passwordEncoder.encode(rawPassword))
                .role(UserRole.DISPATCHER)
                .build();

        return mapToResponse(userRepository.save(user));
    }

    /**
     * Updates an existing Dispatcher user account.
     */
    @Transactional
    public DispatcherResponse updateDispatcher(Long id, DispatcherRequest req) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dispatcher User", id));

        user.setFullName(req.getFullName().trim());
        user.setEmail(req.getEmail().trim());
        user.setUsername(req.getUsername().trim());

        if (req.getPassword() != null && !req.getPassword().trim().isEmpty()) {
            user.setPassword(passwordEncoder.encode(req.getPassword().trim()));
        }

        return mapToResponse(userRepository.save(user));
    }

    /**
     * Deletes a Dispatcher account.
     */
    @Transactional
    public void deleteDispatcher(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dispatcher User", id));
        userRepository.delete(user);
    }

    private DispatcherResponse mapToResponse(User u) {
        return DispatcherResponse.builder()
                .userId(u.getUserId())
                .fullName(u.getFullName())
                .username(u.getUsername())
                .email(u.getEmail())
                .role(u.getRole())
                .createdAt(u.getCreatedAt())
                .build();
    }
}
