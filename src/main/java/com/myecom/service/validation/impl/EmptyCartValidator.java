package com.myecom.service.validation.impl;

import com.myecom.model.CartItem;
import com.myecom.service.validation.OrderValidator;
import com.myecom.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Validatore per carrelli vuoti.
 *
 * Questa strategia impedisce la creazione di ordini quando il carrello è vuoto,
 * che sarebbe uno scenario di business non valido.
 *
 * Ordine di esecuzione: 1 (prima validazione - ha senso controllare prima se c'è qualcosa da ordinare)
 */
@Component
@Slf4j
public class EmptyCartValidator implements OrderValidator {

    @Override
    public void validate(Long userId, List<CartItem> cartItems) throws BusinessException {
        log.debug("Validating empty cart for user: {}", userId);

        if (cartItems == null || cartItems.isEmpty()) {
            log.warn("Empty cart validation failed for user: {}", userId);
            throw new BusinessException("Impossibile creare ordine: il carrello è vuoto");
        }

        log.debug("Empty cart validation passed for user: {} with {} items", userId, cartItems.size());
    }

    @Override
    public String getName() {
        return "EmptyCart";
    }

    @Override
    public int getOrder() {
        return 1; // Prima validazione - controlla se c'è qualcosa da validare
    }
}
