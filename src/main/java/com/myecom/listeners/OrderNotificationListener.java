package com.myecom.listeners;

import com.myecom.events.OrderCreatedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Ascolta eventi di ordini e invia notifiche.
 *
 * Architettura: OrderService → pubblica evento → questo listener reagisce
 * Vantaggio: OrderService non sa di questo listener (disaccoppiamento)
 */
@Component
public class OrderNotificationListener {

    /**
     * Quando viene creato un ordine → stampa notifica
     *
     * In futuro potresti sostituire System.out.println con:
     * - Invio email vero
     * - Push notification
     * - Slack/Discord webhook
     * - Aggiornamento dashboard
     */
    @EventListener
    public void handleNewOrder(OrderCreatedEvent event) {
        System.out.println();
        System.out.println("NUOVO ORDINE RICEVUTO!");
        System.out.println("Cliente: " + event.getUserEmail());
        System.out.println("Numero: " + event.getOrderNumber());
        System.out.println("ID: " + event.getOrderId());
        System.out.println();
    }
}