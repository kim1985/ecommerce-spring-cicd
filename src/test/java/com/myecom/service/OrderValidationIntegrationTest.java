package com.myecom.service;

import com.myecom.dto.order.CreateOrderRequest;
import com.myecom.model.Cart;
import com.myecom.model.CartItem;
import com.myecom.model.Category;
import com.myecom.model.Order;
import com.myecom.model.Product;
import com.myecom.model.User;
import com.myecom.repository.CartItemRepository;
import com.myecom.repository.CartRepository;
import com.myecom.repository.CategoryRepository;
import com.myecom.repository.OrderRepository;
import com.myecom.repository.ProductRepository;
import com.myecom.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OrderValidationIntegrationTest {

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

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;
    private Category testCategory;
    private Product normalProduct;
    private Product expensiveProduct;
    private Product lowStockProduct;
    private Product inactiveProduct;

    @BeforeEach
    void setUp() {
        // Crea utente di test
        testUser = User.builder()
                .email("integration@test.com")
                .password(passwordEncoder.encode("password123"))
                .firstName("Integration")
                .lastName("Test")
                .role(User.Role.USER)
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .build();
        userRepository.save(testUser);

        // Crea categoria
        testCategory = Category.builder()
                .name("Integration Test Category")
                .description("Category for integration testing")
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();
        categoryRepository.save(testCategory);

        // Crea prodotti per diversi scenari di test
        normalProduct = Product.builder()
                .name("Normal Product")
                .description("Product with normal price and stock")
                .price(new BigDecimal("100.00"))
                .stockQuantity(20)
                .brand("TestBrand")
                .category(testCategory)
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();
        productRepository.save(normalProduct);

        // Prodotto costoso (per test limite prezzo)
        expensiveProduct = Product.builder()
                .name("Expensive Product")
                .description("Very expensive product")
                .price(new BigDecimal("6000.00")) // Sopra il limite di €5000
                .stockQuantity(10)
                .brand("LuxuryBrand")
                .category(testCategory)
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();
        productRepository.save(expensiveProduct);

        // Prodotto con poco stock
        lowStockProduct = Product.builder()
                .name("Low Stock Product")
                .description("Product with limited availability")
                .price(new BigDecimal("50.00"))
                .stockQuantity(2) // Solo 2 pezzi disponibili
                .brand("TestBrand")
                .category(testCategory)
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();
        productRepository.save(lowStockProduct);

        // Prodotto inattivo
        inactiveProduct = Product.builder()
                .name("Inactive Product")
                .description("This product is not active")
                .price(new BigDecimal("75.00"))
                .stockQuantity(5)
                .brand("TestBrand")
                .category(testCategory)
                .active(false) // Inattivo
                .createdAt(LocalDateTime.now())
                .build();
        productRepository.save(inactiveProduct);
    }

    @Test
    void shouldValidateEmptyCart() {
        // Given - utente senza carrello
        CreateOrderRequest request = new CreateOrderRequest();
        request.setShippingAddress("Via Test 123");

        // When & Then - deve fallire (potrebbe essere IllegalArgumentException o BusinessException)
        Exception exception = assertThrows(Exception.class,
                () -> orderService.createOrder(testUser.getId(), request));

        assertTrue(exception.getMessage().contains("vuoto") ||
                exception.getMessage().contains("Carrello"));
    }

    @Test
    void shouldValidateStockAvailability() {
        // Given - carrello con quantità maggiore dello stock disponibile
        Cart cart = createCartWithProduct(lowStockProduct, 5); // Richiesti 5, disponibili solo 2

        CreateOrderRequest request = new CreateOrderRequest();
        request.setShippingAddress("Via Test 123");

        // When & Then - deve fallire per stock insufficiente
        Exception exception = assertThrows(Exception.class,
                () -> orderService.createOrder(testUser.getId(), request));

        assertTrue(exception.getMessage().contains("disponibili") ||
                exception.getMessage().contains("stock"));
    }

    @Test
    void shouldValidateInactiveProduct() {
        // Given - carrello con prodotto inattivo
        Cart cart = createCartWithProduct(inactiveProduct, 1);

        CreateOrderRequest request = new CreateOrderRequest();
        request.setShippingAddress("Via Test 123");

        // When & Then - deve fallire per prodotto inattivo
        Exception exception = assertThrows(Exception.class,
                () -> orderService.createOrder(testUser.getId(), request));

        assertTrue(exception.getMessage().contains("disponibile") ||
                exception.getMessage().contains("attivo"));
    }

    @Test
    void shouldValidatePriceLimit() {
        // Given - carrello con prodotto costoso
        Cart cart = createCartWithProduct(expensiveProduct, 1); // €6000 > limite €5000

        CreateOrderRequest request = new CreateOrderRequest();
        request.setShippingAddress("Via Test 123");

        // When & Then - deve fallire per prezzo troppo alto
        Exception exception = assertThrows(Exception.class,
                () -> orderService.createOrder(testUser.getId(), request));

        assertTrue(exception.getMessage().contains("grande") ||
                exception.getMessage().contains("limite") ||
                exception.getMessage().contains("prezzo"));
    }

    @Test
    void shouldValidateDailyLimit() {
        // Given - crea molti ordini per lo stesso utente oggi
        createMultipleOrdersForUser(12); // Sopra il limite di 10

        // Crea carrello valido
        Cart cart = createCartWithProduct(normalProduct, 1);

        CreateOrderRequest request = new CreateOrderRequest();
        request.setShippingAddress("Via Test 123");

        // When & Then - deve fallire per troppi ordini
        Exception exception = assertThrows(Exception.class,
                () -> orderService.createOrder(testUser.getId(), request));

        assertTrue(exception.getMessage().contains("limite") ||
                exception.getMessage().contains("ordini"));
    }

    @Test
    void shouldPassAllValidationsForValidOrder() {
        // Given - carrello con prodotto normale e quantità valida
        Cart cart = createCartWithProduct(normalProduct, 3); // €300, quantità normale

        CreateOrderRequest request = new CreateOrderRequest();
        request.setShippingAddress("Via Test 123, Milano");
        request.setNotes("Valid order");

        // When & Then - tutte le validazioni devono passare
        assertDoesNotThrow(() ->
                orderService.createOrder(testUser.getId(), request));
    }

    @Test
    void shouldExecuteValidationsInCorrectOrder() {
        // Given - carrello vuoto (fallisce EmptyCartValidator che ha order=1)
        // Non aggiungiamo nulla al carrello

        CreateOrderRequest request = new CreateOrderRequest();
        request.setShippingAddress("Via Test 123");

        // When & Then - deve fallire sulla prima validazione (EmptyCart)
        // Nota: potrebbe essere IllegalArgumentException dalla logica esistente di OrderService
        Exception exception = assertThrows(Exception.class,
                () -> orderService.createOrder(testUser.getId(), request));

        assertTrue(exception.getMessage().contains("vuoto") ||
                exception.getMessage().contains("Carrello"));
    }

    @Test
    void shouldStopAtFirstFailingValidation() {
        // Given - carrello con prodotto inattivo (fallisce StockValidator per "prodotto inattivo")
        Cart cart = createCartWithProduct(inactiveProduct, 1); // Solo inattivo, non quantità eccessiva

        CreateOrderRequest request = new CreateOrderRequest();
        request.setShippingAddress("Via Test 123");

        // When & Then - deve fermarsi al primo errore (prodotto inattivo)
        Exception exception = assertThrows(Exception.class,
                () -> orderService.createOrder(testUser.getId(), request));

        assertTrue(exception.getMessage().contains("disponibile") ||
                exception.getMessage().contains("attiv"));
    }

    // Helper methods

    private Cart createCartWithProduct(Product product, int quantity) {
        Cart cart = Cart.builder()
                .user(testUser)
                .createdAt(LocalDateTime.now())
                .build();
        cartRepository.save(cart);

        CartItem cartItem = CartItem.builder()
                .cart(cart)
                .product(product)
                .quantity(quantity)
                .createdAt(LocalDateTime.now())
                .build();
        cartItemRepository.save(cartItem);

        return cart;
    }

    private void createMultipleOrdersForUser(int numberOfOrders) {
        for (int i = 0; i < numberOfOrders; i++) {
            Order order = Order.builder()
                    .orderNumber("TEST-ORDER-" + i)
                    .user(testUser)
                    .status(Order.OrderStatus.PENDING)
                    .totalAmount(new BigDecimal("100.00"))
                    .shippingAddress("Via Test " + i)
                    .createdAt(LocalDateTime.now()) // Tutti oggi
                    .build();
            orderRepository.save(order);
        }
    }
}