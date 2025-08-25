package com.myecom.service;

import com.myecom.dto.order.CreateOrderRequest;
import com.myecom.exception.BusinessException;
import com.myecom.model.Cart;
import com.myecom.model.CartItem;
import com.myecom.model.User;
import com.myecom.repository.CartItemRepository;
import com.myecom.repository.CartRepository;
import com.myecom.repository.OrderRepository;
import com.myecom.repository.UserRepository;
import com.myecom.service.validation.OrderValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Test semplici per verificare che OrderService usi correttamente i Validator
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrderServiceStrategyPatternTest {

    @Mock private OrderRepository orderRepository;
    @Mock private UserRepository userRepository;
    @Mock private CartRepository cartRepository;
    @Mock private CartItemRepository cartItemRepository;
    @Mock private CartService cartService;
    @Mock private ApplicationEventPublisher eventPublisher;

    @Mock private OrderValidator validator1;
    @Mock private OrderValidator validator2;

    @InjectMocks
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        // Setup base solo quando serve - non mock inutili
        User user = mock(User.class);
        Cart cart = mock(Cart.class);
        List<CartItem> items = List.of(mock(CartItem.class));

        // Configura solo il necessario per i test
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(cartRepository.findByUser(user)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCart(cart)).thenReturn(items);

        // Inject validators nella lista
        ReflectionTestUtils.setField(orderService, "validators", List.of(validator1, validator2));
    }

    @Test
    void shouldExecuteEnabledValidators() {
        // Given - entrambi i validator abilitati
        when(validator1.isEnabled()).thenReturn(true);
        when(validator1.getOrder()).thenReturn(10);

        when(validator2.isEnabled()).thenReturn(true);
        when(validator2.getOrder()).thenReturn(20);

        // Fai fallire validator2 per non creare ordine reale
        doThrow(new BusinessException("Test validation failed"))
                .when(validator2).validate(any(), any());

        CreateOrderRequest request = new CreateOrderRequest();
        request.setShippingAddress("Via Test 123");

        // When & Then - deve chiamare entrambi
        assertThrows(BusinessException.class,
                () -> orderService.createOrder(1L, request));

        verify(validator1).validate(eq(1L), any());
        verify(validator2).validate(eq(1L), any());
    }

    @Test
    void shouldSkipDisabledValidators() {
        // Given - validator1 disabilitato, validator2 abilitato
        when(validator1.isEnabled()).thenReturn(false);

        when(validator2.isEnabled()).thenReturn(true);
        when(validator2.getOrder()).thenReturn(20);
        // Fai fallire validator2 per fermare l'esecuzione
        doThrow(new BusinessException("Test validation failed"))
                .when(validator2).validate(eq(1L), any());

        CreateOrderRequest request = new CreateOrderRequest();
        request.setShippingAddress("Via Test 123");

        // When & Then
        assertThrows(BusinessException.class,
                () -> orderService.createOrder(1L, request));

        // Verifica: validator1 non chiamato (disabilitato), validator2 chiamato
        verify(validator1, never()).validate(any(), any());
        verify(validator2).validate(eq(1L), any());
    }

    @Test
    void shouldStopAtFirstFailure() {
        // Given - validator1 fallisce
        when(validator1.isEnabled()).thenReturn(true);
        when(validator1.getOrder()).thenReturn(10);
        when(validator2.isEnabled()).thenReturn(true);
        when(validator2.getOrder()).thenReturn(20);

        // validator1 fallisce
        doThrow(new BusinessException("First validator failed"))
                .when(validator1).validate(any(), any());

        CreateOrderRequest request = new CreateOrderRequest();
        request.setShippingAddress("Via Test 123");

        // When & Then
        assertThrows(BusinessException.class,
                () -> orderService.createOrder(1L, request));

        // validator1 chiamato, validator2 no
        verify(validator1).validate(eq(1L), any());
        verify(validator2, never()).validate(any(), any());
    }

    @Test
    void shouldExecuteInCorrectOrder() {
        // Given - validator2 ha ordine più basso (deve essere eseguito prima)
        when(validator1.isEnabled()).thenReturn(true);
        when(validator1.getOrder()).thenReturn(20); // Secondo

        when(validator2.isEnabled()).thenReturn(true);
        when(validator2.getOrder()).thenReturn(10); // Primo

        // validator2 fallisce per fermare l'esecuzione
        doThrow(new BusinessException("Second validator failed"))
                .when(validator2).validate(any(), any());

        CreateOrderRequest request = new CreateOrderRequest();
        request.setShippingAddress("Via Test 123");

        // When & Then
        assertThrows(BusinessException.class,
                () -> orderService.createOrder(1L, request));

        // Verifica ordine di esecuzione
        var inOrder = inOrder(validator2, validator1);
        inOrder.verify(validator2).validate(eq(1L), any()); // Primo
        inOrder.verify(validator1, never()).validate(any(), any()); // Non eseguito
    }
}