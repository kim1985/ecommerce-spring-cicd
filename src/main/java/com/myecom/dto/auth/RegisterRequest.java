package com.myecom.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
// DTO per la richiesta di registrazione
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "Email è obbligatoria")
    @Email(message = "Formato email non valido")
    private String email;

    @NotBlank(message = "Password è obbligatoria")
    @Size(min = 8, message = "Password deve essere di almeno 8 caratteri")
    private String password;

    @NotBlank(message = "Nome è obbligatorio")
    private String firstName;

    @NotBlank(message = "Cognome è obbligatorio")
    private String lastName;

    private String phone;
    private String address;
    private String city;
    private String zipCode;
}
