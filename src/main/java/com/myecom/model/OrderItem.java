package com.myecom.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Ordine di appartenenza
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    // Prodotto ordinato
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    // Quantità ordinata
    @Column(nullable = false)
    private Integer quantity;

    // Prezzo unitario al momento dell'ordine
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    // Prezzo totale per questo item (unitPrice * quantity)
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPrice;

    // Calcola automaticamente il prezzo totale
    @PrePersist
    private void calculateTotalPrice() {
        if (unitPrice != null && quantity != null) {
            totalPrice = unitPrice.multiply(BigDecimal.valueOf(quantity));
        }
    }
}
