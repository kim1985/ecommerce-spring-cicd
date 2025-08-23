package com.myecom.service.validation;

import com.myecom.exception.BusinessException;
import com.myecom.model.CartItem;

import java.util.List;

/**
 * Strategy interface per la validazione degli ordini.
 *
 * Ogni implementazione rappresenta una strategia di validazione specifica
 * che può essere abilitata/disabilitata indipendentemente dalle altre.
 *
 * Questo pattern permette di:
 * - Aggiungere nuove validazioni senza modificare codice esistente (OCP)
 * - Testare ogni validazione singolarmente
 * - Configurare dinamicamente quali validazioni abilitare
 *
 * */


 public interface OrderValidator {

    /**
     * Valida un ordine secondo questa strategia specifica.
     */
    void validate(Long userId, List<CartItem> cartItems) throws BusinessException;

    /**
     * Nome identificativo della validazione per logging e diagnostica.
     *
     * @return nome della strategia di validazione (es: "EmptyCart", "PriceLimit")
     */
    String getName();

    /**
     * Ordine di esecuzione della validazione.
     * Le validazioni vengono eseguite in ordine crescente.
     *
     * @return ordine di esecuzione (default: 100)
     */
    default int getOrder() {
        return 100;
    }

    /**
     * Indica se questa validazione è abilitata.
     * Può essere utilizzato per abilitare/disabilitare validazioni
     * dinamicamente o via configurazione.
     *
     * @return true se la validazione è abilitata (default: true)
     */
    default boolean isEnabled() {
        return true;
    }
}
