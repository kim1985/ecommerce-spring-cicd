package com.myecom.repository;

import com.myecom.model.Cart;
import com.myecom.model.CartItem;
import com.myecom.model.Category;
import com.myecom.model.Product;
import com.myecom.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
// Repository per gestire le categorie prodotti
@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    // Trova categoria per nome
    Optional<Category> findByName(String name);

    // Trova categorie attive
    List<Category> findByActiveTrue();

    // Trova categorie per nome (case insensitive)
    @Query("SELECT c FROM Category c WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Category> searchByName(@Param("name") String name);

    // Verifica se esiste una categoria con questo nome
    boolean existsByName(String name);

    // Query per trovare categorie con prodotti
    @Query("SELECT DISTINCT c FROM Category c JOIN c.products p WHERE p.active = true")
    List<Category> findCategoriesWithActiveProducts();

    // Query per contare prodotti per categoria
    @Query("SELECT c, COUNT(p) FROM Category c LEFT JOIN c.products p WHERE p.active = true GROUP BY c")
    List<Object[]> countProductsByCategory();

    // Query per trovare categorie vuote (senza prodotti attivi)
    @Query("SELECT c FROM Category c WHERE c.id NOT IN " +
            "(SELECT DISTINCT p.category.id FROM Product p WHERE p.active = true)")
    List<Category> findEmptyCategories();
}
