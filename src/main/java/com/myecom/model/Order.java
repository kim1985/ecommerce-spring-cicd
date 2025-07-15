package com.myecom.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Numero ordine univoco per tracking
    @Column(nullable = false, unique = true)
    private String orderNumber;

    // Utente che ha effettuato l'ordine
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Stato dell'ordine (PENDING, CONFIRMED, etc.)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status = OrderStatus.PENDING;

    // Importo totale dell'ordine
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    // Indirizzo di spedizione per questo ordine
    @Column(nullable = false)
    private String shippingAddress;

    // Note aggiuntive per l'ordine
    @Column
    private String notes;

    // Timestamp di creazione e aggiornamento
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column
    private LocalDateTime updatedAt;

    // Prodotti inclusi nell'ordine
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<OrderItem> orderItems;

    // Aggiorna automaticamente il timestamp di modifica
    @PreUpdate
    private void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Stati possibili per un ordine
    public enum OrderStatus {
        PENDING, CONFIRMED, PROCESSING, SHIPPED, DELIVERED, CANCELLED
    }
}
