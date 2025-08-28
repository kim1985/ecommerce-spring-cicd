package com.myecom.service;

import com.myecom.dto.order.CreateOrderRequest;
import com.myecom.dto.order.OrderResponse;
import com.myecom.model.Order;
import com.myecom.model.User;
import com.myecom.repository.OrderRepository;
import com.myecom.repository.UserRepository;
import com.myecom.service.command.CreateOrderCommand;
import com.myecom.service.mapper.OrderMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test semplificato per OrderService che ora delega tutto al CreateOrderCommand
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private UserRepository userRepository;
    @Mock private OrderMapper orderMapper;
    @Mock private CreateOrderCommand createOrderCommand;

    @InjectMocks
    private OrderService orderService;

    private User testUser;
    private Order testOrder;
    private OrderResponse orderResponse;
    private CreateOrderRequest createOrderRequest;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .firstName("Test")
                .lastName("User")
                .build();

        testOrder = Order.builder()
                .id(1L)
                .orderNumber("ORD-123456")
                .user(testUser)
                .status(Order.OrderStatus.PENDING)
                .totalAmount(new BigDecimal("100.00"))
                .shippingAddress("Via Test 123")
                .build();

        orderResponse = OrderResponse.builder()
                .id(1L)
                .orderNumber("ORD-123456")
                .status("PENDING")
                .totalAmount(new BigDecimal("100.00"))
                .shippingAddress("Via Test 123")
                .build();

        createOrderRequest = new CreateOrderRequest();
        createOrderRequest.setShippingAddress("Via Test 123");
        createOrderRequest.setNotes("Test order");
    }

    @Test
    void shouldDelegateOrderCreationToCommand() {
        // Given - Command configurato per restituire risposta
        when(createOrderCommand.init(anyLong(), any(CreateOrderRequest.class))).thenReturn(createOrderCommand);
        when(createOrderCommand.execute()).thenReturn(orderResponse);

        // When
        OrderResponse result = orderService.createOrder(1L, createOrderRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getOrderNumber()).isEqualTo("ORD-123456");
        assertThat(result.getTotalAmount()).isEqualTo(new BigDecimal("100.00"));

        // Verifica delegazione al comando
        verify(createOrderCommand).init(1L, createOrderRequest);
        verify(createOrderCommand).execute();
    }

    @Test
    void shouldFindOrderById() {
        // Given
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderMapper.toResponse(testOrder)).thenReturn(orderResponse);

        // When
        Optional<OrderResponse> result = orderService.findById(1L);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(1L);
        assertThat(result.get().getOrderNumber()).isEqualTo("ORD-123456");

        verify(orderRepository).findById(1L);
        verify(orderMapper).toResponse(testOrder);
    }

    @Test
    void shouldReturnEmptyWhenOrderNotFound() {
        // Given
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        // When
        Optional<OrderResponse> result = orderService.findById(999L);

        // Then
        assertThat(result).isEmpty();
        verify(orderRepository).findById(999L);
    }

    @Test
    void shouldGetUserOrders() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(orderRepository.findByUserOrderByCreatedAtDesc(testUser)).thenReturn(List.of(testOrder));
        when(orderMapper.toResponse(testOrder)).thenReturn(orderResponse);

        // When
        List<OrderResponse> result = orderService.getUserOrders(1L);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getOrderNumber()).isEqualTo("ORD-123456");

        verify(userRepository).findById(1L);
        verify(orderRepository).findByUserOrderByCreatedAtDesc(testUser);
        verify(orderMapper).toResponse(testOrder);
    }

    @Test
    void shouldThrowExceptionWhenUserNotFoundForUserOrders() {
        // Given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        try {
            orderService.getUserOrders(999L);
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage()).isEqualTo("Utente non trovato");
        }

        verify(userRepository).findById(999L);
    }
}