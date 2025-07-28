package com.myecom.controller;

import com.myecom.dto.order.CreateOrderRequest;
import com.myecom.dto.order.OrderResponse;
import com.myecom.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * Crea nuovo ordine dal carrello dell'utente
     *
     * POST http://localhost:8080/api/orders/1
     *
     * Body:
     * {
     *   "shippingAddress": "Via Roma 1, Milano 20100",
     *   "notes": "Consegnare dopo le 18:00"
     * }
     */
    @PostMapping("/{userId}")
    public OrderResponse createOrder(@PathVariable Long userId, @Valid @RequestBody CreateOrderRequest request) {
        return orderService.createOrder(userId, request);
    }

    /**
     * Lista tutti gli ordini di un utente
     *
     * GET http://localhost:8080/api/orders/user/1
     */
    @GetMapping("/user/{userId}")
    public List<OrderResponse> getUserOrders(@PathVariable Long userId) {
        return orderService.getUserOrders(userId);
    }

    /**
     * Dettaglio singolo ordine
     *
     * GET http://localhost:8080/api/orders/1
     */
    @GetMapping("/{orderId}")
    public Optional<OrderResponse> getOrder(@PathVariable Long orderId) {
        return orderService.findById(orderId);
    }
}