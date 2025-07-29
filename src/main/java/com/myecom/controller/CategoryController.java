package com.myecom.controller;

import com.myecom.dto.product.CategoryRequest;
import com.myecom.dto.product.CategoryResponse;
import com.myecom.model.Category;
import com.myecom.repository.CategoryRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller veloce per creare categorie.
 * Serve solo per testare, poi lo rimuoveremo o miglioreremo.
 */
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryRepository categoryRepository;

    /**
     * Crea nuova categoria
     *
     * POST http://localhost:8080/api/categories
     *
     * Body:
     * {
     *   "name": "Electronics",
     *   "description": "Electronic devices",
     *   "active": true
     * }
     */
    @PostMapping
    public CategoryResponse createCategory(@Valid @RequestBody CategoryRequest request) {
        Category category = Category.builder()
                .name(request.getName())
                .description(request.getDescription())
                .active(request.getActive() != null ? request.getActive() : true)
                .build();

        Category savedCategory = categoryRepository.save(category);
        return convertToResponse(savedCategory);
    }

    /**
     * Lista tutte le categorie (per debug)
     *
     * GET http://localhost:8080/api/categories
     */
    @GetMapping
    public List<CategoryResponse> getCategories() {
        return categoryRepository.findAll().stream()
                .map(this::convertToResponse)
                .toList();
    }

    // Converte Category a CategoryResponse
    private CategoryResponse convertToResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .active(category.isActive())
                .build();
    }
}
