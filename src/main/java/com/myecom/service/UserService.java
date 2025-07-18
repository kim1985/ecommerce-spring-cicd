package com.myecom.service;

import com.myecom.dto.auth.RegisterRequest;
import com.myecom.dto.auth.UserResponse;
import com.myecom.model.User;
import com.myecom.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // Registra nuovo utente
    public UserResponse registerUser(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email già registrata");
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phone(request.getPhone())
                .address(request.getAddress())
                .city(request.getCity())
                .zipCode(request.getZipCode())
                .role(User.Role.USER)
                .enabled(true)
                .build();

        User savedUser = userRepository.save(user);
        return convertToResponse(savedUser);
    }

    // Trova utente per email
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    // Trova utente per ID
    public Optional<UserResponse> findById(Long id) {
        return userRepository.findById(id)
                .map(this::convertToResponse);
    }

    // Converte User a UserResponse
    private UserResponse convertToResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phone(user.getPhone())
                .address(user.getAddress())
                .city(user.getCity())
                .zipCode(user.getZipCode())
                .role(user.getRole().name())
                .enabled(user.isEnabled())
                .createdAt(user.getCreatedAt() != null ?
                        user.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null)
                .build();
    }
}