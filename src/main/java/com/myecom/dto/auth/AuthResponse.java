package com.myecom.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO per la risposta di autenticazione (login/register)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    // Token JWT per l'autenticazione
    private String token;

    // Tipo di token (sempre "Bearer")
    private String type = "Bearer";

    // Dati dell'utente autenticato
    private UserResponse user;
}
