package com.myecom.repository;

import com.myecom.model.Order;
import com.myecom.model.OrderItem;
import com.myecom.model.Product;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test per ProductRepository - verifica query di ricerca e filtri
 */
class ProductRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @Test
    void shouldFindActiveProducts() {
        // When
        List<Product> activeProducts = productRepository.findByActiveTrue();

        // Then
        assertThat(activeProducts).hasSize(2); // laptop + smartphone
        assertThat(activeProducts).extracting(Product::getName)
                .containsExactlyInAnyOrder("Laptop Dell", "iPhone 15");
    }

    @Test
    void shouldFindActiveProductsWithPagination() {
        // Given
        Pageable pageable = PageRequest.of(0, 1);

        // When
        Page<Product> productPage = productRepository.findByActiveTrue(pageable);

        // Then
        assertThat(productPage.getContent()).hasSize(1);
        assertThat(productPage.getTotalElements()).isEqualTo(2);
        assertThat(productPage.getTotalPages()).isEqualTo(2);
    }

    @Test
    void shouldFindProductsByCategory() {
        // When
        List<Product> electronicsProducts = productRepository.findByCategoryAndActiveTrue(electronics);
        List<Product> clothingProducts = productRepository.findByCategoryAndActiveTrue(clothing);

        // Then
        assertThat(electronicsProducts).hasSize(2);
        assertThat(clothingProducts).isEmpty();
    }

    @Test
    void shouldFindProductsByBrand() {
        // When
        List<Product> dellProducts = productRepository.findByBrandAndActiveTrue("Dell");
        List<Product> appleProducts = productRepository.findByBrandAndActiveTrue("Apple");
        List<Product> samsungProducts = productRepository.findByBrandAndActiveTrue("Samsung");

        // Then
        assertThat(dellProducts).hasSize(1);
        assertThat(dellProducts.get(0).getName()).isEqualTo("Laptop Dell");

        assertThat(appleProducts).hasSize(1);
        assertThat(appleProducts.get(0).getName()).isEqualTo("iPhone 15");

        assertThat(samsungProducts).isEmpty();
    }

    @Test
    void shouldFindProductsInStock() {
        // Given - crea prodotto esaurito
        Product outOfStock = Product.builder()
                .name("Out of Stock Product")
                .description("This is out of stock")
                .price(new BigDecimal("99.99"))
                .stockQuantity(0)
                .category(electronics)
                .active(true)
                .build();
        entityManager.persistAndFlush(outOfStock);

        // When
        List<Product> inStockProducts = productRepository.findByActiveTrueAndStockQuantityGreaterThan(0);
        List<Product> outOfStockProducts = productRepository.findByActiveTrueAndStockQuantityLessThanEqual(0);

        // Then
        assertThat(inStockProducts).hasSize(2); // laptop + smartphone
        assertThat(outOfStockProducts).hasSize(1);
        assertThat(outOfStockProducts.get(0).getName()).isEqualTo("Out of Stock Product");
    }

    @Test
    void shouldSearchProductsByName() {
        // When - ricerca case insensitive
        List<Product> laptopResults = productRepository.searchByName("laptop");
        List<Product> iphoneResults = productRepository.searchByName("IPHONE");
        List<Product> dellResults = productRepository.searchByName("dell");

        // Then
        assertThat(laptopResults).hasSize(1);
        assertThat(laptopResults.get(0).getName()).isEqualTo("Laptop Dell");

        assertThat(iphoneResults).hasSize(1);
        assertThat(iphoneResults.get(0).getName()).isEqualTo("iPhone 15");

        assertThat(dellResults).hasSize(1);
    }

    @Test
    void shouldSearchProductsByNameWithPagination() {
        // Given
        Pageable pageable = PageRequest.of(0, 1);

        // When
        Page<Product> results = productRepository.searchByName("phone", pageable);

        // Then
        assertThat(results.getContent()).hasSize(1);
        assertThat(results.getContent().get(0).getName()).contains("iPhone");
    }

    @Test
    void shouldSearchByNameOrDescription() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Product> laptopResults = productRepository.searchByNameOrDescription("laptop", pageable);
        Page<Product> performanceResults = productRepository.searchByNameOrDescription("performance", pageable);

        // Then
        assertThat(laptopResults.getContent()).hasSize(1);
        assertThat(performanceResults.getContent()).hasSize(1); // trova "High performance laptop"
    }

    @Test
    void shouldFindProductsByPriceRange() {
        // When
        List<Product> cheapProducts = productRepository.findByActiveTrueAndPriceBetween(
                new BigDecimal("0"), new BigDecimal("1000"));
        List<Product> expensiveProducts = productRepository.findByActiveTrueAndPriceBetween(
                new BigDecimal("1200"), new BigDecimal("2000"));

        // Then
        assertThat(cheapProducts).hasSize(1); // solo laptop (999.99)
        assertThat(cheapProducts.get(0).getName()).isEqualTo("Laptop Dell");

        assertThat(expensiveProducts).hasSize(1); // solo iPhone (1299.99)
        assertThat(expensiveProducts.get(0).getName()).isEqualTo("iPhone 15");
    }

    @Test
    void shouldFindLowStockProducts() {
        // When - trova prodotti con stock <= 5
        List<Product> lowStockProducts = productRepository.findLowStockProducts(5);

        // Then
        assertThat(lowStockProducts).hasSize(1); // solo smartphone con stock = 5
        assertThat(lowStockProducts.get(0).getName()).isEqualTo("iPhone 15");
    }

    @Test
    void shouldCountProductsByCategory() {
        // When
        List<Object[]> categoryStats = productRepository.countProductsByCategory();

        // Then
        assertThat(categoryStats).hasSize(1); // solo Electronics ha prodotti
        Object[] electronicsStats = categoryStats.get(0);
        assertThat(electronicsStats[0]).isEqualTo("Electronics"); // nome categoria
        assertThat(electronicsStats[1]).isEqualTo(2L); // conteggio prodotti
    }

    @Test
    void shouldFindRelatedProducts() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When - trova prodotti correlati al laptop (stessa categoria, escluso laptop stesso)
        List<Product> relatedProducts = productRepository.findRelatedProducts(
                electronics, laptop.getId(), pageable);

        // Then
        assertThat(relatedProducts).hasSize(1);
        assertThat(relatedProducts.get(0).getName()).isEqualTo("iPhone 15");
        assertThat(relatedProducts.get(0).getId()).isNotEqualTo(laptop.getId());
    }

    @Test
    void shouldFindBestSellingProducts() {
        // Given - crea ordini per simulare vendite
        var order1 = entityManager.persistFlushFind(
                Order.builder()
                        .orderNumber("ORD-001")
                        .user(testUser)
                        .status(Order.OrderStatus.DELIVERED)
                        .totalAmount(new BigDecimal("999.99"))
                        .shippingAddress("Via Roma 1")
                        .build()
        );

        // Crea OrderItems
        entityManager.persistAndFlush(
                OrderItem.builder()
                        .order(order1)
                        .product(laptop)
                        .quantity(2)
                        .unitPrice(laptop.getPrice())
                        .totalPrice(laptop.getPrice().multiply(new BigDecimal("2")))
                        .build()
        );

        entityManager.persistAndFlush(
                OrderItem.builder()
                        .order(order1)
                        .product(smartphone)
                        .quantity(1)
                        .unitPrice(smartphone.getPrice())
                        .totalPrice(smartphone.getPrice())
                        .build()
        );

        Pageable pageable = PageRequest.of(0, 10);

        // When
        List<Product> bestSellers = productRepository.findBestSellingProducts(pageable);

        // Then
        assertThat(bestSellers).hasSize(2);
        // Il laptop dovrebbe essere primo (quantità venduta = 2)
        assertThat(bestSellers.get(0).getName()).isEqualTo("Laptop Dell");
    }
}
