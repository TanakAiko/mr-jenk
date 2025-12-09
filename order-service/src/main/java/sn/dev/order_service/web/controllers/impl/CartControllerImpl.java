package sn.dev.order_service.web.controllers.impl;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.RestController;
import sn.dev.order_service.data.cart.CartDocument;
import sn.dev.order_service.data.cart.CartItemDocument;
import sn.dev.order_service.services.CartService;
import sn.dev.order_service.web.controllers.CartController;
import sn.dev.order_service.web.dto.CartItemDto;
import sn.dev.order_service.web.dto.CartItemRequestDto;
import sn.dev.order_service.web.dto.CartResponseDto;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class CartControllerImpl implements CartController {

    private final CartService cartService;

    @Override
    public ResponseEntity<CartResponseDto> getCart() {
        String userId = getCurrentUserId();
        CartDocument cart = cartService.getOrCreateCart(userId);
        return ResponseEntity.ok(mapToDto(cart));
    }

    @Override
    public ResponseEntity<CartResponseDto> addItem(String productId, @Valid CartItemRequestDto requestDto) {
        String userId = getCurrentUserId();
        int quantity = requestDto.getQuantity();
        CartDocument cart = cartService.addItem(userId, productId, quantity);
        return ResponseEntity.ok(mapToDto(cart));
    }

    @Override
    public ResponseEntity<CartResponseDto> updateItemQuantity(String productId, @Valid CartItemRequestDto requestDto) {
        String userId = getCurrentUserId();
        int quantity = requestDto.getQuantity();
        CartDocument cart = cartService.updateItemQuantity(userId, productId, quantity);
        return ResponseEntity.ok(mapToDto(cart));
    }

    @Override
    public ResponseEntity<CartResponseDto> removeItem(String productId) {
        String userId = getCurrentUserId();
        CartDocument cart = cartService.removeItem(userId, productId);
        return ResponseEntity.ok(mapToDto(cart));
    }

    @Override
    public ResponseEntity<Void> clearCart() {
        String userId = getCurrentUserId();
        cartService.clearCart(userId);
        return ResponseEntity.noContent().build();
    }

    private String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            return jwt.getClaimAsString("userID");
        }
        throw new IllegalStateException("User ID not found in JWT");
    }

    private CartResponseDto mapToDto(CartDocument cart) {
        List<CartItemDto> itemDtos = cart.getItems().stream()
                .map(this::mapItemToDto)
                .collect(Collectors.toList());

        double totalPrice = itemDtos.stream()
                .mapToDouble(CartItemDto::subtotal)
                .sum();

        return new CartResponseDto(
                cart.getUserId(),
                itemDtos,
                totalPrice
        );
    }

    private CartItemDto mapItemToDto(CartItemDocument item) {
        double subtotal = item.getPriceSnapshot() * item.getQuantity();
        return new CartItemDto(
                item.getProductId(),
                item.getSellerId(),
                item.getProductName(),
                item.getPriceSnapshot(),
                item.getQuantity(),
                subtotal
        );
    }
}
