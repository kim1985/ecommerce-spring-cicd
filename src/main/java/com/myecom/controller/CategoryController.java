package com.myecom.controller;

import com.myecom.dto.product.CategoryRequest;
import com.myecom.dto.product.CategoryResponse;
import com.myecom.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * Controller REST per gestire le categorie prodotti nell'e-commerce.
 *
 * Questo controller segue l'architettura corretta:
 * Client → Controller → Service → Repository → Database
 *
 * Il controller si occupa solo di:
 * - Ricevere richieste HTTP
 * - Validare parametri
 * - Chiamare il service appropriato
 * - Restituire risposte HTTP
 *
 * Tutta la logica di business è delegata al CategoryService.
 */
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    /**
     * Crea una nuova categoria prodotti.
     *
     * POST http://localhost:8090/api/categories
     *
     * Body esempio:
     * {
     *   "name": "Electronics",
     *   "description": "Electronic devices and gadgets",
     *   "active": true
     * }
     */
    @PostMapping
    public ResponseEntity<CategoryResponse> createCategory(@Valid @RequestBody CategoryRequest request) {
        CategoryResponse createdCategory = categoryService.createCategory(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdCategory);
    }

    /**
     * Recupera tutte le categorie attive per il catalogo pubblico.
     *
     * GET http://localhost:8090/api/categories
     *
     * Questo endpoint è pubblico e mostra solo categorie attive.
     */
    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getActiveCategories() {
        List<CategoryResponse> categories = categoryService.getActiveCategories();
        return ResponseEntity.ok(categories);
    }

    /**
     * Recupera tutte le categorie (incluse disattive) per pannello amministrativo.
     *
     * GET http://localhost:8090/api/categories/all
     *
     * Questo endpoint dovrebbe essere protetto e accessibile solo agli admin.
     */
    @GetMapping("/all")
    public ResponseEntity<List<CategoryResponse>> getAllCategories() {
        List<CategoryResponse> categories = categoryService.getAllCategories();
        return ResponseEntity.ok(categories);
    }

    /**
     * Recupera una categoria specifica per ID.
     *
     * GET http://localhost:8090/api/categories/1
     */
    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponse> getCategory(@PathVariable Long id) {
        Optional<CategoryResponse> category = categoryService.findById(id);
        return category.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Cerca categorie per nome (funzionalità di ricerca).
     *
     * GET http://localhost:8090/api/categories/search?name=electronics
     */
    @GetMapping("/search")
    public ResponseEntity<List<CategoryResponse>> searchCategories(@RequestParam String name) {
        List<CategoryResponse> categories = categoryService.searchByName(name);
        return ResponseEntity.ok(categories);
    }

    /**
     * Aggiorna una categoria esistente.
     *
     * PUT http://localhost:8090/api/categories/1
     *
     * Body esempio:
     * {
     *   "name": "Electronics Updated",
     *   "description": "Updated description",
     *   "active": true
     * }
     */
    @PutMapping("/{id}")
    public ResponseEntity<CategoryResponse> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CategoryRequest request) {

        CategoryResponse updatedCategory = categoryService.updateCategory(id, request);
        return ResponseEntity.ok(updatedCategory);
    }

    /**
     * Disattiva una categoria (soft delete).
     *
     * DELETE http://localhost:8090/api/categories/1
     *
     * Non elimina fisicamente la categoria ma la disattiva.
     * Questo preserva l'integrità dei dati per prodotti esistenti.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivateCategory(@PathVariable Long id) {
        categoryService.deactivateCategory(id);
        return ResponseEntity.noContent().build();
    }
}