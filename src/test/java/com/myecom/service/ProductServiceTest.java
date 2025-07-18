package com.myecom.service;

import com.myecom.dto.product.PageResponse;
import com.myecom.dto.product.ProductRequest;
import com.myecom.dto.product.ProductResponse;
import com.myecom.model.Category;
import com.myecom.model.Product;
import com.myecom.repository.CategoryRepository;
import com.myecom.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * Unit test per ProductService con Mock
 */
@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private ProductService productService;

    private ProductRequest productRequest;
    private Category electronics;
    private Product laptop;
    private Product smartphone;

    @BeforeEach
    void setUp() {
        // Setup categoria
        electronics = Category.builder()
                .id(1L)
                .name("Electronics")
                .description("Electronic devices")
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();

        // Setup prodotti
        laptop = Product.builder()
                .id(1L)
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
                .id(2L)
                .name("iPhone 15")
                .description("Latest iPhone")
                .price(new BigDecimal("1299.99"))
                .stockQuantity(5)
                .brand("Apple")
                .category(electronics)
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();

        // Setup request
        productRequest = ProductRequest.builder()
                .name("Nuovo Prodotto")
                .description("Descrizione del nuovo prodotto")
                .price(new BigDecimal("199.99"))
                .stockQuantity(20)
                .imageUrl("http://example.com/image.jpg")
                .brand("TestBrand")
                .categoryId(1L)
                .active(true)
                .build();
    }

    @Test
    void shouldCreateNewProduct() {
        // Given
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(electronics));

        Product newProduct = Product.builder()
                .id(3L)
                .name("Nuovo Prodotto")
                .description("Descrizione del nuovo prodotto")
                .price(new BigDecimal("199.99"))
                .stockQuantity(20)
                .imageUrl("http://example.com/image.jpg")
                .brand("TestBrand")
                .category(electronics)
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();

        when(productRepository.save(any(Product.class))).thenReturn(newProduct);

        // When
        ProductResponse response = productService.createProduct(productRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("Nuovo Prodotto");
        assertThat(response.getPrice()).isEqualTo(new BigDecimal("199.99"));
        assertThat(response.getStockQuantity()).isEqualTo(20);
        assertThat(response.getBrand()).isEqualTo("TestBrand");
        assertThat(response.isActive()).isTrue();
        assertThat(response.isInStock()).isTrue();
        assertThat(response.getCategory()).isNotNull();
        assertThat(response.getCategory().getName()).isEqualTo("Electronics");
    }

    @Test
    void shouldNotCreateProductWithInvalidCategory() {
        // Given
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        ProductRequest invalidRequest = ProductRequest.builder()
                .name("Prodotto Invalido")
                .price(new BigDecimal("99.99"))
                .stockQuantity(10)
                .categoryId(999L)
                .build();

        // When & Then
        assertThatThrownBy(() -> productService.createProduct(invalidRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Categoria non trovata");
    }

    @Test
    void shouldFindProductById() {
        // Given
        when(productRepository.findById(1L)).thenReturn(Optional.of(laptop));

        // When
        Optional<ProductResponse> found = productService.findById(1L);

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(1L);
        assertThat(found.get().getName()).isEqualTo("Laptop Dell");
        assertThat(found.get().getBrand()).isEqualTo("Dell");
    }

    @Test
    void shouldNotFindInactiveProduct() {
        // Given - prodotto inattivo
        Product inactiveProduct = Product.builder()
                .id(1L)
                .name("Laptop Dell")
                .active(false)
                .build();

        when(productRepository.findById(1L)).thenReturn(Optional.of(inactiveProduct));

        // When
        Optional<ProductResponse> found = productService.findById(1L);

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    void shouldGetProductsWithPagination() {
        // Given
        List<Product> products = Arrays.asList(laptop, smartphone);
        Page<Product> productPage = new PageImpl<>(products, PageRequest.of(0, 10), 2);

        when(productRepository.findByActiveTrue(any(Pageable.class))).thenReturn(productPage);

        // When
        PageResponse<ProductResponse> result = productService.getProducts(0, 10);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getCurrentPage()).isEqualTo(0);
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getTotalPages()).isEqualTo(1);
        assertThat(result.isHasNext()).isFalse();
        assertThat(result.isHasPrevious()).isFalse();

        // Verifica contenuto
        assertThat(result.getContent())
                .extracting(ProductResponse::getName)
                .containsExactlyInAnyOrder("Laptop Dell", "iPhone 15");
    }

    @Test
    void shouldSearchProducts() {
        // Given
        List<Product> laptopResults = Collections.singletonList(laptop);
        Page<Product> laptopPage = new PageImpl<>(laptopResults, PageRequest.of(0, 10), 1);

        when(productRepository.searchByNameOrDescription("laptop", PageRequest.of(0, 10)))
                .thenReturn(laptopPage);

        // When
        PageResponse<ProductResponse> result = productService.searchProducts("laptop", 0, 10);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Laptop Dell");
    }

    @Test
    void shouldReturnEmptyResultForNonExistentSearch() {
        // Given
        Page<Product> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 10), 0);

        when(productRepository.searchByNameOrDescription("prodottoinesistente", PageRequest.of(0, 10)))
                .thenReturn(emptyPage);

        // When
        PageResponse<ProductResponse> result = productService.searchProducts("prodottoinesistente", 0, 10);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
    }

    @Test
    void shouldNotFindProductWithNonExistentId() {
        // Given
        when(productRepository.findById(anyLong())).thenReturn(Optional.empty());

        // When
        Optional<ProductResponse> found = productService.findById(999L);

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    void shouldHandlePaginationCorrectly() {
        // Given - prima pagina
        List<Product> page1Products = Arrays.asList(laptop, smartphone);
        Page<Product> page1 = new PageImpl<>(page1Products, PageRequest.of(0, 10), 17);

        when(productRepository.findByActiveTrue(PageRequest.of(0, 10))).thenReturn(page1);

        // When
        PageResponse<ProductResponse> result = productService.getProducts(0, 10);

        // Then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getCurrentPage()).isEqualTo(0);
        assertThat(result.getTotalElements()).isEqualTo(17);
        assertThat(result.getTotalPages()).isEqualTo(2);
        assertThat(result.isHasNext()).isTrue();
        assertThat(result.isHasPrevious()).isFalse();
    }
}