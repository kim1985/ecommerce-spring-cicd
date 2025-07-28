package com.myecom.controller;

import com.myecom.dto.product.CategoryResponse;
import com.myecom.dto.product.PageResponse;
import com.myecom.dto.product.ProductRequest;
import com.myecom.dto.product.ProductResponse;
import com.myecom.service.ProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private ProductService productService;

    @InjectMocks
    private ProductController productController;

    private ProductRequest productRequest;
    private ProductResponse productResponse;
    private PageResponse<ProductResponse> pageResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(productController).build();

        productRequest = ProductRequest.builder()
                .name("iPhone 15")
                .description("Latest iPhone")
                .price(new BigDecimal("1299.99"))
                .stockQuantity(50)
                .imageUrl("http://example.com/iphone.jpg")
                .brand("Apple")
                .categoryId(1L)
                .active(true)
                .build();

        CategoryResponse category = CategoryResponse.builder()
                .id(1L)
                .name("Electronics")
                .description("Electronic devices")
                .active(true)
                .build();

        productResponse = ProductResponse.builder()
                .id(1L)
                .name("iPhone 15")
                .description("Latest iPhone")
                .price(new BigDecimal("1299.99"))
                .stockQuantity(50)
                .imageUrl("http://example.com/iphone.jpg")
                .brand("Apple")
                .active(true)
                .inStock(true)
                .category(category)
                .createdAt("2024-01-15T10:30:00")
                .build();

        pageResponse = PageResponse.<ProductResponse>builder()
                .content(Arrays.asList(productResponse))
                .currentPage(0)
                .totalPages(1)
                .totalElements(1L)
                .hasNext(false)
                .hasPrevious(false)
                .build();
    }

    @Test
    void shouldGetProducts() throws Exception {
        // Given
        when(productService.getProducts(anyInt(), anyInt())).thenReturn(pageResponse);

        // When & Then
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].name").value("iPhone 15"))
                .andExpect(jsonPath("$.content[0].price").value(1299.99))
                .andExpect(jsonPath("$.currentPage").value(0))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void shouldGetProductById() throws Exception {
        // Given
        when(productService.findById(1L)).thenReturn(Optional.of(productResponse));

        // When & Then
        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("iPhone 15"))
                .andExpect(jsonPath("$.price").value(1299.99))
                .andExpect(jsonPath("$.brand").value("Apple"))
                .andExpect(jsonPath("$.inStock").value(true))
                .andExpect(jsonPath("$.category.name").value("Electronics"));
    }

    @Test
    void shouldReturnEmptyWhenProductNotFound() throws Exception {
        // Given
        when(productService.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/api/products/999"))
                .andExpect(status().isOk())
                .andExpect(content().string(""));
    }

    @Test
    void shouldSearchProducts() throws Exception {
        // Given
        when(productService.searchProducts(anyString(), anyInt(), anyInt())).thenReturn(pageResponse);

        // When & Then
        mockMvc.perform(get("/api/products/search")
                        .param("q", "iphone"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].name").value("iPhone 15"));
    }

    @Test
    void shouldSearchProductsWithPagination() throws Exception {
        // Given
        when(productService.searchProducts("laptop", 0, 5)).thenReturn(pageResponse);

        // When & Then
        mockMvc.perform(get("/api/products/search")
                        .param("q", "laptop")
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void shouldCreateProduct() throws Exception {
        // Given
        when(productService.createProduct(any(ProductRequest.class))).thenReturn(productResponse);

        // When & Then
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.name").value("iPhone 15"))
                .andExpect(jsonPath("$.price").value(1299.99))
                .andExpect(jsonPath("$.brand").value("Apple"));
    }

    @Test
    void shouldHandleServiceException() throws Exception {
        // Given
        when(productService.createProduct(any(ProductRequest.class)))
                .thenThrow(new IllegalArgumentException("Categoria non trovata"));

        // When & Then
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productRequest)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void shouldGetProductsWithPagination() throws Exception {
        // Given
        when(productService.getProducts(1, 10)).thenReturn(pageResponse);

        // When & Then
        mockMvc.perform(get("/api/products")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }
}