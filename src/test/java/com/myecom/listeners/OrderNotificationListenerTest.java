package com.myecom.listeners;

import com.myecom.events.OrderCreatedEvent;
import com.myecom.service.LambdaNotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

/**
 * Test per OrderNotificationListener che verifica l'integrazione con LambdaNotificationService
 */
@ExtendWith(MockitoExtension.class)
class OrderNotificationListenerTest {

    @Mock
    private LambdaNotificationService lambdaNotificationService;

    @InjectMocks
    private OrderNotificationListener orderNotificationListener;

    @Test
    void shouldCallLambdaServiceWhenOrderCreated() {
        // Given - Evento di ordine creato
        OrderCreatedEvent event = new OrderCreatedEvent(
                1L,                           // orderId
                "mario.rossi@example.com",    // userEmail
                "ORD-1234567890"             // orderNumber
        );

        // When - Gestisce l'evento
        orderNotificationListener.handleNewOrder(event);

        // Then - Verifica che LambdaNotificationService sia stato chiamato
        verify(lambdaNotificationService).sendOrderCreatedNotification(
                "mario.rossi@example.com",  // email
                "ORD-1234567890",          // orderNumber
                "Mario"                     // firstName estratto da email
        );
    }

    @Test
    void shouldCallLambdaServiceWithComplexEmail() {
        // Given - Evento con email complessa
        OrderCreatedEvent event = new OrderCreatedEvent(
                2L,
                "luigi.verdi.test@company.com",
                "ORD-COMPLEX-123"
        );

        // When - Gestisce l'evento
        orderNotificationListener.handleNewOrder(event);

        // Then - Verifica estrazione nome da email complessa
        verify(lambdaNotificationService).sendOrderCreatedNotification(
                "luigi.verdi.test@company.com",
                "ORD-COMPLEX-123",
                "Luigi"  // Prima parte prima del primo punto
        );
    }

    @Test
    void shouldCallLambdaServiceWithSimpleEmail() {
        // Given - Email senza punti
        OrderCreatedEvent event = new OrderCreatedEvent(
                3L,
                "test@example.com",
                "ORD-SIMPLE-456"
        );

        // When - Gestisce l'evento
        orderNotificationListener.handleNewOrder(event);

        // Then - Verifica estrazione nome da email semplice
        verify(lambdaNotificationService).sendOrderCreatedNotification(
                "test@example.com",
                "ORD-SIMPLE-456",
                "Test"  // Parte prima della @
        );
    }

    @Test
    void shouldHandleEmailWithoutValidFormat() {
        // Given - Email con formato strano
        OrderCreatedEvent event = new OrderCreatedEvent(
                4L,
                "invalid-email-format",  // Senza @
                "ORD-INVALID-789"
        );

        // When - Gestisce l'evento (non dovrebbe crashare)
        orderNotificationListener.handleNewOrder(event);

        // Then - Verifica che venga chiamato con nome estratto dalla stringa
        verify(lambdaNotificationService).sendOrderCreatedNotification(
                "invalid-email-format",
                "ORD-INVALID-789",
                "Invalid-email-format"  // Il metodo prende tutta la stringa e la capitalizza
        );
    }

    @Test
    void shouldCapitalizeFirstName() {
        // Given - Email con nome in minuscolo
        OrderCreatedEvent event = new OrderCreatedEvent(
                5L,
                "marco@example.com",
                "ORD-CAPITALIZE-999"
        );

        // When - Gestisce l'evento
        orderNotificationListener.handleNewOrder(event);

        // Then - Verifica capitalizzazione
        verify(lambdaNotificationService).sendOrderCreatedNotification(
                "marco@example.com",
                "ORD-CAPITALIZE-999",
                "Marco"  // Capitalizzato
        );
    }

    @Test
    void shouldUseFallbackForEmptyString() {
        // Given - Email vuota (caso edge)
        OrderCreatedEvent event = new OrderCreatedEvent(
                6L,
                "",  // Email vuota
                "ORD-EMPTY-111"
        );

        // When - Gestisce l'evento
        orderNotificationListener.handleNewOrder(event);

        // Then - Verifica fallback "Cliente"
        verify(lambdaNotificationService).sendOrderCreatedNotification(
                "",
                "ORD-EMPTY-111",
                "Cliente"  // Vero fallback quando email è vuota
        );
    }

    @Test
    void shouldUseFallbackForNullEmail() {
        // Given - Email null (caso edge)
        OrderCreatedEvent event = new OrderCreatedEvent(
                7L,
                null,  // Email null
                "ORD-NULL-222"
        );

        // When - Gestisce l'evento (non dovrebbe crashare)
        orderNotificationListener.handleNewOrder(event);

        // Then - Verifica fallback "Cliente"
        verify(lambdaNotificationService).sendOrderCreatedNotification(
                null,
                "ORD-NULL-222",
                "Cliente"  // Fallback per email null
        );
    }
}