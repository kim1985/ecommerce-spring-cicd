package com.myecom.service;

import com.myecom.dto.auth.AuthResponse;
import com.myecom.dto.auth.LoginRequest;
import com.myecom.dto.auth.RegisterRequest;
import com.myecom.dto.auth.UserResponse;
import com.myecom.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Unit test per AuthService con Mock
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private UserResponse userResponse;
    private User testUser;

    @BeforeEach
    void setUp() {
        registerRequest = RegisterRequest.builder()
                .email("auth@example.com")
                .password("password123")
                .firstName("Auth")
                .lastName("User")
                .build();

        loginRequest = LoginRequest.builder()
                .email("auth@example.com")
                .password("password123")
                .build();

        userResponse = UserResponse.builder()
                .id(1L)
                .email("auth@example.com")
                .firstName("Auth")
                .lastName("User")
                .role("USER")
                .enabled(true)
                .createdAt(LocalDateTime.now().toString())
                .build();

        testUser = User.builder()
                .id(1L)
                .email("auth@example.com")
                .password("hashedPassword")
                .firstName("Auth")
                .lastName("User")
                .role(User.Role.USER)
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void shouldRegisterAndReturnAuthResponse() {
        // Given
        when(userService.registerUser(registerRequest)).thenReturn(userResponse);

        // When
        AuthResponse response = authService.register(registerRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getToken()).isNotNull();
        assertThat(response.getToken()).startsWith("dummy-token-");
        assertThat(response.getType()).isEqualTo("Bearer");
        assertThat(response.getUser()).isNotNull();
        assertThat(response.getUser().getEmail()).isEqualTo("auth@example.com");
    }

    @Test
    void shouldLoginWithCorrectCredentials() {
        // Given
        when(userService.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(userService.findById(anyLong())).thenReturn(Optional.of(userResponse));

        // When
        AuthResponse response = authService.login(loginRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getToken()).isNotNull();
        assertThat(response.getToken()).startsWith("dummy-token-");
        assertThat(response.getType()).isEqualTo("Bearer");
        assertThat(response.getUser()).isNotNull();
        assertThat(response.getUser().getEmail()).isEqualTo("auth@example.com");
    }

    @Test
    void shouldNotLoginWithWrongEmail() {
        // Given
        when(userService.findByEmail(anyString())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Email o password non corretti");
    }

    @Test
    void shouldNotLoginWithWrongPassword() {
        // Given
        when(userService.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Email o password non corretti");
    }

    @Test
    void shouldNotLoginWithDisabledUser() {
        // Given
        User disabledUser = User.builder()
                .id(1L)
                .email("auth@example.com")
                .password("hashedPassword")
                .firstName("Auth")
                .lastName("User")
                .role(User.Role.USER)
                .enabled(false) // Utente disabilitato
                .createdAt(LocalDateTime.now())
                .build();

        when(userService.findByEmail(anyString())).thenReturn(Optional.of(disabledUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Account disabilitato");
    }
}