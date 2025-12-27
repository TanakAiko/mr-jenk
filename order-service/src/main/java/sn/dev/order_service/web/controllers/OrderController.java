package sn.dev.order_service.web.controllers;

import java.util.List;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import sn.dev.order_service.web.dto.UpdateOrderItemStatusRequestDto;
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
    @PreAuthorize("hasAuthority('SELLER')")
    ResponseEntity<List<OrderResponseDto>> getOrdersForSeller();

    @PatchMapping("/{orderId}/items/{itemId}/status")
    @PreAuthorize("hasAuthority('SELLER')")
    ResponseEntity<OrderResponseDto> updateItemStatus(@PathVariable String orderId,
                                                      @PathVariable String itemId,
                                                      @Valid @RequestBody UpdateOrderItemStatusRequestDto requestDto);

    @GetMapping("/search")
    ResponseEntity<List<OrderResponseDto>> searchOrders(@RequestParam String query);

    @GetMapping("/seller/search")
    @PreAuthorize("hasAuthority('SELLER')")
    ResponseEntity<List<OrderResponseDto>> searchSellerOrders(@RequestParam String query);
}
