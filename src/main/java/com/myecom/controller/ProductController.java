package com.myecom.controller;

import com.myecom.dto.product.PageResponse;
import com.myecom.dto.product.ProductRequest;
import com.myecom.dto.product.ProductResponse;
import com.myecom.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    /**
     * Lista prodotti con paginazione
     *
     * GET http://localhost:8080/api/products
     * GET http://localhost:8080/api/products?page=0&size=10
     */
    @GetMapping
    public PageResponse<ProductResponse> getProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return productService.getProducts(page, size);
    }

    /**
     * Dettaglio singolo prodotto
     *
     * GET http://localhost:8080/api/products/1
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProduct(@PathVariable Long id) {
        Optional<ProductResponse> product = productService.findById(id);
        return product.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Ricerca prodotti per nome/descrizione
     *
     * GET http://localhost:8080/api/products/search?q=laptop
     * GET http://localhost:8080/api/products/search?q=laptop&page=0&size=10
     */
    @GetMapping("/search")
    public PageResponse<ProductResponse> searchProducts(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return productService.searchProducts(q, page, size);
    }

    /**
     * Crea nuovo prodotto (admin)
     *
     * POST http://localhost:8080/api/products
     *
     * Body:
     * {
     *   "name": "iPhone 15",
     *   "description": "Latest iPhone",
     *   "price": 1299.99,
     *   "stockQuantity": 50,
     *   "imageUrl": "http://example.com/iphone.jpg",
     *   "brand": "Apple",
     *   "categoryId": 1,
     *   "active": true
     * }
     */
    @PostMapping
    public ProductResponse createProduct(@Valid @RequestBody ProductRequest request) {
        return productService.createProduct(request);
    }
}