package com.myecom.service;

import com.myecom.dto.auth.RegisterRequest;
import com.myecom.dto.auth.UserResponse;
import com.myecom.model.User;
import com.myecom.repository.UserRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit test per UserService con Mock
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private RegisterRequest registerRequest;
    private User testUser;

    @BeforeEach
    void setUp() {
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

        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .password("hashedPassword")
                .firstName("Mario")
                .lastName("Rossi")
                .phone("123456789")
                .address("Via Roma 1")
                .city("Milano")
                .zipCode("20100")
                .role(User.Role.USER)
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void shouldRegisterNewUser() {
        // Given
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        UserResponse response = userService.registerUser(registerRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getEmail()).isEqualTo("test@example.com");
        assertThat(response.getFirstName()).isEqualTo("Mario");
        assertThat(response.getLastName()).isEqualTo("Rossi");
        assertThat(response.getRole()).isEqualTo("USER");
        assertThat(response.isEnabled()).isTrue();

        // Verifica chiamate
        verify(userRepository).existsByEmail("test@example.com");
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void shouldNotRegisterUserWithDuplicateEmail() {
        // Given
        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> userService.registerUser(registerRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Email già registrata");

        // Verifica che non ci siano altre chiamate
        verify(userRepository).existsByEmail("test@example.com");
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void shouldFindUserByEmail() {
        // Given
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));

        // When
        Optional<User> found = userService.findByEmail("test@example.com");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("test@example.com");
        assertThat(found.get().getFirstName()).isEqualTo("Mario");

        verify(userRepository).findByEmail("test@example.com");
    }

    @Test
    void shouldNotFindUserWithNonExistentEmail() {
        // Given
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        // When
        Optional<User> found = userService.findByEmail("nonexistente@example.com");

        // Then
        assertThat(found).isEmpty();

        verify(userRepository).findByEmail("nonexistente@example.com");
    }

    @Test
    void shouldFindUserById() {
        // Given
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(testUser));

        // When
        Optional<UserResponse> found = userService.findById(1L);

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(1L);
        assertThat(found.get().getEmail()).isEqualTo("test@example.com");

        verify(userRepository).findById(1L);
    }

    @Test
    void shouldNotFindUserWithNonExistentId() {
        // Given
        when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

        // When
        Optional<UserResponse> found = userService.findById(999L);

        // Then
        assertThat(found).isEmpty();

        verify(userRepository).findById(999L);
    }
}