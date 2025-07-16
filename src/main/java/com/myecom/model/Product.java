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
@Table(name = "products")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Nome del prodotto
    @Column(nullable = false)
    private String name;

    // Descrizione dettagliata del prodotto
    @Column(length = 1000)
    private String description;

    // Prezzo unitario con precisione per valuta
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    // Quantità disponibile in magazzino
    @Column(nullable = false)
    private Integer stockQuantity;

    // URL dell'immagine del prodotto
    @Column
    private String imageUrl;

    // Marca del prodotto
    @Column
    private String brand;

    // Flag per prodotti attivi/disattivi
    @Column(nullable = false)
    private boolean active = true;

    // Timestamp di creazione e aggiornamento
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column
    private LocalDateTime updatedAt;

    // Categoria di appartenenza del prodotto
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    // Relazione con gli ordini che contengono questo prodotto
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<OrderItem> orderItems;

    // Relazione con i carrelli che contengono questo prodotto
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<CartItem> cartItems;

    // Inizializza automaticamente createdAt prima del persist
    @PrePersist
    private void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    // Aggiorna automaticamente il timestamp di modifica
    @PreUpdate
    private void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Verifica se il prodotto è disponibile
    public boolean isInStock() {
        return stockQuantity > 0;
    }

    // Diminuisce la quantità in stock (per gli ordini)
    public void decreaseStock(int quantity) {
        if (stockQuantity >= quantity) {
            stockQuantity -= quantity;
        } else {
            throw new IllegalArgumentException("Insufficient stock");
        }
    }

    // Aumenta la quantità in stock (per i resi o nuovi arrivi)
    public void increaseStock(int quantity) {
        stockQuantity += quantity;
    }
}
