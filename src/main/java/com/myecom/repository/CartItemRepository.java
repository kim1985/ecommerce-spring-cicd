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


// Repository per gestire gli articoli nei carrelli
@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    // Trova tutti gli articoli in un carrello specifico
    List<CartItem> findByCart(Cart cart);

    // Trova un articolo specifico in un carrello
    Optional<CartItem> findByCartAndProduct(Cart cart, Product product);

    // Trova articoli per prodotto (per vedere in quanti carrelli è presente)
    List<CartItem> findByProduct(Product product);

    // Rimuove tutti gli articoli di un carrello
    void deleteByCart(Cart cart);

    // Rimuove tutti gli articoli di un prodotto specifico (quando viene eliminato)
    void deleteByProduct(Product product);

    // Query per trovare prodotti più aggiunti ai carrelli
    @Query("SELECT ci.product, SUM(ci.quantity) as totalQuantity FROM CartItem ci " +
            "GROUP BY ci.product ORDER BY totalQuantity DESC")
    List<Object[]> findMostAddedToCartProducts();

    // Query per trovare carrelli abbandonati (non modificati da X giorni)
    @Query("SELECT ci FROM CartItem ci WHERE ci.cart.updatedAt < :cutoffDate OR " +
            "(ci.cart.updatedAt IS NULL AND ci.cart.createdAt < :cutoffDate)")
    List<CartItem> findAbandonedCartItems(@Param("cutoffDate") java.time.LocalDateTime cutoffDate);
}
