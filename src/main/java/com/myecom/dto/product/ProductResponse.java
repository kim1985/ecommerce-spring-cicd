package com.myecom.dto.product;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

// DTO per la risposta con dettagli prodotto completi
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {

    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer stockQuantity;
    private String imageUrl;
    private String brand;
    private boolean active;
    private boolean inStock; // Calcolato: stockQuantity > 0
    private String createdAt;
    private String updatedAt;

    // Informazioni categoria
    private CategoryResponse category;
}
