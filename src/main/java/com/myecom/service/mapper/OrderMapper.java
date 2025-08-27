package com.myecom.service.mapper;

import com.myecom.dto.order.OrderItemResponse;
import com.myecom.dto.order.OrderResponse;
import com.myecom.model.Order;
import com.myecom.model.OrderItem;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Mapper per convertire entità Order in DTO
 * Evita duplicazione di codice tra Service e Command
 */
@Component
public class OrderMapper {

    /**
     * Converte Order + OrderItems in OrderResponse completo
     * Usato dal CreateOrderCommand
     */
    public OrderResponse toResponse(Order order, List<OrderItem> orderItems) {
        List<OrderItemResponse> itemResponses = orderItems.stream()
                .map(this::toItemResponse)
                .toList();

        return OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus().name())
                .totalAmount(order.getTotalAmount())
                .shippingAddress(order.getShippingAddress())
                .notes(order.getNotes())
                .createdAt(order.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .items(itemResponses)
                .build();
    }

    /**
     * Converte Order in OrderResponse (carica items se presenti)
     * Usato dal OrderService per query
     */
    public OrderResponse toResponse(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus().name())
                .totalAmount(order.getTotalAmount())
                .shippingAddress(order.getShippingAddress())
                .notes(order.getNotes())
                .createdAt(order.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .items(order.getOrderItems() != null ?
                        order.getOrderItems().stream()
                                .map(this::toItemResponse)
                                .toList() :
                        List.of())
                .build();
    }

    /**
     * Converte OrderItem in OrderItemResponse
     */
    public OrderItemResponse toItemResponse(OrderItem item) {
        return OrderItemResponse.builder()
                .id(item.getId())
                .productId(item.getProduct().getId())
                .productName(item.getProduct().getName())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .totalPrice(item.getTotalPrice())
                .build();
    }
}
