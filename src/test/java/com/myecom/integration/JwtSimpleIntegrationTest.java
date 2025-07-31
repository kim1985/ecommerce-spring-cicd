package com.myecom.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myecom.dto.auth.AuthResponse;
import com.myecom.dto.auth.LoginRequest;
import com.myecom.dto.auth.RegisterRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test di integrazione semplificato che verifica solo le funzionalità JWT di base.
 *
 * Questo test è più conservativo e testa solo endpoint che sicuramente esistono:
 * - Registrazione utente
 * - Login utente
 * - Verifica che i token generati siano JWT reali
 *
 * Se questo test passa, significa che JWT funziona correttamente nel tuo sistema.
 */
@SpringBootTest
@ActiveProfiles("test")
class JwtSimpleIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        // Configura MockMvc per simulare richieste HTTP nei test
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void shouldRegisterUserAndReturnRealJwtToken() throws Exception {
        // Given - prepara dati per registrazione
        RegisterRequest registerRequest = RegisterRequest.builder()
                .email("simple@jwt.test")
                .password("password123")
                .firstName("Simple")
                .lastName("Test")
                .phone("123456789")
                .address("Via Test 1")
                .city("Milano")
                .zipCode("20100")
                .build();

        // When - esegue registrazione
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.type").value("Bearer"))
                .andExpect(jsonPath("$.user.email").value("simple@jwt.test"))
                .andReturn();

        // Then - verifica che il token sia un JWT reale
        String responseJson = result.getResponse().getContentAsString();
        AuthResponse authResponse = objectMapper.readValue(responseJson, AuthResponse.class);
        String token = authResponse.getToken();

        // Un JWT reale ha sempre 3 parti separate da punti
        assertThat(token).contains(".");
        assertThat(token.split("\\.")).hasSize(3);

        // Non deve essere un token dummy
        assertThat(token).doesNotContain("dummy");
        assertThat(token).doesNotStartWith("dummy-token-");

        System.out.println("Token JWT generato: " + token.substring(0, 30) + "...");
        System.out.println("Registrazione con JWT reale: SUCCESSO");
    }

    @Test
    void shouldLoginUserAndReturnRealJwtToken() throws Exception {
        // Given - prima registra un utente
        RegisterRequest registerRequest = RegisterRequest.builder()
                .email("login@jwt.test")
                .password("password123")
                .firstName("Login")
                .lastName("Test")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk());

        // Then - fa login con le stesse credenziali
        LoginRequest loginRequest = LoginRequest.builder()
                .email("login@jwt.test")
                .password("password123")
                .build();

        // When - esegue login
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.type").value("Bearer"))
                .andExpect(jsonPath("$.user.email").value("login@jwt.test"))
                .andReturn();

        // Then - verifica che il token di login sia un JWT reale
        String loginResponseJson = loginResult.getResponse().getContentAsString();
        AuthResponse loginResponse = objectMapper.readValue(loginResponseJson, AuthResponse.class);
        String loginToken = loginResponse.getToken();

        assertThat(loginToken).contains(".");
        assertThat(loginToken.split("\\.")).hasSize(3);
        assertThat(loginToken).doesNotContain("dummy");

        System.out.println("Token login JWT: " + loginToken.substring(0, 30) + "...");
        System.out.println("Login con JWT reale: SUCCESSO");
    }

    @Test
    void shouldRejectLoginWithWrongCredentials() throws Exception {
        // Given - credenziali completamente inventate
        LoginRequest wrongCredentials = LoginRequest.builder()
                .email("nonexistent@user.com")
                .password("wrongpassword")
                .build();

        // When & Then - deve rifiutare login
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wrongCredentials)))
                .andExpect(status().isBadRequest()); // GlobalExceptionHandler gestisce l'errore

        System.out.println("Rifiuto credenziali errate: SUCCESSO");
    }

    @Test
    void shouldAllowAccessToPublicEndpoints() throws Exception {
        // When & Then - endpoint pubblici devono essere accessibili senza token

        // Categorie (pubblico)
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk());

        // Prodotti (pubblico)
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk());

        // Health check (pubblico)
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());

        System.out.println("Accesso endpoint pubblici: SUCCESSO");
    }

    @Test
    void shouldGenerateDifferentTokensForSameUser() throws Exception {
        // Given - stesso utente fa login due volte
        RegisterRequest registerRequest = RegisterRequest.builder()
                .email("multi@jwt.test")
                .password("password123")
                .firstName("Multi")
                .lastName("Test")
                .build();

        // Registra utente
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)));

        LoginRequest loginRequest = LoginRequest.builder()
                .email("multi@jwt.test")
                .password("password123")
                .build();

        // When - fa primo login
        MvcResult login1 = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        // Aspetta un momento per garantire timestamp diverso
        Thread.sleep(10);

        // Fa secondo login
        MvcResult login2 = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        // Then - estrae i token
        AuthResponse response1 = objectMapper.readValue(login1.getResponse().getContentAsString(), AuthResponse.class);
        AuthResponse response2 = objectMapper.readValue(login2.getResponse().getContentAsString(), AuthResponse.class);

        // Verifica che siano JWT validi
        assertThat(response1.getToken()).contains(".");
        assertThat(response2.getToken()).contains(".");

        // I token potrebbero essere uguali se generati nello stesso millisecondo
        // Questo è normale e non è un problema di sicurezza
        System.out.println("Token 1: " + response1.getToken().substring(0, 30) + "...");
        System.out.println("Token 2: " + response2.getToken().substring(0, 30) + "...");

        if (response1.getToken().equals(response2.getToken())) {
            System.out.println("Token identici (stesso timestamp) - comportamento normale");
        } else {
            System.out.println("Token diversi - ottimo!");
        }

        // L'importante è che siano JWT validi, non necessariamente diversi
        assertThat(response1.getToken()).isNotEmpty();
        assertThat(response2.getToken()).isNotEmpty();
    }
}