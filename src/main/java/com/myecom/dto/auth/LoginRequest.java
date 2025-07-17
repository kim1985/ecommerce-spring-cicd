package com.myecom.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO per la richiesta di login
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    @NotBlank(message = "Email è obbligatoria")
    @Email(message = "Formato email non valido")
    private String email;

    @NotBlank(message = "Password è obbligatoria")
    private String password;
}
