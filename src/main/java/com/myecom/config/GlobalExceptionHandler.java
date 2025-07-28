package com.myecom.config;

import com.myecom.dto.common.ApiResponse;
import com.myecom.exception.BusinessException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Gestisce tutte le eccezioni dell'applicazione in un posto solo.
 * Questo è un pattern architetturale importante: centralizzare la gestione errori.
 */
@ControllerAdvice
@Profile("!test")
public class GlobalExceptionHandler {

    /**
     * Gestisce errori nelle regole di business
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
    }

    /**
     * Gestisce errori di validazione dei parametri
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.error("Richiesta non valida: " + e.getMessage()));
    }

    /**
     * Gestisce errori di validazione Spring (@Valid)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationErrors(MethodArgumentNotValidException e) {
        List<String> errors = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.toList());

        return ResponseEntity.badRequest()
                .body(ApiResponse.error("Errori di validazione", errors));
    }

    /**
     * Gestisce tutti gli altri errori non previsti
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception e) {
        // In produzione, logga l'errore completo ma non mostrare dettagli all'utente
        System.err.println("Errore non gestito: " + e.getMessage());
        e.printStackTrace();

        return ResponseEntity.internalServerError()
                .body(ApiResponse.error("Errore interno del server"));
    }
}