package com.myecom.dto.auth;

import com.myecom.dto.product.CategoryResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

// DTO per la risposta con dati utente (senza password)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private String address;
    private String city;
    private String zipCode;
    private String role;
    private boolean enabled;
    private String createdAt;
}
