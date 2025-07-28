package com.myecom.service;

import com.myecom.dto.order.CreateOrderRequest;
import com.myecom.dto.order.OrderItemResponse;
import com.myecom.dto.order.OrderResponse;
import com.myecom.exception.BusinessException;
import com.myecom.model.*;
import com.myecom.repository.CartItemRepository;
import com.myecom.repository.CartRepository;
import com.myecom.repository.OrderRepository;
import com.myecom.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final CartService cartService;

    // Crea ordine dal carrello
    @Transactional
    public OrderResponse createOrder(Long userId, CreateOrderRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Utente non trovato"));

        Cart cart = cartRepository.findByUser(user)
                .orElseThrow(() -> new IllegalArgumentException("Carrello vuoto"));

        List<CartItem> cartItems = cartItemRepository.findByCart(cart);

        validateOrderCreation(userId, cartItems); // viene fatta qui la validazione

        // Calcola totale
        BigDecimal totalAmount = cartItems.stream()
                .map(CartItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Crea ordine
        Order order = Order.builder()
                .orderNumber(generateOrderNumber())
                .user(user)
                .status(Order.OrderStatus.PENDING)
                .totalAmount(totalAmount)
                .shippingAddress(request.getShippingAddress())
                .notes(request.getNotes())
                .build();

        Order savedOrder = orderRepository.save(order);

        // Crea order items e aggiorna stock
        List<OrderItem> orderItems = cartItems.stream()
                .map(cartItem -> {
                    // Aggiorna stock
                    Product product = cartItem.getProduct();
                    product.decreaseStock(cartItem.getQuantity());

                    return OrderItem.builder()
                            .order(savedOrder)
                            .product(product)
                            .quantity(cartItem.getQuantity())
                            .unitPrice(product.getPrice())
                            .build();
                })
                .toList();

        // Svuota carrello
        cartService.clearCart(userId);

        return convertToResponse(savedOrder, orderItems);
    }

    // Trova ordine per ID
    public Optional<OrderResponse> findById(Long orderId) {
        return orderRepository.findById(orderId)
                .map(this::convertToResponse);
    }

    // Lista ordini utente
    public List<OrderResponse> getUserOrders(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Utente non trovato"));

        return orderRepository.findByUserOrderByCreatedAtDesc(user).stream()
                .map(this::convertToResponse)
                .toList();
    }

    // Genera numero ordine
    private String generateOrderNumber() {
        return "ORD-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    // Converte Order a OrderResponse
    private OrderResponse convertToResponse(Order order) {
        return convertToResponse(order, order.getOrderItems());
    }

    // Converte Order a OrderResponse con OrderItems
    private OrderResponse convertToResponse(Order order, List<OrderItem> orderItems) {
        List<OrderItemResponse> itemResponses = orderItems.stream()
                .map(this::convertToItemResponse)
                .toList();

        return OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus().name())
                .totalAmount(order.getTotalAmount())
                .shippingAddress(order.getShippingAddress())
                .notes(order.getNotes())
                .createdAt(order.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .items(itemResponses)
                .build();
    }

    // Converte OrderItem a OrderItemResponse
    private OrderItemResponse convertToItemResponse(OrderItem item) {
        return OrderItemResponse.builder()
                .id(item.getId())
                .productId(item.getProduct().getId())
                .productName(item.getProduct().getName())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .totalPrice(item.getTotalPrice())
                .build();
    }

    /**
     * Valida che l'ordine possa essere creato secondo le regole di business.
     * Questo è il cuore delle regole dell'e-commerce.
     */
    private void validateOrderCreation(Long userId, List<CartItem> cartItems) {
        // Regola 1: Carrello non può essere vuoto
        if (cartItems.isEmpty()) {
            throw new BusinessException("Impossibile creare ordine: il carrello è vuoto");
        }

        // Regola 2: Calcola totale e controlla limite massimo
        BigDecimal totalAmount = calculateTotalAmount(cartItems);
        BigDecimal maxOrderAmount = new BigDecimal("5000.00");

        if (totalAmount.compareTo(maxOrderAmount) > 0) {
            throw new BusinessException(
                    String.format("Ordine troppo grande (€%.2f). Il limite massimo è €%.2f. " +
                                    "Per ordini superiori contatta il supporto clienti.",
                            totalAmount, maxOrderAmount)
            );
        }

        // Regola 3: Controlla disponibilità di ogni prodotto
        for (CartItem item : cartItems) {
            Product product = item.getProduct();

            // Prodotto deve essere attivo
            if (!product.isActive()) {
                throw new BusinessException(
                        String.format("Il prodotto '%s' non è più disponibile", product.getName())
                );
            }

            // Stock sufficiente
            if (product.getStockQuantity() < item.getQuantity()) {
                throw new BusinessException(
                        String.format("Prodotto '%s': richiesti %d pezzi ma disponibili solo %d",
                                product.getName(), item.getQuantity(), product.getStockQuantity())
                );
            }

            // Quantità ragionevole (max 99 per prodotto)
            if (item.getQuantity() > 99) {
                throw new BusinessException(
                        String.format("Quantità troppo alta per '%s'. Massimo 99 pezzi per prodotto",
                                product.getName())
                );
            }
        }

        // Regola 4: Limite ordini giornalieri per utente (anti-spam)
        long ordersToday = countOrdersTodayForUser(userId);
        if (ordersToday >= 10) {
            throw new BusinessException(
                    "Raggiunto il limite massimo di 10 ordini al giorno. " +
                            "Se hai necessità particolari, contatta il supporto clienti."
            );
        }
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