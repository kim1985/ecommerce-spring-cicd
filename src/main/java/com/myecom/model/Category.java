package com.myecom.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "categories")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Nome della categoria (deve essere unico)
    @Column(nullable = false, unique = true)
    private String name;

    // Descrizione della categoria
    @Column(length = 500)
    private String description;

    // Flag per categorie attive/disattive
    @Column(nullable = false)
    private boolean active = true;

    // Timestamp di creazione e aggiornamento
    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime updatedAt;

    // Prodotti appartenenti a questa categoria
    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Product> products;

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
}
