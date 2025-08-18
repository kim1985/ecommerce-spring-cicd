package com.myecom.listeners;

import com.myecom.events.OrderCreatedEvent;
import com.myecom.service.LambdaNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Listener che reagisce agli eventi di creazione ordini.
 *
 * Ora integrato con AWS Lambda per inviare notifiche reali!
 * Pattern: Spring Events → Lambda Function → Email/Notifications
 */
@Component
@RequiredArgsConstructor
public class OrderNotificationListener {

    // Injection del service che gestisce le chiamate AWS Lambda
    private final LambdaNotificationService lambdaNotificationService;

    /**
     * Gestisce l'evento "ordine creato" chiamando AWS Lambda.
     *
     * OrderService pubblica evento → questo metodo → Lambda → Email
     *
     * @param event Contiene dati dell'ordine (ID, email, numero ordine)
     */
    @EventListener
    public void handleNewOrder(OrderCreatedEvent event) {
        // Prima: System.out.println (solo console)
        // Ora: Chiamata AWS Lambda (email reale!)

        lambdaNotificationService.sendOrderCreatedNotification(
                event.getUserEmail(),           // Email destinatario
                event.getOrderNumber(),         // Numero ordine (es: ORD-123456)
                extractFirstName(event.getUserEmail())  // Nome del cliente
        );

        // Nota: La chiamata è asincrona, non blocca la creazione dell'ordine
    }

    /**
     * Estrae il nome dal formato email quando il nome non è disponibile.
     *
     * Fallback strategy: mario.rossi@gmail.com → "mario"
     *
     * @param email Email del cliente
     * @return Nome estratto o "Cliente" se impossibile estrarre
     */
    private String extractFirstName(String email) {
        try {
            // Prende la parte prima della @ e prima del primo punto
            String localPart = email.split("@")[0];  // mario.rossi@gmail.com → mario.rossi
            String firstName = localPart.split("\\.")[0];  // mario.rossi → mario

            // Capitalizza la prima lettera
            return firstName.substring(0, 1).toUpperCase() + firstName.substring(1).toLowerCase();

        } catch (Exception e) {
            // Se qualcosa va storto, usa fallback generico
            return "Cliente";
        }
    }
}