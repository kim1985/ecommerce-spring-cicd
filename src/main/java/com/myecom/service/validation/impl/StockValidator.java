package com.myecom.service.validation.impl;

import com.myecom.model.CartItem;
import com.myecom.model.Product;
import com.myecom.service.validation.OrderValidator;
import com.myecom.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Validatore per la disponibilità di stock dei prodotti.
 *
 * Verifica che:
 * - I prodotti siano ancora attivi
 * - Ci sia stock sufficiente per la quantità richiesta
 * - La quantità richiesta sia ragionevole (max 99 per prodotto)
 *
 * Ordine di esecuzione: 20
 */
@Component
@Slf4j
public class StockValidator implements OrderValidator {

    private static final int MAX_QUANTITY_PER_PRODUCT = 99;

    @Override
    public void validate(Long userId, List<CartItem> cartItems) throws BusinessException {
        log.debug("Validating stock for user: {} with {} items", userId, cartItems.size());

        for (CartItem item : cartItems) {
            Product product = item.getProduct();

            log.debug("Checking stock for product: {} (ID: {}), requested: {}, available: {}",
                    product.getName(), product.getId(), item.getQuantity(), product.getStockQuantity());

            // Verifica che il prodotto sia attivo
            if (!product.isActive()) {
                log.warn("Inactive product validation failed for user: {}, product: {}",
                        userId, product.getName());
                throw new BusinessException(
                        String.format("Il prodotto '%s' non è più disponibile", product.getName())
                );
            }

            // Verifica stock disponibile
            if (product.getStockQuantity() < item.getQuantity()) {
                log.warn("Insufficient stock validation failed for user: {}, product: {}, requested: {}, available: {}",
                        userId, product.getName(), item.getQuantity(), product.getStockQuantity());
                throw new BusinessException(
                        String.format("Prodotto '%s': richiesti %d pezzi ma disponibili solo %d",
                                product.getName(), item.getQuantity(), product.getStockQuantity())
                );
            }

            // Verifica quantità ragionevole (prevenzione errori utente)
            if (item.getQuantity() > MAX_QUANTITY_PER_PRODUCT) {
                log.warn("Excessive quantity validation failed for user: {}, product: {}, quantity: {}",
                        userId, product.getName(), item.getQuantity());
                throw new BusinessException(
                        String.format("Quantità troppo alta per '%s'. Massimo %d pezzi per prodotto",
                                product.getName(), MAX_QUANTITY_PER_PRODUCT)
                );
            }
        }

        log.debug("Stock validation passed for user: {} - all products available", userId);
    }

    @Override
    public String getName() {
        return "Stock";
    }

    @Override
    public int getOrder() {
        return 20; // Dopo aver controllato carrello vuoto e prezzo
    }
}