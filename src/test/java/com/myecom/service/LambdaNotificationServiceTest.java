package com.myecom.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Test per LambdaNotificationService usando Mock di WebClient
 * Approccio semplificato senza dipendenze esterne
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LambdaNotificationServiceTest {

    @Mock
    private WebClient.Builder webClientBuilder;

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private WebClient.RequestBodySpec requestBodySpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    @InjectMocks
    private LambdaNotificationService lambdaNotificationService;

    @BeforeEach
    void setUp() {
        // Configura il service con URL test (solo quello che serve sempre)
        ReflectionTestUtils.setField(lambdaNotificationService, "lambdaUrl", "https://test-lambda-url.com");
    }

    /**
     * Setup mock WebClient chain per test che ne hanno bisogno
     */
    private void setupWebClientMocks() {
        when(webClientBuilder.build()).thenReturn(webClient);
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just("Success response"));
    }

    @Test
    void shouldSendOrderCreatedNotificationSuccessfully() {
        // Given - Setup mocks
        setupWebClientMocks();

        // When - Invia notifica ordine
        assertThatNoException().isThrownBy(() ->
                lambdaNotificationService.sendOrderCreatedNotification(
                        "test@example.com",
                        "ORD-12345",
                        "Mario"
                )
        );

        // Then - Verifica che WebClient sia stato chiamato correttamente
        verify(webClientBuilder).build();
        verify(webClient).post();
        verify(requestBodyUriSpec).uri("https://test-lambda-url.com");
        verify(requestBodySpec).bodyValue(any());
    }

    @Test
    void shouldSendWelcomeNotificationSuccessfully() {
        // Given - Setup mocks
        setupWebClientMocks();

        // When - Invia notifica benvenuto
        assertThatNoException().isThrownBy(() ->
                lambdaNotificationService.sendWelcomeNotification("welcome@example.com", "Luigi")
        );

        // Then - Verifica chiamate WebClient
        verify(webClientBuilder).build();
        verify(webClient).post();
    }

    @Test
    void shouldSkipNotificationWhenLambdaUrlNotConfigured() {
        // Given - URL Lambda vuoto
        ReflectionTestUtils.setField(lambdaNotificationService, "lambdaUrl", "");

        // When - Prova a inviare notifica
        lambdaNotificationService.sendOrderCreatedNotification(
                "test@example.com",
                "ORD-12345",
                "Mario"
        );

        // Then - WebClient non dovrebbe essere chiamato
        verify(webClientBuilder, never()).build();
    }

    @Test
    void shouldSkipNotificationWhenLambdaUrlIsNull() {
        // Given - URL Lambda null
        ReflectionTestUtils.setField(lambdaNotificationService, "lambdaUrl", null);

        // When - Prova a inviare notifica
        lambdaNotificationService.sendWelcomeNotification("test@example.com", "Mario");

        // Then - Nessuna chiamata HTTP dovrebbe essere fatta
        verify(webClientBuilder, never()).build();
    }

    @Test
    void shouldHandleNullFirstNameGracefully() {
        // Given - Setup mocks
        setupWebClientMocks();

        // When - Invia notifica con firstName null
        assertThatNoException().isThrownBy(() ->
                lambdaNotificationService.sendOrderCreatedNotification(
                        "test@example.com",
                        "ORD-12345",
                        null  // firstName null
                )
        );

        // Then - Non dovrebbe lanciare eccezioni
        verify(webClientBuilder).build();
    }

    @Test
    void shouldNotThrowExceptionOnWebClientError() {
        // Given - WebClient che lancia errore
        setupWebClientMocks();
        when(responseSpec.bodyToMono(String.class))
                .thenReturn(Mono.error(new RuntimeException("Connection failed")));

        // When - Invia notifica (non dovrebbe lanciare eccezione)
        assertThatNoException().isThrownBy(() ->
                lambdaNotificationService.sendOrderCreatedNotification(
                        "error@example.com",
                        "ORD-ERROR",
                        "Error"
                )
        );

        // Then - Il service gestisce l'errore gracefully
        verify(webClientBuilder).build();
    }
}