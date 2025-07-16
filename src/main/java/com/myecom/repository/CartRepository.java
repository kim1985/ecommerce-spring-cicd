package com.myecom.repository;

import com.myecom.model.Cart;
import com.myecom.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

// Repository per gestire i carrelli degli utenti
@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {

    // Trova il carrello di un utente specifico
    Optional<Cart> findByUser(User user);

    // Trova carrelli modificati di recente (per pulizia periodica)
    @Query("SELECT c FROM Cart c WHERE c.updatedAt IS NOT NULL ORDER BY c.updatedAt DESC")
    List<Cart> findRecentlyUpdatedCarts();

    // Conta il numero di articoli in tutti i carrelli
    @Query("SELECT COUNT(ci) FROM Cart c JOIN c.cartItems ci")
    long countTotalItemsInAllCarts();
}
