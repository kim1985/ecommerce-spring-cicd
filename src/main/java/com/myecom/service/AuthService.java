package com.myecom.service;

import com.myecom.dto.auth.AuthResponse;
import com.myecom.dto.auth.LoginRequest;
import com.myecom.dto.auth.RegisterRequest;
import com.myecom.dto.auth.UserResponse;
import com.myecom.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    // Registra nuovo utente
    public AuthResponse register(RegisterRequest request) {
        UserResponse userResponse = userService.registerUser(request);

        return AuthResponse.builder()
                .token("dummy-token-" + userResponse.getId()) // Token semplificato per ora
                .type("Bearer")
                .user(userResponse)
                .build();
    }

    // Effettua login
    public AuthResponse login(LoginRequest request) {
        // Trova utente
        User user = userService.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Email o password non corretti"));

        // Verifica password
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Email o password non corretti");
        }

        // Verifica che l'utente sia attivo
        if (!user.isEnabled()) {
            throw new IllegalArgumentException("Account disabilitato");
        }

        UserResponse userResponse = userService.findById(user.getId())
                .orElseThrow(() -> new IllegalStateException("Errore nel recupero utente"));

        return AuthResponse.builder()
                .token("dummy-token-" + user.getId()) // Token semplificato per ora
                .type("Bearer")
                .user(userResponse)
                .build();
    }
}