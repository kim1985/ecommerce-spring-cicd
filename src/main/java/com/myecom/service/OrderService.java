package com.myecom.service;

import com.myecom.dto.order.CreateOrderRequest;
import com.myecom.dto.order.OrderResponse;
import com.myecom.exception.BusinessException;
import com.myecom.model.CartItem;
import com.myecom.model.User;
import com.myecom.repository.OrderRepository;
import com.myecom.repository.UserRepository;
import com.myecom.service.command.CreateOrderCommand;
import com.myecom.service.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * OrderService - ora delega la creazione ordini al Command
 * Questo è il pattern Command in azione: il service non sa COME creare l'ordine,
 * sa solo QUALE comando chiamare per farlo
 */

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final OrderMapper orderMapper;

    // Il Command - incapsula la logica di creazione ordini
    private final CreateOrderCommand createOrderCommand;

    /**
     * Crea ordine del carrello - delegato al Command Pattern
     * Stessa signature di prima, zero breaking changes
     */
    public OrderResponse createOrder(Long userId, CreateOrderRequest request) {

        // Command Pattern: delega tutto al comando
        return createOrderCommand
                .init(userId, request)  // Prepara il comando
                .execute();             // Esegue l'operazione

    }

    // Trova ordine per ID
    public Optional<OrderResponse> findById(Long orderId) {
        return orderRepository.findById(orderId)
                .map(orderMapper::toResponse);
    }

    // Lista ordini utente
    public List<OrderResponse> getUserOrders(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Utente non trovato"));

        return orderRepository.findByUserOrderByCreatedAtDesc(user).stream()
                .map(orderMapper::toResponse)
                .toList();
    }

    /**
     * Calcola il totale dell'ordine
     */
    private BigDecimal calculateTotalAmount(List<CartItem> cartItems) {
        return cartItems.stream()
                .map(item -> item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Conta quanti ordini ha fatto l'utente oggi (per prevenire spam)
     */
    private long countOrdersTodayForUser(Long userId) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new BusinessException("Utente non trovato"));

            LocalDateTime startOfDay = LocalDateTime.now()
                    .withHour(0).withMinute(0).withSecond(0).withNano(0);
            LocalDateTime endOfDay = LocalDateTime.now()
                    .withHour(23).withMinute(59).withSecond(59).withNano(999999999);

            // Usa il metodo che hai già nel repository
            return orderRepository.findUserOrdersInPeriod(user, startOfDay, endOfDay).size();

        } catch (Exception e) {
            // Se c'è un errore nel conteggio, lascia passare (non bloccare l'ordine per questo)
            System.err.println("Errore nel conteggio ordini giornalieri: " + e.getMessage());
            return 0;
        }
    }
}