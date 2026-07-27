package com.portx.routesystem.service;

import com.portx.routesystem.dto.RegisterRequest;
import com.portx.routesystem.entity.User;
import com.portx.routesystem.exception.ResourceNotFoundException;
import com.portx.routesystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * UserService — Business logic for Admin User Management (Admin, Dispatcher, Driver).
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
                .username(req.getUsername().trim())
                .password(passwordEncoder.encode(req.getPassword()))
                .email(req.getEmail())
                .fullName(req.getFullName())
                .role(req.getRole())
                .build();

        return userRepository.save(user);
    }

    @Transactional
    public User updateUser(Long id, RegisterRequest req) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));

        user.setFullName(req.getFullName());
        user.setEmail(req.getEmail());
        if (req.getRole() != null) {
            user.setRole(req.getRole());
        }
        if (req.getPassword() != null && !req.getPassword().trim().isEmpty()) {
            user.setPassword(passwordEncoder.encode(req.getPassword()));
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
