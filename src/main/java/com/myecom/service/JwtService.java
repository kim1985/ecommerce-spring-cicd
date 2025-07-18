package com.myecom.service;

import org.springframework.stereotype.Service;

/**
 * JWT Service semplificato - per ora ritorna token dummy
 * In futuro qui implementeremo la logica JWT completa
 */
@Service
public class JwtService {

    // Per ora generiamo token semplici
    public String generateToken(String email) {
        return "dummy-token-" + email + "-" + System.currentTimeMillis();
    }

    // Valida token (semplificato)
    public boolean isValidToken(String token) {
        return token != null && token.startsWith("dummy-token-");
    }

    // Estrae email dal token
    public String getEmailFromToken(String token) {
        if (token == null || !token.startsWith("dummy-token-")) {
            return null;
        }

        // Estrae email dal token dummy
        String[] parts = token.split("-");
        return parts.length > 2 ? parts[2] : null;
    }
}