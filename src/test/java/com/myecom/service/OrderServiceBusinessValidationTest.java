package com.myecom.service;

import com.myecom.dto.order.CreateOrderRequest;
import com.myecom.exception.BusinessException;
import com.myecom.model.User;
import com.myecom.model.Cart;
import com.myecom.model.Product;
import com.myecom.model.Category;
import com.myecom.model.CartItem;
import com.myecom.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OrderServiceBusinessValidationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Test
    void shouldRejectOrderWithEmptyCart() {
        // Given - Crea utente con carrello vuoto
        User user = createTestUser("empty@test.com");
        Cart cart = createEmptyCart(user);

        CreateOrderRequest request = new CreateOrderRequest();
        request.setShippingAddress("Via Test 123");

        // When & Then - Deve lanciare BusinessException
        assertThatThrownBy(() -> orderService.createOrder(user.getId(), request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("carrello è vuoto");
    }

    @Test
    void shouldRejectOrderAboveLimit() {
        // Given - Crea ordine costoso (sopra €5000)
        User user = createTestUser("expensive@test.com");
        Cart cart = createCartWithExpensiveProduct(user);

        CreateOrderRequest request = new CreateOrderRequest();
        request.setShippingAddress("Via Test 123");

        // When & Then - Deve lanciare BusinessException
        assertThatThrownBy(() -> orderService.createOrder(user.getId(), request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("troppo grande");
    }

    @Test
    void shouldRejectOrderWithInsufficientStock() {
        // Given - Prodotto con poco stock
        User user = createTestUser("stock@test.com");
        Cart cart = createCartWithLowStockProduct(user);

        CreateOrderRequest request = new CreateOrderRequest();
        request.setShippingAddress("Via Test 123");

        // When & Then - Deve lanciare BusinessException
        assertThatThrownBy(() -> orderService.createOrder(user.getId(), request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("disponibili solo");
    }

    // Helper methods per creare dati di test
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

    private Cart createEmptyCart(User user) {
        Cart cart = Cart.builder()
                .user(user)
                .build();
        return cartRepository.save(cart);
    }

    private Cart createCartWithExpensiveProduct(User user) {
        // Crea categoria
        Category category = Category.builder()
                .name("Luxury")
                .active(true)
                .build();
        categoryRepository.save(category);

        // Crea prodotto costoso
        Product expensiveProduct = Product.builder()
                .name("Prodotto Costoso")
                .price(new BigDecimal("6000.00")) // Sopra il limite di €5000
                .stockQuantity(10)
                .category(category)
                .active(true)
                .build();
        productRepository.save(expensiveProduct);

        // Crea carrello
        Cart cart = Cart.builder()
                .user(user)
                .build();
        cartRepository.save(cart);

        // Aggiungi prodotto al carrello
        CartItem cartItem = CartItem.builder()
                .cart(cart)
                .product(expensiveProduct)
                .quantity(1)
                .build();
        cartItemRepository.save(cartItem);

        return cart;
    }

    private Cart createCartWithLowStockProduct(User user) {
        // Crea categoria
        Category category = Category.builder()
                .name("Electronics")
                .active(true)
                .build();
        categoryRepository.save(category);

        // Crea prodotto con poco stock
        Product lowStockProduct = Product.builder()
                .name("Prodotto Limitato")
                .price(new BigDecimal("100.00"))
                .stockQuantity(2) // Solo 2 pezzi disponibili
                .category(category)
                .active(true)
                .build();
        productRepository.save(lowStockProduct);

        // Crea carrello
        Cart cart = Cart.builder()
                .user(user)
                .build();
        cartRepository.save(cart);

        // Prova a ordinare 5 pezzi (più di quelli disponibili)
        CartItem cartItem = CartItem.builder()
                .cart(cart)
                .product(lowStockProduct)
                .quantity(5)
                .build();
        cartItemRepository.save(cartItem);

        return cart;
    }
}
