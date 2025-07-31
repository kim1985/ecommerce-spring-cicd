package com.myecom.service;

import com.myecom.dto.product.CategoryRequest;
import com.myecom.dto.product.CategoryResponse;
import com.myecom.model.Category;
import com.myecom.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Service per gestire le operazioni di business sulle categorie prodotti.
 *
 * Questo service implementa la logica di business per le categorie:
 * - Validazione dei dati di input
 * - Controllo duplicati
 * - Conversione tra entità e DTO
 * - Gestione regole di business specifiche
 *
 * Segue il pattern: Controller → Service → Repository
 */
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    /**
     * Crea una nuova categoria verificando che non esista già una con lo stesso nome.
     *
     * Regole di business:
     * - Nome categoria deve essere unico (case-insensitive)
     * - Nome non può essere vuoto o solo spazi
     * - Se non specificato, la categoria è attiva per default
     */
    public CategoryResponse createCategory(CategoryRequest request) {
        // Valida che il nome non sia già in uso
        if (categoryRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("Esiste già una categoria con nome: " + request.getName());
        }

        // Crea l'entità Category dal DTO
        Category category = Category.builder()
                .name(request.getName().trim())                    // Rimuove spazi extra
                .description(request.getDescription())
                .active(request.getActive() != null ? request.getActive() : true)  // Default true
                .build();

        // Salva nel database
        Category savedCategory = categoryRepository.save(category);

        // Converte l'entità salvata in DTO per la risposta
        return convertToResponse(savedCategory);
    }

    /**
     * Recupera tutte le categorie attive per il catalogo pubblico.
     *
     * Le categorie disattive non vengono mostrate ai clienti.
     */
    public List<CategoryResponse> getActiveCategories() {
        return categoryRepository.findByActiveTrue().stream()
                .map(this::convertToResponse)
                .toList();
    }

    /**
     * Recupera tutte le categorie (incluse quelle disattive) per pannello admin.
     */
    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(this::convertToResponse)
                .toList();
    }

    /**
     * Trova una categoria specifica per ID.
     */
    public Optional<CategoryResponse> findById(Long id) {
        return categoryRepository.findById(id)
                .map(this::convertToResponse);
    }

    /**
     * Cerca categorie per nome (case-insensitive).
     *
     * Utile per funzionalità di ricerca nel pannello admin.
     */
    public List<CategoryResponse> searchByName(String name) {
        return categoryRepository.searchByName(name).stream()
                .map(this::convertToResponse)
                .toList();
    }

    /**
     * Aggiorna una categoria esistente.
     *
     * Permette di modificare nome, descrizione e stato attivo/inattivo.
     */
    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Categoria non trovata"));

        // Verifica che il nuovo nome non sia già in uso (se diverso da quello attuale)
        if (!category.getName().equals(request.getName()) &&
                categoryRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("Esiste già una categoria con nome: " + request.getName());
        }

        // Aggiorna i campi
        category.setName(request.getName().trim());
        category.setDescription(request.getDescription());
        if (request.getActive() != null) {
            category.setActive(request.getActive());
        }

        Category updatedCategory = categoryRepository.save(category);
        return convertToResponse(updatedCategory);
    }

    /**
     * Disattiva una categoria invece di eliminarla fisicamente.
     *
     * Questo preserva i dati storici e i riferimenti dai prodotti esistenti.
     * È una best practice per mantenere l'integrità dei dati.
     */
    public void deactivateCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Categoria non trovata"));

        category.setActive(false);
        categoryRepository.save(category);
    }

    /**
     * Converte un'entità Category in CategoryResponse per l'API.
     *
     * Questo metodo nasconde i dettagli interni dell'entità e espone
     * solo i dati che servono al client.
     */
    private CategoryResponse convertToResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .active(category.isActive())
                .build();
    }
}