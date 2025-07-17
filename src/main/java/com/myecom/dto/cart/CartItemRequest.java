package com.myecom.dto.cart;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO per aggiungere/aggiornare articolo nel carrello
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItemRequest {

    @NotNull(message = "ID prodotto è obbligatorio")
    private Long productId;

    @NotNull(message = "Quantità è obbligatoria")
    @Min(value = 1, message = "Quantità deve essere almeno 1")
    private Integer quantity;
}
