package com.myecom.repository;

import com.myecom.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Classe base per i test dei repository con dati di test comuni
 */
@DataJpaTest
@ActiveProfiles("test")
public abstract class BaseRepositoryTest {

    @Autowired
    protected TestEntityManager entityManager;

    // Dati di test condivisi
    protected User testUser;
    protected User adminUser;
    protected Category electronics;
    protected Category clothing;
    protected Product laptop;
    protected Product smartphone;
    protected Order testOrder;
    protected Cart testCart;

    @BeforeEach
    void setUp() {
        // Crea utenti di test
        testUser = User.builder()
                .email("test@example.com")
                .password("password123")
                .firstName("Mario")
                .lastName("Rossi")
                .phone("123456789")
                .address("Via Roma 1")
                .city("Milano")
                .zipCode("20100")
                .role(User.Role.USER)
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .build();

        adminUser = User.builder()
                .email("admin@example.com")
                .password("admin123")
                .firstName("Admin")
                .lastName("User")
                .role(User.Role.ADMIN)
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .build();

        entityManager.persistAndFlush(testUser);
        entityManager.persistAndFlush(adminUser);

        // Crea categorie di test
        electronics = Category.builder()
                .name("Electronics")
                .description("Electronic devices")
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();

        clothing = Category.builder()
                .name("Clothing")
                .description("Clothing items")
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();

        entityManager.persistAndFlush(electronics);
        entityManager.persistAndFlush(clothing);

        // Crea prodotti di test
        laptop = Product.builder()
                .name("Laptop Dell")
                .description("High performance laptop")
                .price(new BigDecimal("999.99"))
                .stockQuantity(10)
                .brand("Dell")
                .category(electronics)
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();

        smartphone = Product.builder()
                .name("iPhone 15")
                .description("Latest iPhone")
                .price(new BigDecimal("1299.99"))
                .stockQuantity(5)
                .brand("Apple")
                .category(electronics)
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();

        entityManager.persistAndFlush(laptop);
        entityManager.persistAndFlush(smartphone);

        // Crea carrello di test
        testCart = Cart.builder()
                .user(testUser)
                .createdAt(LocalDateTime.now())
                .build();

        entityManager.persistAndFlush(testCart);

        // Pulisce la cache per assicurare che le query siano fresche
        entityManager.clear();
    }
}
