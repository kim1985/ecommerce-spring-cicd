package com.myecom.service.command;

import com.myecom.dto.order.CreateOrderRequest;
import com.myecom.dto.order.OrderResponse;
import com.myecom.events.OrderCreatedEvent;
import com.myecom.exception.BusinessException;
import com.myecom.model.*;
import com.myecom.repository.*;
import com.myecom.service.CartService;
import com.myecom.service.mapper.OrderMapper;
import com.myecom.service.validation.OrderValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Test SEMPLICI per CreateOrderCommand - un test = una cosa sola
 */
@ExtendWith(MockitoExtension.class)
class CreateOrderCommandTest {

    @Mock private OrderRepository orderRepository;
    @Mock private UserRepository userRepository;
    @Mock private CartRepository cartRepository;
    @Mock private CartItemRepository cartItemRepository;
    @Mock private CartService cartService;
    @Mock private OrderMapper orderMapper;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private CreateOrderCommand createOrderCommand;

    private CreateOrderRequest request;

    @BeforeEach
    void setUp() {
        // Nessun validator - lista vuota per semplicità
        ReflectionTestUtils.setField(createOrderCommand, "validators", Collections.emptyList());

        request = new CreateOrderRequest();
        request.setShippingAddress("Via Test 123");
    }

    @Test
    void shouldFailWhenUserNotFound() {
        // Given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> createOrderCommand.init(999L, request).execute())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Utente non trovato");
    }

    @Test
    void shouldFailWhenCartNotFound() {
        // Given
        User user = mock(User.class);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(cartRepository.findByUser(user)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> createOrderCommand.init(1L, request).execute())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Carrello vuoto");
    }

    @Test
    void shouldCallEventPublisher() {
        // Given - setup minimale per arrivare fino alla pubblicazione evento
        User user = mock(User.class);
        Cart cart = mock(Cart.class);
        Order savedOrder = mock(Order.class);
        OrderResponse response = mock(OrderResponse.class);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(cartRepository.findByUser(user)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCart(cart)).thenReturn(Collections.emptyList());
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        when(orderMapper.toResponse(eq(savedOrder), any())).thenReturn(response);

        // When
        createOrderCommand.init(1L, request).execute();

        // Then - verifica che venga pubblicato un evento del tipo corretto
        verify(eventPublisher).publishEvent(any(OrderCreatedEvent.class));
    }

    @Test
    void shouldCallCartServiceClear() {
        // Given - setup minimale per arrivare fino al clear cart
        User user = mock(User.class);
        Cart cart = mock(Cart.class);
        Order savedOrder = mock(Order.class);
        OrderResponse response = mock(OrderResponse.class);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(cartRepository.findByUser(user)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCart(cart)).thenReturn(Collections.emptyList());
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        when(orderMapper.toResponse(eq(savedOrder), any())).thenReturn(response);

        // When
        createOrderCommand.init(1L, request).execute();

        // Then
        verify(cartService).clearCart(1L);
    }

    @Test
    void shouldReturnResponseFromMapper() {
        // Given
        User user = mock(User.class);
        Cart cart = mock(Cart.class);
        Order savedOrder = mock(Order.class);
        OrderResponse expectedResponse = OrderResponse.builder()
                .id(1L)
                .orderNumber("ORD-123")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(cartRepository.findByUser(user)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCart(cart)).thenReturn(Collections.emptyList());
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        when(orderMapper.toResponse(eq(savedOrder), any())).thenReturn(expectedResponse);

        // When
        OrderResponse result = createOrderCommand.init(1L, request).execute();

        // Then
        assertThat(result).isEqualTo(expectedResponse);
        assertThat(result.getOrderNumber()).isEqualTo("ORD-123");
    }
}