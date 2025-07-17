package com.myecom.dto.cart;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

// DTO per articolo nel carrello
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItemResponse {

    private Long id;
    private Long productId;
    private String productName;
    private BigDecimal unitPrice;
    private String productImageUrl;
    private Integer quantity;
    private BigDecimal totalPrice;
    private boolean productInStock;
}
