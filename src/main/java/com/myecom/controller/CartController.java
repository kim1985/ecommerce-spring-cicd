package com.myecom.controller;

import com.myecom.dto.cart.CartItemRequest;
import com.myecom.dto.cart.CartResponse;
import com.myecom.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    /**
     * Visualizza carrello utente
     *
     * GET http://localhost:8080/api/cart/1
     */
    @GetMapping("/{userId}")
    public CartResponse getCart(@PathVariable Long userId) {
        return cartService.getCart(userId);
    }

    /**
     * Aggiungi prodotto al carrello
     *
     * POST http://localhost:8080/api/cart/1/add
     *
     * Body:
     * {
     *   "productId": 1,
     *   "quantity": 2
     * }
     */
    @PostMapping("/{userId}/add")
    public CartResponse addToCart(@PathVariable Long userId, @Valid @RequestBody CartItemRequest request) {
        return cartService.addToCart(userId, request);
    }

    /**
     * Rimuovi prodotto dal carrello
     *
     * DELETE http://localhost:8080/api/cart/1/product/1
     */
    @DeleteMapping("/{userId}/product/{productId}")
    public CartResponse removeFromCart(@PathVariable Long userId, @PathVariable Long productId) {
        return cartService.removeFromCart(userId, productId);
    }

    /**
     * Svuota completamente il carrello
     *
     * DELETE http://localhost:8080/api/cart/1/clear
     */
    @DeleteMapping("/{userId}/clear")
    public void clearCart(@PathVariable Long userId) {
        cartService.clearCart(userId);
    }
}