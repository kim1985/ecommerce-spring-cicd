package com.myecom.controller;

import com.myecom.dto.auth.AuthResponse;
import com.myecom.dto.auth.LoginRequest;
import com.myecom.dto.auth.RegisterRequest;
import com.myecom.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Registrazione nuovo utente
     *
     * POST http://localhost:8080/api/auth/register
     *
     * Body:
     * {
     *   "email": "mario@example.com",
     *   "password": "password123",
     *   "firstName": "Mario",
     *   "lastName": "Rossi",
     *   "phone": "123456789",
     *   "address": "Via Roma 1",
     *   "city": "Milano",
     *   "zipCode": "20100"
     * }
     */
    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    /**
     * Login utente
     *
     * POST http://localhost:8080/api/auth/login
     *
     * Body:
     * {
     *   "email": "mario@example.com",
     *   "password": "password123"
     * }
     */
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}