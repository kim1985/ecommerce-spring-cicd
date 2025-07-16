package com.myecom.repository;

import com.myecom.model.Product;
import com.myecom.model.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    // Trova prodotti attivi (per il catalogo pubblico)
    List<Product> findByActiveTrue();

    // Trova prodotti attivi con paginazione
    Page<Product> findByActiveTrue(Pageable pageable);

    // Trova prodotti per categoria
    List<Product> findByCategoryAndActiveTrue(Category category);

    // Trova prodotti per categoria con paginazione
    Page<Product> findByCategoryAndActiveTrue(Category category, Pageable pageable);

    // Trova prodotti per marca
    List<Product> findByBrandAndActiveTrue(String brand);

    // Trova prodotti disponibili in magazzino
    List<Product> findByActiveTrueAndStockQuantityGreaterThan(int minStock);

    // Trova prodotti esauriti
    List<Product> findByActiveTrueAndStockQuantityLessThanEqual(int maxStock);

    // Ricerca per nome prodotto (case insensitive)
    @Query("SELECT p FROM Product p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%')) AND p.active = true")
    List<Product> searchByName(@Param("name") String name);

    // Ricerca per nome prodotto con paginazione
    @Query("SELECT p FROM Product p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%')) AND p.active = true")
    Page<Product> searchByName(@Param("name") String name, Pageable pageable);

    // Ricerca full-text per nome e descrizione
    @Query("SELECT p FROM Product p WHERE (LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%'))) AND p.active = true")
    Page<Product> searchByNameOrDescription(@Param("search") String search, Pageable pageable);

    // Trova prodotti in una fascia di prezzo
    List<Product> findByActiveTrueAndPriceBetween(BigDecimal minPrice, BigDecimal maxPrice);

    // Trova prodotti in una fascia di prezzo con paginazione
    Page<Product> findByActiveTrueAndPriceBetween(BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);

    // Query per trovare prodotti più venduti
    @Query("SELECT p FROM Product p JOIN p.orderItems oi GROUP BY p ORDER BY SUM(oi.quantity) DESC")
    List<Product> findBestSellingProducts(Pageable pageable);

    // Query per trovare prodotti con stock basso (per alert amministratori)
    @Query("SELECT p FROM Product p WHERE p.active = true AND p.stockQuantity <= :threshold")
    List<Product> findLowStockProducts(@Param("threshold") int threshold);

    // Query per statistiche: conteggio prodotti per categoria
    @Query("SELECT p.category.name, COUNT(p) FROM Product p WHERE p.active = true GROUP BY p.category")
    List<Object[]> countProductsByCategory();

    // Query per trovare prodotti correlati (stessa categoria, escluso quello corrente)
    @Query("SELECT p FROM Product p WHERE p.category = :category AND p.id != :excludeId AND p.active = true")
    List<Product> findRelatedProducts(@Param("category") Category category, @Param("excludeId") Long excludeId, Pageable pageable);
}
