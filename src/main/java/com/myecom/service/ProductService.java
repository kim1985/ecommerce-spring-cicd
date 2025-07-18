package com.myecom.service;

import com.myecom.dto.product.*;
import com.myecom.model.Category;
import com.myecom.model.Product;
import com.myecom.repository.CategoryRepository;
import com.myecom.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    // Crea nuovo prodotto
    public ProductResponse createProduct(ProductRequest request) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Categoria non trovata"));

        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .stockQuantity(request.getStockQuantity())
                .imageUrl(request.getImageUrl())
                .brand(request.getBrand())
                .category(category)
                .active(true)
                .build();

        Product savedProduct = productRepository.save(product);
        return convertToResponse(savedProduct);
    }

    // Trova prodotto per ID
    public Optional<ProductResponse> findById(Long id) {
        return productRepository.findById(id)
                .filter(Product::isActive)
                .map(this::convertToResponse);
    }

    // Lista prodotti con paginazione
    public PageResponse<ProductResponse> getProducts(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Product> productPage = productRepository.findByActiveTrue(pageable);

        List<ProductResponse> products = productPage.getContent().stream()
                .map(this::convertToResponse)
                .toList();

        return PageResponse.<ProductResponse>builder()
                .content(products)
                .currentPage(productPage.getNumber())
                .totalPages(productPage.getTotalPages())
                .totalElements(productPage.getTotalElements())
                .hasNext(productPage.hasNext())
                .hasPrevious(productPage.hasPrevious())
                .build();
    }

    // Cerca prodotti
    public PageResponse<ProductResponse> searchProducts(String search, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Product> productPage = productRepository.searchByNameOrDescription(search, pageable);

        List<ProductResponse> products = productPage.getContent().stream()
                .map(this::convertToResponse)
                .toList();

        return PageResponse.<ProductResponse>builder()
                .content(products)
                .currentPage(productPage.getNumber())
                .totalPages(productPage.getTotalPages())
                .totalElements(productPage.getTotalElements())
                .hasNext(productPage.hasNext())
                .hasPrevious(productPage.hasPrevious())
                .build();
    }

    // Converte Product a ProductResponse
    private ProductResponse convertToResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .stockQuantity(product.getStockQuantity())
                .imageUrl(product.getImageUrl())
                .brand(product.getBrand())
                .active(product.isActive())
                .inStock(product.isInStock())
                .createdAt(product.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .category(convertCategoryToResponse(product.getCategory()))
                .build();
    }

    // Converte Category a CategoryResponse
    private CategoryResponse convertCategoryToResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .active(category.isActive())
                .build();
    }
}
