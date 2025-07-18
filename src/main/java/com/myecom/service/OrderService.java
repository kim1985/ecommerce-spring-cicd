package com.myecom.service;

import com.myecom.dto.order.CreateOrderRequest;
import com.myecom.dto.order.OrderItemResponse;
import com.myecom.dto.order.OrderResponse;
import com.myecom.model.*;
import com.myecom.repository.CartItemRepository;
import com.myecom.repository.CartRepository;
import com.myecom.repository.OrderRepository;
import com.myecom.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
        if (cartItems.isEmpty()) {
            throw new IllegalArgumentException("Carrello vuoto");
        }

        // Verifica disponibilità prodotti
        for (CartItem item : cartItems) {
            if (item.getProduct().getStockQuantity() < item.getQuantity()) {
                throw new IllegalArgumentException("Quantità non disponibile per: " + item.getProduct().getName());
            }
        }

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
}