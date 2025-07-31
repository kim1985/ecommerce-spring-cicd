package com.myecom.service;

import com.myecom.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test per JwtService che verifica la generazione e validazione dei token JWT.
 *
 * Questi test assicurano che:
 * - I token vengano generati correttamente
 * - La validazione funzioni per token validi e invalidi
 * - La gestione della scadenza sia corretta
 * - L'estrazione dei dati dal token funzioni
 */
@SpringBootTest
@ActiveProfiles("test")
class JwtServiceTest {

    private JwtService jwtService;
    private User testUser;

    @BeforeEach
    void setUp() {
        // Crea il servizio JWT con configurazione di test
        jwtService = new JwtService();

        // Imposta valori di test usando reflection (per evitare dipendenze da file properties)
        ReflectionTestUtils.setField(jwtService, "secretKey", "testSecretKey123456789012345678901234567890");
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 3600000L); // 1 ora per i test

        // Crea utente di test
        testUser = User.builder()
                .id(1L)
                .email("test@jwt.com")
                .password("hashedPassword")
                .firstName("Test")
                .lastName("User")
                .role(User.Role.USER)
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void shouldGenerateValidJwtToken() {
        // When - genera token per utente
        String token = jwtService.generateToken(testUser);

        // Then - verifica che il token sia stato generato
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(token.split("\\.")).hasSize(3); // JWT ha 3 parti separate da punti

        // Verifica che contenga l'email dell'utente
        String extractedEmail = jwtService.extractUsername(token);
        assertThat(extractedEmail).isEqualTo("test@jwt.com");
    }

    @Test
    void shouldGenerateTokenWithExtraClaims() {
        // Given - prepara claims personalizzati
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("role", "USER");
        extraClaims.put("userId", 1L);

        // When - genera token con claims extra
        String token = jwtService.generateToken(extraClaims, testUser);

        // Then - verifica che il token contenga i dati
        assertThat(token).isNotNull();
        String extractedEmail = jwtService.extractUsername(token);
        assertThat(extractedEmail).isEqualTo("test@jwt.com");
    }

    @Test
    void shouldExtractUsernameFromToken() {
        // Given - genera token
        String token = jwtService.generateToken(testUser);

        // When - estrae username
        String username = jwtService.extractUsername(token);

        // Then - verifica che sia corretto
        assertThat(username).isEqualTo("test@jwt.com");
    }

    @Test
    void shouldExtractExpirationFromToken() {
        // Given - genera token
        String token = jwtService.generateToken(testUser);

        // When - estrae data scadenza
        Date expiration = jwtService.extractExpiration(token);

        // Then - verifica che sia nel futuro (token valido per 1 ora)
        assertThat(expiration).isAfter(new Date());
        assertThat(expiration).isBefore(new Date(System.currentTimeMillis() + 3700000)); // Poco più di 1 ora
    }

    @Test
    void shouldValidateCorrectToken() {
        // Given - genera token valido
        String token = jwtService.generateToken(testUser);

        // When - valida token
        boolean isValid = jwtService.isTokenValid(token, testUser);

        // Then - deve essere valido
        assertThat(isValid).isTrue();
    }

    @Test
    void shouldRejectInvalidToken() {
        // Given - token completamente inventato
        String invalidToken = "invalid.jwt.token";

        // When & Then - deve lanciare eccezione
        assertThatThrownBy(() -> jwtService.extractUsername(invalidToken))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Token JWT non valido");
    }

    @Test
    void shouldRejectTokenForWrongUser() {
        // Given - genera token per testUser
        String token = jwtService.generateToken(testUser);

        // Crea utente diverso
        User wrongUser = User.builder()
                .email("wrong@user.com")
                .password("password")
                .firstName("Wrong")
                .lastName("User")
                .role(User.Role.USER)
                .enabled(true)
                .build();

        // When - prova a validare token con utente sbagliato
        boolean isValid = jwtService.isTokenValid(token, wrongUser);

        // Then - deve essere falso
        assertThat(isValid).isFalse();
    }

    @Test
    void shouldRejectExpiredToken() {
        // Given - crea servizio con token che scade subito
        JwtService shortLivedJwtService = new JwtService();
        ReflectionTestUtils.setField(shortLivedJwtService, "secretKey", "testSecretKey123456789012345678901234567890");
        ReflectionTestUtils.setField(shortLivedJwtService, "jwtExpiration", 1L); // 1 millisecondo

        String token = shortLivedJwtService.generateToken(testUser);

        // Aspetta che scada
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // When - valida token scaduto
        boolean isValid = shortLivedJwtService.isTokenValid(token, testUser);

        // Then - deve essere falso
        assertThat(isValid).isFalse();
    }

    @Test
    void shouldWorkWithCompatibilityMethods() {
        // Given - genera token
        String token = jwtService.generateToken(testUser);

        // When - usa metodi di compatibilità
        boolean isValidSimple = jwtService.isValidToken(token);
        String emailFromToken = jwtService.getEmailFromToken(token);

        // Then - devono funzionare
        assertThat(isValidSimple).isTrue();
        assertThat(emailFromToken).isEqualTo("test@jwt.com");
    }

    @Test
    void shouldHandleNullTokenGracefully() {
        // When & Then - metodi devono gestire token null senza crashare
        assertThat(jwtService.getEmailFromToken(null)).isNull();
        assertThat(jwtService.isValidToken(null)).isFalse();
    }
}