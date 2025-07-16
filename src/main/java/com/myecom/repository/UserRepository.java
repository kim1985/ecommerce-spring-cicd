package com.myecom.repository;

import com.myecom.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Query derivata per trovare utente tramite email (usato per login)
    Optional<User> findByEmail(String email);

    // Verifica se esiste già un utente con questa email
    boolean existsByEmail(String email);

    // Trova utenti per ruolo
    List<User> findByRole(User.Role role);

    // Trova utenti attivi/disattivi
    List<User> findByEnabled(boolean enabled);

    // Trova utenti registrati in un periodo specifico
    List<User> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    // Trova utenti per città (per statistiche geografiche)
    List<User> findByCity(String city);

    // Query custom per trovare utenti con ordini
    @Query("SELECT DISTINCT u FROM User u JOIN u.orders o WHERE o.createdAt >= :since")
    List<User> findUsersWithOrdersSince(@Param("since") LocalDateTime since);

    // Query per contare utenti attivi
    @Query("SELECT COUNT(u) FROM User u WHERE u.enabled = true")
    long countActiveUsers();

    // Query per trovare utenti amministratori
    @Query("SELECT u FROM User u WHERE u.role = 'ADMIN' AND u.enabled = true")
    List<User> findActiveAdmins();

    // Query per cercare utenti per nome o cognome (case insensitive)
    @Query("SELECT u FROM User u WHERE LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :search, '%'))")
    List<User> searchUsersByName(@Param("search") String search);
}
