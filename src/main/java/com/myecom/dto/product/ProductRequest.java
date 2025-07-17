package com.myecom.dto.product;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

// DTO per la creazione/aggiornamento di un prodotto
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductRequest {

    @NotBlank(message = "Nome prodotto è obbligatorio")
    private String name;

    private String description;

    @NotNull(message = "Prezzo è obbligatorio")
    @DecimalMin(value = "0.01", message = "Prezzo deve essere maggiore di 0")
    private BigDecimal price;

    @NotNull(message = "Quantità stock è obbligatoria")
    @Min(value = 0, message = "Quantità non può essere negativa")
    private Integer stockQuantity;

    private String imageUrl;
    private String brand;

    @NotNull(message = "Categoria è obbligatoria")
    private Long categoryId;

    private Boolean active = true;
}
