package com.myecom.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * Service per inviare notifiche tramite AWS Lambda
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LambdaNotificationService {

    private final WebClient.Builder webClientBuilder;

    // URL della tua Lambda Function (da application.properties)
    @Value("${aws.lambda.notification.url:}")
    private String lambdaUrl;

    /**
     * Invia notifica di ordine creato tramite Lambda
     */
    public void sendOrderCreatedNotification(String userEmail, String orderNumber, String firstName) {
        if (lambdaUrl == null || lambdaUrl.trim().isEmpty()) {
            log.warn("Lambda URL non configurato - skip notifica per ordine {}", orderNumber);
            return;
        }

        try {
            // Prepara i dati da inviare alla Lambda
            Map<String, Object> notificationData = new HashMap<>();
            notificationData.put("email", userEmail);
            notificationData.put("firstName", firstName != null ? firstName : "Cliente");
            notificationData.put("orderNumber", orderNumber);
            notificationData.put("type", "ORDER_CREATED");

            log.info("Invio notifica Lambda per ordine {} a {}", orderNumber, userEmail);

            // Chiamata asincrona alla Lambda (non bloccante)
            WebClient webClient = webClientBuilder.build();

            // ASINCRONO - non blocca il thread!
            webClient.post() // "Voglio fare una chiamata POST"
                    .uri(lambdaUrl) // "Al numero: https://lambda-url..."
                    .contentType(MediaType.APPLICATION_JSON) // "Parlerò in formato JSON"
                    .bodyValue(notificationData) // "Dirò questi dati"
                    .retrieve() // "Fai la chiamata!"
                    .bodyToMono(String.class) // "Dammi la risposta come testo"
                    .doOnSuccess(response ->
                            log.info("Notifica Lambda inviata con successo per ordine {}: {}", orderNumber, response))
                    .doOnError(error ->
                            log.error("Errore invio notifica Lambda per ordine {}: {}", orderNumber, error.getMessage()))
                    .onErrorResume(error -> {
                        // In caso di errore, non bloccare la creazione dell'ordine
                        log.warn("Notifica fallita ma ordine {} creato comunque", orderNumber);
                        return Mono.empty();
                    })
                    .subscribe(); // Esecuzione in background

        } catch (Exception e) {
            log.error("Errore preparazione notifica Lambda per ordine {}: {}", orderNumber, e.getMessage());
        }
    }

    /**
     * Invia notifica di benvenuto per nuovo utente
     */
    public void sendWelcomeNotification(String userEmail, String firstName) {
        if (lambdaUrl == null || lambdaUrl.trim().isEmpty()) {
            log.warn("Lambda URL non configurato - skip notifica benvenuto per {}", userEmail);
            return;
        }

        try {
            Map<String, Object> welcomeData = new HashMap<>();
            welcomeData.put("email", userEmail);
            welcomeData.put("firstName", firstName != null ? firstName : "Utente");
            welcomeData.put("type", "WELCOME");

            log.info("Invio notifica benvenuto Lambda per {}", userEmail);

            WebClient webClient = webClientBuilder.build();

            webClient.post()
                    .uri(lambdaUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(welcomeData)
                    .retrieve()
                    .bodyToMono(String.class)
                    .doOnSuccess(response ->
                            log.info("Notifica benvenuto Lambda inviata per {}: {}", userEmail, response))
                    .doOnError(error ->
                            log.error("Errore notifica benvenuto Lambda per {}: {}", userEmail, error.getMessage()))
                    .onErrorResume(error -> Mono.empty())
                    .subscribe();

        } catch (Exception e) {
            log.error("Errore notifica benvenuto Lambda per {}: {}", userEmail, e.getMessage());
        }
    }
}