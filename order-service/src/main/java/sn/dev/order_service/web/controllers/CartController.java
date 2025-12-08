package sn.dev.order_service.web.controllers;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sn.dev.order_service.web.dto.CartResponseDto;
import sn.dev.order_service.web.dto.CartItemRequestDto;

@RequestMapping("/api/cart")
public interface CartController {

    @GetMapping
    ResponseEntity<CartResponseDto> getCart();

    @PostMapping("/items/{productId}")
    ResponseEntity<CartResponseDto> addItem(@PathVariable String productId,
                                            @Valid @RequestBody CartItemRequestDto requestDto);

    @PutMapping("/items/{productId}")
    ResponseEntity<CartResponseDto> updateItemQuantity(@PathVariable String productId,
                                                       @Valid @RequestBody CartItemRequestDto requestDto);

    @DeleteMapping("/items/{productId}")
    ResponseEntity<CartResponseDto> removeItem(@PathVariable String productId);

    @DeleteMapping
    ResponseEntity<Void> clearCart();
}
