package com.myecom.controller;

import com.myecom.config.GlobalExceptionHandler;
import com.myecom.dto.product.CategoryRequest;
import com.myecom.dto.product.CategoryResponse;
import com.myecom.service.CategoryService;
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

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test per CategoryController che ora usa correttamente il service layer.
 *
 * Questi test verificano che:
 * - Il controller chiami correttamente i metodi del service
 * - Le risposte HTTP siano formattate correttamente
 * - Gli status code siano appropriati
 * - La validazione dei parametri funzioni
 */
@ExtendWith(MockitoExtension.class)
class CategoryControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private CategoryService categoryService;

    @InjectMocks
    private CategoryController categoryController;

    private CategoryRequest categoryRequest;
    private CategoryResponse categoryResponse;

    @BeforeEach
    void setUp() {
        // Configura MockMvc con GlobalExceptionHandler per gestire eccezioni
        mockMvc = MockMvcBuilders.standaloneSetup(categoryController)
                .setControllerAdvice(new GlobalExceptionHandler())  // Aggiunge gestione errori
                .build();

        categoryRequest = CategoryRequest.builder()
                .name("Electronics")
                .description("Electronic devices and gadgets")
                .active(true)
                .build();

        categoryResponse = CategoryResponse.builder()
                .id(1L)
                .name("Electronics")
                .description("Electronic devices and gadgets")
                .active(true)
                .build();
    }

    @Test
    void shouldCreateCategorySuccessfully() throws Exception {
        // Given - service restituisce categoria creata
        when(categoryService.createCategory(any(CategoryRequest.class))).thenReturn(categoryResponse);

        // When & Then - testa endpoint POST
        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(categoryRequest)))
                .andExpect(status().isCreated())                    // Status 201 Created
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Electronics"))
                .andExpect(jsonPath("$.description").value("Electronic devices and gadgets"))
                .andExpect(jsonPath("$.active").value(true));

        // Verifica che il service sia stato chiamato
        verify(categoryService).createCategory(any(CategoryRequest.class));
    }

    @Test
    void shouldGetActiveCategoriesSuccessfully() throws Exception {
        // Given - service restituisce lista categorie attive
        CategoryResponse clothing = CategoryResponse.builder()
                .id(2L)
                .name("Clothing")
                .description("Clothing items")
                .active(true)
                .build();

        List<CategoryResponse> activeCategories = Arrays.asList(categoryResponse, clothing);
        when(categoryService.getActiveCategories()).thenReturn(activeCategories);

        // When & Then - testa endpoint GET
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))         // Array con 2 elementi
                .andExpect(jsonPath("$[0].name").value("Electronics"))
                .andExpect(jsonPath("$[1].name").value("Clothing"))
                .andExpect(jsonPath("$[0].active").value(true))
                .andExpect(jsonPath("$[1].active").value(true));

        verify(categoryService).getActiveCategories();
    }

    @Test
    void shouldGetAllCategoriesForAdmin() throws Exception {
        // Given - service restituisce tutte le categorie (incluse inattive)
        CategoryResponse inactiveCategory = CategoryResponse.builder()
                .id(3L)
                .name("Inactive Category")
                .active(false)
                .build();

        List<CategoryResponse> allCategories = Arrays.asList(categoryResponse, inactiveCategory);
        when(categoryService.getAllCategories()).thenReturn(allCategories);

        // When & Then - testa endpoint GET /all
        mockMvc.perform(get("/api/categories/all"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].active").value(true))
                .andExpect(jsonPath("$[1].active").value(false));

        verify(categoryService).getAllCategories();
    }

    @Test
    void shouldGetCategoryByIdSuccessfully() throws Exception {
        // Given - categoria esistente
        when(categoryService.findById(1L)).thenReturn(Optional.of(categoryResponse));

        // When & Then - testa GET per ID specifico
        mockMvc.perform(get("/api/categories/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Electronics"));

        verify(categoryService).findById(1L);
    }

    @Test
    void shouldReturn404WhenCategoryNotFound() throws Exception {
        // Given - categoria non esistente
        when(categoryService.findById(999L)).thenReturn(Optional.empty());

        // When & Then - deve restituire 404
        mockMvc.perform(get("/api/categories/999"))
                .andExpect(status().isNotFound());

        verify(categoryService).findById(999L);
    }

    @Test
    void shouldSearchCategoriesByName() throws Exception {
        // Given - risultati ricerca
        when(categoryService.searchByName("elec")).thenReturn(Arrays.asList(categoryResponse));

        // When & Then - testa ricerca
        mockMvc.perform(get("/api/categories/search")
                        .param("name", "elec"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Electronics"));

        verify(categoryService).searchByName("elec");
    }

    @Test
    void shouldUpdateCategorySuccessfully() throws Exception {
        // Given - categoria aggiornata
        CategoryResponse updatedCategory = CategoryResponse.builder()
                .id(1L)
                .name("Updated Electronics")
                .description("Updated description")
                .active(false)
                .build();

        CategoryRequest updateRequest = CategoryRequest.builder()
                .name("Updated Electronics")
                .description("Updated description")
                .active(false)
                .build();

        when(categoryService.updateCategory(eq(1L), any(CategoryRequest.class))).thenReturn(updatedCategory);

        // When & Then - testa PUT
        mockMvc.perform(put("/api/categories/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.name").value("Updated Electronics"))
                .andExpect(jsonPath("$.description").value("Updated description"))
                .andExpect(jsonPath("$.active").value(false));

        verify(categoryService).updateCategory(eq(1L), any(CategoryRequest.class));
    }

    @Test
    void shouldDeactivateCategorySuccessfully() throws Exception {
        // Given - service che disattiva categoria
        doNothing().when(categoryService).deactivateCategory(1L);

        // When & Then - testa DELETE
        mockMvc.perform(delete("/api/categories/1"))
                .andExpect(status().isNoContent());  // Status 204 No Content

        verify(categoryService).deactivateCategory(1L);
    }

    @Test
    void shouldHandleServiceExceptionDuringCreation() throws Exception {
        // Given - service lancia eccezione per nome duplicato
        when(categoryService.createCategory(any(CategoryRequest.class)))
                .thenThrow(new IllegalArgumentException("Esiste già una categoria con nome: Electronics"));

        // When & Then - il GlobalExceptionHandler deve gestire l'errore
        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(categoryRequest)))
                .andExpect(status().isBadRequest());  // Il GlobalExceptionHandler converte in 400

        verify(categoryService).createCategory(any(CategoryRequest.class));
    }

    @Test
    void shouldValidateRequiredFields() throws Exception {
        // Given - richiesta con nome vuoto (violazione @NotBlank)
        CategoryRequest invalidRequest = CategoryRequest.builder()
                .name("")  // Nome vuoto non valido
                .description("Test")
                .build();

        // When & Then - deve rifiutare per validazione
        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        // Il service non deve essere chiamato se la validazione fallisce
        verify(categoryService, never()).createCategory(any(CategoryRequest.class));
    }
}