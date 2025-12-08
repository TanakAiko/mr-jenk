package sn.dev.order_service.web.controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import sn.dev.order_service.data.order.OrderItemStatus;
import sn.dev.order_service.web.dto.OrderResponseDto;

@RequestMapping("/api/orders")
public interface OrderController {

    @PostMapping("/checkout")
    ResponseEntity<OrderResponseDto> checkout();

    @GetMapping
    ResponseEntity<List<OrderResponseDto>> getMyOrders();

    @GetMapping("/{orderId}")
    ResponseEntity<OrderResponseDto> getOrder(@PathVariable String orderId);

    @PatchMapping("/{orderId}/cancel")
    ResponseEntity<OrderResponseDto> cancelOrder(@PathVariable String orderId);

    @PostMapping("/{orderId}/redo-to-cart")
    ResponseEntity<OrderResponseDto> redoToCart(@PathVariable String orderId);

    @GetMapping("/seller")
    ResponseEntity<List<OrderResponseDto>> getOrdersForSeller();

    @PatchMapping("/{orderId}/items/{itemId}/status")
    ResponseEntity<OrderResponseDto> updateItemStatus(@PathVariable String orderId,
                                                      @PathVariable String itemId,
                                                      @RequestParam OrderItemStatus status);
}
