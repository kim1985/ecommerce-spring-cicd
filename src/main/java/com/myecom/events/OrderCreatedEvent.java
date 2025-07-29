package com.myecom.events;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Evento: "È stato creato un ordine"
 *
 * Contiene le info essenziali che potrebbero servire
 * a chi vuole reagire a questo evento.
 */
@Getter
@AllArgsConstructor
public class OrderCreatedEvent {
    private final Long orderId;
    private final String userEmail;
    private final String orderNumber;
}
