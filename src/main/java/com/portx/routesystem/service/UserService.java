package com.portx.routesystem.service;

import com.portx.routesystem.dto.RegisterRequest;
import com.portx.routesystem.entity.User;
import com.portx.routesystem.entity.UserRole;
import com.portx.routesystem.exception.ResourceNotFoundException;
import com.portx.routesystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * UserService - Business logic for Admin User Management (Admin, Dispatcher, Driver).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    @Transactional
    public User createUser(RegisterRequest req) {
        if (userRepository.existsByUsername(req.getUsername())) {
            throw new IllegalArgumentException("Username already exists: " + req.getUsername());
        }

        User user = User.builder()
                .fullName(req.getFullName().trim())
                .username(req.getUsername().trim())
                .email(req.getEmail().trim())
                .password(passwordEncoder.encode(req.getPassword().trim()))
                .role(req.getRole() != null ? req.getRole() : UserRole.DISPATCHER)
                .build();

        return userRepository.save(user);
    }

    @Transactional
    public User updateUser(Long id, RegisterRequest req) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));

        user.setFullName(req.getFullName().trim());
        user.setEmail(req.getEmail().trim());
        user.setUsername(req.getUsername().trim());

        if (req.getRole() != null) {
            user.setRole(req.getRole());
        }

        if (req.getPassword() != null && !req.getPassword().trim().isEmpty()) {
            user.setPassword(passwordEncoder.encode(req.getPassword().trim()));
        }

        return userRepository.save(user);
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
        userRepository.delete(user);
    }
}
