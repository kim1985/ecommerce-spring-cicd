package com.myecom.repository;

import com.myecom.model.Order;
import com.myecom.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    // Trova ordine per numero ordine (per tracking)
    Optional<Order> findByOrderNumber(String orderNumber);

    // Trova tutti gli ordini di un utente
    List<Order> findByUserOrderByCreatedAtDesc(User user);

    // Trova ordini di un utente con paginazione
    Page<Order> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    // Trova ordini per stato
    List<Order> findByStatus(Order.OrderStatus status);

    // Trova ordini per stato con paginazione
    Page<Order> findByStatus(Order.OrderStatus status, Pageable pageable);

    // Trova ordini di un utente per stato
    List<Order> findByUserAndStatus(User user, Order.OrderStatus status);

    // Trova ordini in un periodo di tempo
    List<Order> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    // Trova ordini in un periodo con paginazione
    Page<Order> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);

    // Trova ordini superiori a un importo minimo
    List<Order> findByTotalAmountGreaterThanEqual(BigDecimal minAmount);

    // Query per calcolare il totale vendite in un periodo
    @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.createdAt BETWEEN :start AND :end " +
            "AND o.status NOT IN ('CANCELLED')")
    BigDecimal calculateTotalSalesInPeriod(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // Query per contare ordini per stato
    @Query("SELECT o.status, COUNT(o) FROM Order o GROUP BY o.status")
    List<Object[]> countOrdersByStatus();

    // Query per trovare ordini recenti (ultime 24 ore)
    @Query("SELECT o FROM Order o WHERE o.createdAt >= :since ORDER BY o.createdAt DESC")
    List<Order> findRecentOrders(@Param("since") LocalDateTime since);

    // Query per trovare top clienti per importo speso
    @Query("SELECT o.user, SUM(o.totalAmount) as totalSpent FROM Order o " +
            "WHERE o.status NOT IN ('CANCELLED') GROUP BY o.user ORDER BY totalSpent DESC")
    List<Object[]> findTopCustomersBySpent(Pageable pageable);

    // Query per trovare ordini da processare (confermati ma non ancora processati)
    @Query("SELECT o FROM Order o WHERE o.status IN ('CONFIRMED', 'PENDING') ORDER BY o.createdAt ASC")
    List<Order> findOrdersToProcess();

    // Query per statistiche mensili
    @Query("SELECT YEAR(o.createdAt), MONTH(o.createdAt), COUNT(o), SUM(o.totalAmount) " +
            "FROM Order o WHERE o.status NOT IN ('CANCELLED') " +
            "GROUP BY YEAR(o.createdAt), MONTH(o.createdAt) ORDER BY YEAR(o.createdAt), MONTH(o.createdAt)")
    List<Object[]> getMonthlySalesStats();

    // Query per trovare ordini di un utente in un periodo specifico
    @Query("SELECT o FROM Order o WHERE o.user = :user AND o.createdAt BETWEEN :start AND :end " +
            "ORDER BY o.createdAt DESC")
    List<Order> findUserOrdersInPeriod(@Param("user") User user, @Param("start") LocalDateTime start,
                                       @Param("end") LocalDateTime end);
}
