package com.myecom.integration;

import com.myecom.model.Category;
import com.myecom.model.Product;
import com.myecom.model.User;
import com.myecom.model.Cart;
import com.myecom.model.CartItem;
import com.myecom.repository.CategoryRepository;
import com.myecom.repository.ProductRepository;
import com.myecom.repository.UserRepository;
import com.myecom.repository.CartRepository;
import com.myecom.repository.CartItemRepository;
import com.myecom.service.OrderService;
import com.myecom.dto.order.CreateOrderRequest;
import com.myecom.dto.order.OrderResponse;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;

/**
 * Test per creare dati di test e verificare che gli eventi funzionino.
 *
 * Questo test:
 * 1. Crea categoria, prodotto, utente
 * 2. Aggiunge prodotto al carrello
 * 3. Crea ordine
 * 4. Verifica che l'evento venga lanciato (vedrai nella console!)
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class EventTestDataSetup {

    @Autowired private CategoryRepository categoryRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private CartRepository cartRepository;
    @Autowired private CartItemRepository cartItemRepository;
    @Autowired private OrderService orderService;
    @Autowired private PasswordEncoder passwordEncoder;

    @Test
    void createTestDataAndTestEvents() {
        System.out.println("\n🚀 CREAZIONE DATI DI TEST E VERIFICA EVENTI");
        System.out.println("=" .repeat(50));

        // 1. Crea categoria
        System.out.println("📁 Creando categoria...");
        Category category = Category.builder()
                .name("Electronics")
                .description("Electronic devices and gadgets")
                .active(true)
                .build();
        Category savedCategory = categoryRepository.save(category);
        System.out.println("✅ Categoria creata: " + savedCategory.getName() + " (ID: " + savedCategory.getId() + ")");

        // 2. Crea prodotto
        System.out.println("📱 Creando prodotto...");
        Product product = Product.builder()
                .name("iPhone 15")
                .description("Latest iPhone model")
                .price(new BigDecimal("999.99"))
                .stockQuantity(50)
                .brand("Apple")
                .category(savedCategory)
                .active(true)
                .build();
        Product savedProduct = productRepository.save(product);
        System.out.println("✅ Prodotto creato: " + savedProduct.getName() + " (ID: " + savedProduct.getId() + ")");

        // 3. Crea utente
        System.out.println("👤 Creando utente...");
        User user = User.builder()
                .email("eventtest@example.com")
                .password(passwordEncoder.encode("password123"))
                .firstName("Mario")
                .lastName("Rossi")
                .phone("123456789")
                .address("Via Roma 1")
                .city("Milano")
                .zipCode("20100")
                .role(User.Role.USER)
                .enabled(true)
                .build();
        User savedUser = userRepository.save(user);
        System.out.println("✅ Utente creato: " + savedUser.getEmail() + " (ID: " + savedUser.getId() + ")");

        // 4. Crea carrello
        System.out.println("🛒 Creando carrello...");
        Cart cart = Cart.builder()
                .user(savedUser)
                .build();
        Cart savedCart = cartRepository.save(cart);

        // 5. Aggiungi prodotto al carrello
        System.out.println("📦 Aggiungendo prodotto al carrello...");
        CartItem cartItem = CartItem.builder()
                .cart(savedCart)
                .product(savedProduct)
                .quantity(2)
                .build();
        cartItemRepository.save(cartItem);
        System.out.println("✅ Prodotto aggiunto al carrello: 2x " + savedProduct.getName());

        // 6. Crea ordine (QUI DOVREBBE SCATTARE L'EVENTO!)
        System.out.println("\n🎯 CREANDO ORDINE - Aspettati l'evento nella console!");
        System.out.println("-".repeat(50));

        CreateOrderRequest orderRequest = new CreateOrderRequest();
        orderRequest.setShippingAddress("Via Roma 123, Milano");
        orderRequest.setNotes("Ordine di test per verificare eventi");

        try {
            OrderResponse orderResponse = orderService.createOrder(savedUser.getId(), orderRequest);

            System.out.println("\n✅ ORDINE CREATO CON SUCCESSO!");
            System.out.println("📦 Numero ordine: " + orderResponse.getOrderNumber());
            System.out.println("💰 Importo totale: €" + orderResponse.getTotalAmount());
            System.out.println("📧 Cliente: " + savedUser.getEmail());

            System.out.println("\n🎉 SE HAI VISTO IL MESSAGGIO EVENTO SOPRA, TUTTO FUNZIONA!");

        } catch (Exception e) {
            System.err.println("❌ ERRORE nella creazione ordine: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("\n" + "=".repeat(50));
        System.out.println("🏁 TEST COMPLETATO");
    }

    /**
     * Test semplice per verificare solo la creazione categoria
     */
    @Test
    void shouldCreateCategory() {
        // Test base per verificare che i repository funzionino
        Category category = Category.builder()
                .name("Test Category")
                .description("Just a test")
                .active(true)
                .build();

        Category saved = categoryRepository.save(category);

        System.out.println("✅ Categoria salvata: " + saved.getName() + " con ID: " + saved.getId());

        // Verifica che sia stata salvata
        assert saved.getId() != null;
        assert saved.getName().equals("Test Category");
    }
}
