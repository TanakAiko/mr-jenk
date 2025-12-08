package sn.dev.order_service.web.dto;

public record CartItemDto(
        String productId,
        String sellerId,
        String productName,
        double priceSnapshot,
        int quantity,
        double subtotal
) {}
