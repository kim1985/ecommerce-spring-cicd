package com.myecom.exception;

/**
 * Eccezione per errori nelle regole di business.
 * Quando lanciata, indica che l'utente ha fatto qualcosa
 * che viola le regole dell'applicazione.
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}
