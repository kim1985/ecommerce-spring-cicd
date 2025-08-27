package com.myecom.service.command;

// Command Pattern - Incapsula un'operazione in un oggetto
// Vantaggi: logica isolata, rollback supportato, riusabile

/**
 * Command interface - definisce cosa può fare ogni comando
 */
public interface OrderCommand<T> {
    /**
     * Esegue l'operazione
     */
    T execute();

    /**
     * Annulla l'operazione se qualcosa va storto
     */
    default void rollback() {
        // Default: niente da rollback
    }
}
