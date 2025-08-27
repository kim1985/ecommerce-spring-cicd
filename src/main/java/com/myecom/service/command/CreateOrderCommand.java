/**
 * Comando per creare ordini - incapsula tutta la logica di creazione
 * Prima era tutto nel OrderService, ora è isolato qui per essere più testabile
 */
package com.myecom.service.command;

import com.myecom.dto.order.CreateOrderRequest;
import com.myecom.dto.order.OrderResponse;
import com.myecom.events.OrderCreatedEvent;
import com.myecom.exception.BusinessException;
import com.myecom.model.Cart;
import com.myecom.model.CartItem;
import com.myecom.model.Order;
import com.myecom.model.OrderItem;
import com.myecom.model.Product;
import com.myecom.model.User;
import com.myecom.repository.CartItemRepository;
import com.myecom.repository.CartRepository;
import com.myecom.repository.OrderRepository;
import com.myecom.repository.UserRepository;
import com.myecom.service.CartService;
import com.myecom.service.mapper.OrderMapper;
import com.myecom.service.validation.OrderValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class CreateOrderCommand implements OrderCommand<OrderResponse>{

    // Dipendenze necessarie per creare ordini
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final CartService cartService;
    private final OrderMapper orderMapper;

    // Il "megafono" per annunciare eventi
    private final ApplicationEventPublisher eventPublisher;

    // ========== STRATEGY PATTERN IMPLEMENTATION ==========
    // Spring inietta automaticamente TUTTE le implementazioni di OrderValidator
    private final List<OrderValidator> validators;

    // Parametri del comando - si impostano prima dell'execute
    private Long userId;
    private CreateOrderRequest request;
    private Order createdOrder; // Per rollback se serve

    /**
     * Prepara il comando con i dati necessari
     * Si chiama prima di execute()
     */
    public CreateOrderCommand init(Long userId, CreateOrderRequest request) {
        this.userId = userId;
        this.request = request;
        this.createdOrder = null; // Reset per riuso
        return this;
    }

    /**
     * Esegue la creazione dell'ordine
     * Tutta la logica che prima era in OrderService ora è qui
     */
    @Override
    @Transactional
    public OrderResponse execute() {
        log.debug("Creating order for user: {}", userId);

        // 1. Carica dati necessari
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Utente non trovato"));

        Cart cart = cartRepository.findByUser(user)
                .orElseThrow(() -> new IllegalArgumentException("Carrello vuoto"));

        List<CartItem> cartItems = cartItemRepository.findByCart(cart);

        // 2. ========== STRATEGY PATTERN IN ACTION ==========
        runValidations(userId, cartItems); // viene fatta qui la validazione

        // 3. Calcola totale e crea ordine
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
        this.createdOrder = savedOrder; // Salva per rollback

        // 4. Annuncia "Ordine creato!" a tutti gli interessati
        // Spring troverà automaticamente tutti i @EventListener che ascoltano OrderCreatedEvent
        eventPublisher.publishEvent(new OrderCreatedEvent(
                savedOrder.getId(),
                user.getEmail(),
                savedOrder.getOrderNumber()
        ));

        // 5. Crea order items e aggiorna stock
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

        // 6. Svuota carrello
        cartService.clearCart(userId);

        log.info("Order created: {}", savedOrder.getOrderNumber());
        return orderMapper.toResponse(savedOrder, orderItems);
    }

    /**
     * Annulla la creazione se qualcosa va storto dopo il salvataggio
     */
    @Override
    public void rollback() {
        if (createdOrder != null) {
            log.warn("Rolling back order: {}", createdOrder.getOrderNumber());
            try {
                orderRepository.delete(createdOrder);
                log.info("Order rolled back: {}", createdOrder.getOrderNumber());
            } catch (Exception e) {
                log.error("Rollback failed for order: {}", createdOrder.getOrderNumber(), e);
            }
        }
    }

    // Genera numero ordine
    private String generateOrderNumber() {
        return "ORD-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    // ========== STRATEGY PATTERN IMPLEMENTATION ==========
    /**
     * Esegue tutte le strategie di validazione attive.
     * <p>
     * QUESTO È IL CUORE DEL PATTERN:
     * - Ordina le strategie per ordine di esecuzione
     * - Esegue ogni strategia indipendentemente
     * - Se una strategia fallisce, ferma tutto
     * - NON devi mai modificare questo metodo!
     * <p>
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
}
