package com.myecom.service.validation.impl;

import com.myecom.model.CartItem;
import com.myecom.service.validation.OrderValidator;
import com.myecom.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Validatore per il limite massimo del valore dell'ordine.
 *
 * Previene ordini troppo costosi che potrebbero indicare errori utente
 * o richiedere approvazioni speciali per importi elevati.
 *
 * Il limite è configurabile tramite property: order.max-amount
 *
 * Ordine di esecuzione: 10
 */
@Component
@Slf4j
public class PriceLimitValidator implements OrderValidator {

    @Value("${order.max-amount:5000.00}")
    private BigDecimal maxOrderAmount;

    @Override
    public void validate(Long userId, List<CartItem> cartItems) throws BusinessException {
        log.debug("Validating price limit for user: {}", userId);

        BigDecimal totalAmount = calculateTotalAmount(cartItems);

        log.debug("Order total amount: €{} for user: {}, limit: €{}",
                totalAmount, userId, maxOrderAmount);

        if (totalAmount.compareTo(maxOrderAmount) > 0) {
            log.warn("Price limit validation failed for user: {}. Amount: €{}, Limit: €{}",
                    userId, totalAmount, maxOrderAmount);

            throw new BusinessException(
                    String.format("Ordine troppo grande (€%.2f). Il limite massimo è €%.2f. " +
                                    "Per ordini superiori contatta il supporto clienti.",
                            totalAmount, maxOrderAmount)
            );
        }

        log.debug("Price limit validation passed for user: {} with total €{}", userId, totalAmount);
    }

    @Override
    public String getName() {
        return "PriceLimit";
    }

    @Override
    public int getOrder() {
        return 10;
    }

    /**
     * Calcola l'importo totale dell'ordine.
     */
    private BigDecimal calculateTotalAmount(List<CartItem> cartItems) {
        return cartItems.stream()
                .map(item -> item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}