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
 * Test aggiornati per AuthService che ora usa JWT reali invece di token dummy.
 *
 * Verifica che:
 * - La registrazione generi token JWT validi
 * - Il login restituisca token JWT veri
 * - La validazione dei token funzioni correttamente
 * - Gli errori siano gestiti appropriatamente
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private UserResponse userResponse;
    private User testUser;
    private String mockJwtToken;

    @BeforeEach
    void setUp() {
        // Prepara dati di test
        registerRequest = RegisterRequest.builder()
                .email("auth@jwt.com")
                .password("password123")
                .firstName("Auth")
                .lastName("User")
                .build();

        loginRequest = LoginRequest.builder()
                .email("auth@jwt.com")
                .password("password123")
                .build();

        userResponse = UserResponse.builder()
                .id(1L)
                .email("auth@jwt.com")
                .firstName("Auth")
                .lastName("User")
                .role("USER")
                .enabled(true)
                .build();

        testUser = User.builder()
                .id(1L)
                .email("auth@jwt.com")
                .password("hashedPassword")
                .firstName("Auth")
                .lastName("User")
                .role(User.Role.USER)
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .build();

        // Token JWT simulato per i test (un JWT vero è troppo lungo per i test)
        mockJwtToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test.token";
    }

    @Test
    void shouldRegisterUserAndReturnJwtToken() {
        // Given - configura mock per restituire dati attesi
        when(userService.registerUser(registerRequest)).thenReturn(userResponse);
        when(userService.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(jwtService.generateToken(testUser)).thenReturn(mockJwtToken);

        // When - esegue registrazione
        AuthResponse response = authService.register(registerRequest);

        // Then - verifica che la risposta contenga un token JWT valido
        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo(mockJwtToken);
        assertThat(response.getType()).isEqualTo("Bearer");
        assertThat(response.getUser()).isNotNull();
        assertThat(response.getUser().getEmail()).isEqualTo("auth@jwt.com");
        assertThat(response.getUser().getFirstName()).isEqualTo("Auth");
        assertThat(response.getUser().getRole()).isEqualTo("USER");
    }

    @Test
    void shouldLoginUserAndReturnJwtToken() {
        // Given - configura mock per login corretto
        when(userService.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(userService.findById(anyLong())).thenReturn(Optional.of(userResponse));
        when(jwtService.generateToken(testUser)).thenReturn(mockJwtToken);

        // When - esegue login
        AuthResponse response = authService.login(loginRequest);

        // Then - verifica che restituisca token JWT
        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo(mockJwtToken);
        assertThat(response.getType()).isEqualTo("Bearer");
        assertThat(response.getUser().getEmail()).isEqualTo("auth@jwt.com");
    }

    @Test
    void shouldRejectLoginWithWrongEmail() {
        // Given - utente non esiste
        when(userService.findByEmail(anyString())).thenReturn(Optional.empty());

        // When & Then - deve lanciare eccezione
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Email o password non corretti");
    }

    @Test
    void shouldRejectLoginWithWrongPassword() {
        // Given - utente esiste ma password sbagliata
        when(userService.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        // When & Then - deve rifiutare login
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Email o password non corretti");
    }

    @Test
    void shouldRejectLoginForDisabledUser() {
        // Given - utente disabilitato
        User disabledUser = User.builder()
                .id(1L)
                .email("auth@jwt.com")
                .password("hashedPassword")
                .firstName("Auth")
                .lastName("User")
                .role(User.Role.USER)
                .enabled(false) // Account disabilitato
                .createdAt(LocalDateTime.now())
                .build();

        when(userService.findByEmail(anyString())).thenReturn(Optional.of(disabledUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

        // When & Then - deve rifiutare login
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Account disabilitato");
    }

    @Test
    void shouldValidateTokenAndReturnUser() {
        // Given - prepara token valido
        String bearerToken = "Bearer " + mockJwtToken;
        when(jwtService.extractUsername(mockJwtToken)).thenReturn("auth@jwt.com");
        when(userService.findByEmail("auth@jwt.com")).thenReturn(Optional.of(testUser));
        when(jwtService.isTokenValid(mockJwtToken, testUser)).thenReturn(true);

        // When - valida token
        User validatedUser = authService.validateTokenAndGetUser(bearerToken);

        // Then - deve restituire l'utente corretto
        assertThat(validatedUser).isNotNull();
        assertThat(validatedUser.getEmail()).isEqualTo("auth@jwt.com");
        assertThat(validatedUser.getId()).isEqualTo(1L);
    }

    @Test
    void shouldRejectInvalidTokenFormat() {
        // Given - token senza "Bearer " prefix
        String invalidToken = "invalid-token-format";

        // When & Then - deve rifiutare token
        assertThatThrownBy(() -> authService.validateTokenAndGetUser(invalidToken))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Token non valido");
    }

    @Test
    void shouldRejectExpiredToken() {
        // Given - token scaduto
        String bearerToken = "Bearer " + mockJwtToken;
        when(jwtService.extractUsername(mockJwtToken)).thenReturn("auth@jwt.com");
        when(userService.findByEmail("auth@jwt.com")).thenReturn(Optional.of(testUser));
        when(jwtService.isTokenValid(mockJwtToken, testUser)).thenReturn(false); // Token scaduto

        // When & Then - deve rifiutare token scaduto
        assertThatThrownBy(() -> authService.validateTokenAndGetUser(bearerToken))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Token scaduto o non valido");
    }

    @Test
    void shouldRejectTokenForNonExistentUser() {
        // Given - token con email di utente che non esiste più
        String bearerToken = "Bearer " + mockJwtToken;
        when(jwtService.extractUsername(mockJwtToken)).thenReturn("nonexistent@user.com");
        when(userService.findByEmail("nonexistent@user.com")).thenReturn(Optional.empty());

        // When & Then - deve rifiutare perché utente non esiste
        assertThatThrownBy(() -> authService.validateTokenAndGetUser(bearerToken))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Utente non trovato");
    }
}