package com.myecom.dto.cart;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

// DTO per risposta carrello
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartResponse {

    private Long id;
    private List<CartItemResponse> items;
    private BigDecimal totalAmount;
    private int totalItems;
    private String updatedAt;
}
