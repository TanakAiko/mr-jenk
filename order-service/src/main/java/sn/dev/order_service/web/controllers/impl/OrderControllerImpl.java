package sn.dev.order_service.web.controllers.impl;

import java.util.List;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import sn.dev.order_service.data.order.OrderDocument;
import sn.dev.order_service.data.order.OrderItemDocument;
import sn.dev.order_service.data.order.OrderItemStatus;
import sn.dev.order_service.services.OrderService;
import sn.dev.order_service.web.controllers.OrderController;
import sn.dev.order_service.web.dto.OrderItemDto;
import sn.dev.order_service.web.dto.OrderResponseDto;
import sn.dev.order_service.web.dto.UpdateOrderItemStatusRequestDto;

@RestController
@RequiredArgsConstructor
public class OrderControllerImpl implements OrderController {

    private final OrderService orderService;

    // private String getCurrentUserId() {
    //     Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    //     Jwt jwt = (Jwt) auth.getPrincipal();
    //     return jwt.getClaimAsString("userID");
    // }

    private String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            return jwt.getClaimAsString("userID");
        }
        throw new IllegalStateException("User ID not found in JWT");
    }

    private String getCurrentSellerId() {
        // In this simple model, sellerId is same as userID
        return getCurrentUserId();
    }

    private OrderResponseDto toDto(OrderDocument order) {
        List<OrderItemDto> items = order.getItems().stream()
                .map(this::toItemDto)
                .toList();

        return new OrderResponseDto(
                order.getId(),
                order.getUserId(),
                order.getStatus().name(),
                order.getPaymentMode().name(),
                order.getTotalPrice(),
                order.getCreatedAt(),
                order.getUpdatedAt(),
                items
        );
    }

    private OrderItemDto toItemDto(OrderItemDocument item) {
        return new OrderItemDto(
                item.getProductId(),
                item.getSellerId(),
                item.getProductName(),
                item.getUnitPrice(),
                item.getQuantity(),
                item.getSubtotal(),
                item.getStatus().name()
        );
    }

    @Override
    public ResponseEntity<OrderResponseDto> checkout() {
        String userId = getCurrentUserId();
        OrderDocument order = orderService.checkout(userId);
        return ResponseEntity.ok(toDto(order));
    }

    @Override
    public ResponseEntity<List<OrderResponseDto>> getMyOrders() {
        String userId = getCurrentUserId();
        List<OrderResponseDto> orders = orderService.getOrdersForUser(userId)
                .stream()
                .map(this::toDto)
                .toList();
        return ResponseEntity.ok(orders);
    }

    @Override
    public ResponseEntity<OrderResponseDto> getOrder(String orderId) {
        String userId = getCurrentUserId();
        OrderDocument order = orderService.getOrderForUser(userId, orderId);
        return ResponseEntity.ok(toDto(order));
    }

    @Override
    public ResponseEntity<OrderResponseDto> cancelOrder(String orderId) {
        String userId = getCurrentUserId();
        OrderDocument order = orderService.cancelOrder(userId, orderId);
        return ResponseEntity.ok(toDto(order));
    }

    @Override
    public ResponseEntity<OrderResponseDto> redoToCart(String orderId) {
        String userId = getCurrentUserId();
        OrderDocument order = orderService.redoOrderToCart(userId, orderId);
        return ResponseEntity.ok(toDto(order));
    }

    @Override
    public ResponseEntity<List<OrderResponseDto>> getOrdersForSeller() {
        String sellerId = getCurrentSellerId();
        List<OrderResponseDto> orders = orderService.getOrdersForSeller(sellerId)
                .stream()
                .map(this::toDto)
                .toList();
        return ResponseEntity.ok(orders);
    }

    @Override
    public ResponseEntity<OrderResponseDto> updateItemStatus(String orderId,
                                                             String itemId,
                                                             @Valid UpdateOrderItemStatusRequestDto requestDto) {
        String sellerId = getCurrentSellerId();
        String statusValue = requestDto.getStatus();

        OrderItemStatus status;
        try {
            status = OrderItemStatus.valueOf(statusValue);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid order item status: " + statusValue);
        }

        OrderDocument order = orderService.updateOrderItemStatusForSeller(sellerId, orderId, itemId, status);
        return ResponseEntity.ok(toDto(order));
    }
}
