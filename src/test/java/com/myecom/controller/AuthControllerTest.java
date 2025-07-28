package com.myecom.controller;

import com.myecom.dto.auth.AuthResponse;
import com.myecom.dto.auth.LoginRequest;
import com.myecom.dto.auth.RegisterRequest;
import com.myecom.dto.auth.UserResponse;
import com.myecom.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private AuthResponse authResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController).build();

        registerRequest = RegisterRequest.builder()
                .email("test@example.com")
                .password("password123")
                .firstName("Mario")
                .lastName("Rossi")
                .phone("123456789")
                .address("Via Roma 1")
                .city("Milano")
                .zipCode("20100")
                .build();

        loginRequest = LoginRequest.builder()
                .email("test@example.com")
                .password("password123")
                .build();

        UserResponse userResponse = UserResponse.builder()
                .id(1L)
                .email("test@example.com")
                .firstName("Mario")
                .lastName("Rossi")
                .role("USER")
                .enabled(true)
                .build();

        authResponse = AuthResponse.builder()
                .token("dummy-token-1")
                .type("Bearer")
                .user(userResponse)
                .build();
    }

    @Test
    void shouldRegisterUser() throws Exception {
        // Given
        when(authService.register(any(RegisterRequest.class))).thenReturn(authResponse);

        // When & Then
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.token").value("dummy-token-1"))
                .andExpect(jsonPath("$.type").value("Bearer"))
                .andExpect(jsonPath("$.user.email").value("test@example.com"))
                .andExpect(jsonPath("$.user.firstName").value("Mario"))
                .andExpect(jsonPath("$.user.role").value("USER"));
    }

    @Test
    void shouldLoginUser() throws Exception {
        // Given
        when(authService.login(any(LoginRequest.class))).thenReturn(authResponse);

        // When & Then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.token").value("dummy-token-1"))
                .andExpect(jsonPath("$.type").value("Bearer"))
                .andExpect(jsonPath("$.user.email").value("test@example.com"));
    }

    @Test
    @Disabled("Disabilitato temporaneamente - da sistemare con nuova gestione errori")
    void shouldHandleServiceException() throws Exception {
        // Given
        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new IllegalArgumentException("Email già registrata"));

        // When & Then - Ora ci aspettiamo una risposta JSON strutturata
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Richiesta non valida: Email già registrata"));
    }

    @Test
    @Disabled("Disabilitato temporaneamente - da sistemare con nuova gestione errori")
    void shouldHandleLoginException() throws Exception {
        // Given
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new IllegalArgumentException("Email o password non corretti"));

        // When & Then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Richiesta non valida: Email o password non corretti"));
    }
}