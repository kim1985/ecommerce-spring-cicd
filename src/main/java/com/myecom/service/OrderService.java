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
import com.myecom.service.validation.OrderValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationEventPublisher;
import com.myecom.events.OrderCreatedEvent;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
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

    // Il "megafono" per annunciare eventi
    private final ApplicationEventPublisher eventPublisher;

    // ========== STRATEGY PATTERN IMPLEMENTATION ==========
    // Spring inietta automaticamente TUTTE le implementazioni di OrderValidator
    private final List<OrderValidator> validators;

    // Crea ordine dal carrello
    @Transactional
    public OrderResponse createOrder(Long userId, CreateOrderRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Utente non trovato"));

        Cart cart = cartRepository.findByUser(user)
                .orElseThrow(() -> new IllegalArgumentException("Carrello vuoto"));

        List<CartItem> cartItems = cartItemRepository.findByCart(cart);

        // ========== STRATEGY PATTERN IN ACTION ==========
        runValidations(userId, cartItems); // viene fatta qui la validazione

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

        // Annuncia "Ordine creato!" a tutti gli interessati
        // Spring troverà automaticamente tutti i @EventListener che ascoltano OrderCreatedEvent
        eventPublisher.publishEvent(new OrderCreatedEvent(
                savedOrder.getId(),
                user.getEmail(),
                savedOrder.getOrderNumber()
        ));

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

    // ========== STRATEGY PATTERN IMPLEMENTATION ==========
    /**
     * Esegue tutte le strategie di validazione attive.
     *
     * QUESTO È IL CUORE DEL PATTERN:
     * - Ordina le strategie per ordine di esecuzione
     * - Esegue ogni strategia indipendentemente
     * - Se una strategia fallisce, ferma tutto
     * - NON devi mai modificare questo metodo!
     *
     * Per aggiungere nuove validazioni: crea solo nuove classi @Component
     * che implementano OrderValidator. Spring le troverà automaticamente!
     */
    private void runValidations(Long userId, List<CartItem> cartItems) {
        // Filtra solo validazioni abilitate e ordina per priorità
        List<OrderValidator> enabledValidators = validators.stream()
                .filter(OrderValidator::isEnabled)
                .sorted(Comparator.comparing(OrderValidator::getOrder))
                .toList();

        if (enabledValidators.isEmpty()) {
            return;
        }

        // Esegui ogni strategia in ordine
        for (OrderValidator validator : enabledValidators) {
            try {

                validator.validate(userId, cartItems);

            } catch (BusinessException e) {
                throw e; // Ferma tutto se una validazione fallisce
            } catch (Exception e) {
                throw new BusinessException(
                        "Errore interno durante la validazione dell'ordine: " + validator.getName());
            }
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