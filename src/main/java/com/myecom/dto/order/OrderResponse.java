package com.myecom.dto.order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

// DTO per risposta ordine (usato sia per dettagli che per liste)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {

    private Long id;
    private String orderNumber;
    private String status;
    private BigDecimal totalAmount;
    private String shippingAddress;
    private String notes;
    private String createdAt;
    private List<OrderItemResponse> items;
}
