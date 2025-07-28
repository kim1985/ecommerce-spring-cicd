package com.myecom.controller;

import com.myecom.dto.cart.CartItemRequest;
import com.myecom.dto.cart.CartItemResponse;
import com.myecom.dto.cart.CartResponse;
import com.myecom.service.CartService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class CartControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private CartService cartService;

    @InjectMocks
    private CartController cartController;

    private CartItemRequest cartItemRequest;
    private CartResponse cartResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(cartController).build();

        cartItemRequest = CartItemRequest.builder()
                .productId(1L)
                .quantity(2)
                .build();

        CartItemResponse cartItem = CartItemResponse.builder()
                .id(1L)
                .productId(1L)
                .productName("iPhone 15")
                .unitPrice(new BigDecimal("1299.99"))
                .productImageUrl("http://example.com/iphone.jpg")
                .quantity(2)
                .totalPrice(new BigDecimal("2599.98"))
                .productInStock(true)
                .build();

        cartResponse = CartResponse.builder()
                .id(1L)
                .items(Arrays.asList(cartItem))
                .totalAmount(new BigDecimal("2599.98"))
                .totalItems(2)
                .updatedAt("2024-01-15T10:30:00")
                .build();
    }

    @Test
    void shouldGetCart() throws Exception {
        // Given
        when(cartService.getCart(1L)).thenReturn(cartResponse);

        // When & Then
        mockMvc.perform(get("/api/cart/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items[0].productName").value("iPhone 15"))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.totalAmount").value(2599.98))
                .andExpect(jsonPath("$.totalItems").value(2));
    }

    @Test
    void shouldAddToCart() throws Exception {
        // Given
        when(cartService.addToCart(eq(1L), any(CartItemRequest.class))).thenReturn(cartResponse);

        // When & Then
        mockMvc.perform(post("/api/cart/1/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cartItemRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items[0].productId").value(1))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.totalAmount").value(2599.98));
    }

    @Test
    void shouldRemoveFromCart() throws Exception {
        // Given - carrello vuoto dopo rimozione
        CartResponse emptyCart = CartResponse.builder()
                .id(1L)
                .items(Arrays.asList())
                .totalAmount(BigDecimal.ZERO)
                .totalItems(0)
                .build();

        when(cartService.removeFromCart(1L, 1L)).thenReturn(emptyCart);

        // When & Then
        mockMvc.perform(delete("/api/cart/1/product/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.totalAmount").value(0))
                .andExpect(jsonPath("$.totalItems").value(0));
    }

    @Test
    void shouldClearCart() throws Exception {
        // Given
        doNothing().when(cartService).clearCart(1L);

        // When & Then
        mockMvc.perform(delete("/api/cart/1/clear"))
                .andExpect(status().isOk());

        // Verifica che il servizio sia stato chiamato
        verify(cartService).clearCart(1L);
    }

    @Test
    @Disabled("Disabilitato temporaneamente - da sistemare con nuova gestione errori")
    void shouldHandleServiceException() throws Exception {
        // Given
        when(cartService.addToCart(eq(1L), any(CartItemRequest.class)))
                .thenThrow(new IllegalArgumentException("Prodotto non trovato"));

        // When & Then
        mockMvc.perform(post("/api/cart/1/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cartItemRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Richiesta non valida: Prodotto non trovato"));
    }

    @Test
    @Disabled("Disabilitato temporaneamente - da sistemare con nuova gestione errori")
    void shouldHandleUserNotFound() throws Exception {
        // Given
        when(cartService.getCart(999L))
                .thenThrow(new IllegalArgumentException("Utente non trovato"));

        // When & Then
        mockMvc.perform(get("/api/cart/999"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Richiesta non valida: Utente non trovato"));
    }

    @Test
    @Disabled("Disabilitato temporaneamente - da sistemare con nuova gestione errori")
    void shouldHandleInvalidCartRequest() throws Exception {
        // Given
        when(cartService.addToCart(eq(1L), any(CartItemRequest.class)))
                .thenThrow(new IllegalArgumentException("Quantità non disponibile"));

        // When & Then
        mockMvc.perform(post("/api/cart/1/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cartItemRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Richiesta non valida: Quantità non disponibile"));
    }
}