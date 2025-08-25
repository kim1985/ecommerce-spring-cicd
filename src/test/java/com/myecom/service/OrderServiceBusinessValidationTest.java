package com.myecom.service;

import com.myecom.dto.order.CreateOrderRequest;
import com.myecom.exception.BusinessException;
import com.myecom.model.*;
import com.myecom.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * AGGIORNATO: Test di integrazione per le validazioni business con i nuovi Validator
 *
 * Questo test verifica che tutte le validazioni funzionino insieme nel contesto Spring completo.
 * Ora usa il Strategy Pattern con i Validator invece della logica inline.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OrderServiceBusinessValidationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Test
    void shouldRejectOrderWithEmptyCart() {
        // Given - Utente senza carrello (EmptyCartValidator test)
        User user = createTestUser("empty@test.com");

        CreateOrderRequest request = new CreateOrderRequest();
        request.setShippingAddress("Via Test 123");

        // When & Then - deve fallire per carrello vuoto
        Exception exception = assertThrows(Exception.class,
                () -> orderService.createOrder(user.getId(), request));

        assertTrue(exception.getMessage().contains("vuoto") ||
                exception.getMessage().contains("Carrello"));
    }

    @Test
    void shouldRejectOrderAboveLimit() {
        // Given - Ordine costoso (PriceLimitValidator test)
        User user = createTestUser("expensive@test.com");
        createCartWithExpensiveProduct(user);

        CreateOrderRequest request = new CreateOrderRequest();
        request.setShippingAddress("Via Test 123");

        // When & Then - deve fallire per prezzo troppo alto
        Exception exception = assertThrows(Exception.class,
                () -> orderService.createOrder(user.getId(), request));

        assertTrue(exception.getMessage().contains("grande") ||
                exception.getMessage().contains("limite"));
    }

    @Test
    void shouldRejectOrderWithInsufficientStock() {
        // Given - Prodotto con poco stock (StockValidator test)
        User user = createTestUser("stock@test.com");
        createCartWithLowStockProduct(user);

        CreateOrderRequest request = new CreateOrderRequest();
        request.setShippingAddress("Via Test 123");

        // When & Then - deve fallire per stock insufficiente
        Exception exception = assertThrows(Exception.class,
                () -> orderService.createOrder(user.getId(), request));

        assertTrue(exception.getMessage().contains("disponibili") ||
                exception.getMessage().contains("stock"));
    }

    // Helper methods semplificati
    private User createTestUser(String email) {
        User user = User.builder()
                .email(email)
                .password("password123")
                .firstName("Test")
                .lastName("User")
                .role(User.Role.USER)
                .enabled(true)
                .build();
        return userRepository.save(user);
    }

    private void createCartWithExpensiveProduct(User user) {
        Category category = categoryRepository.save(
                Category.builder().name("Luxury").active(true).build()
        );

        Product expensiveProduct = productRepository.save(
                Product.builder()
                        .name("Prodotto Costoso")
                        .price(new BigDecimal("6000.00")) // Sopra limite €5000
                        .stockQuantity(10)
                        .category(category)
                        .active(true)
                        .build()
        );

        Cart cart = cartRepository.save(
                Cart.builder().user(user).build()
        );

        cartItemRepository.save(
                CartItem.builder()
                        .cart(cart)
                        .product(expensiveProduct)
                        .quantity(1)
                        .build()
        );
    }

    private void createCartWithLowStockProduct(User user) {
        Category category = categoryRepository.save(
                Category.builder().name("Electronics").active(true).build()
        );

        Product lowStockProduct = productRepository.save(
                Product.builder()
                        .name("Prodotto Limitato")
                        .price(new BigDecimal("100.00"))
                        .stockQuantity(2) // Solo 2 disponibili
                        .category(category)
                        .active(true)
                        .build()
        );

        Cart cart = cartRepository.save(
                Cart.builder().user(user).build()
        );

        cartItemRepository.save(
                CartItem.builder()
                        .cart(cart)
                        .product(lowStockProduct)
                        .quantity(5) // Richiesti 5, disponibili solo 2
                        .build()
        );
    }
}