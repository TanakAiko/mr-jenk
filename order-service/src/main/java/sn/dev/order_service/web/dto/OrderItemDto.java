package sn.dev.order_service.web.dto;

public record OrderItemDto(
        String productId,
        String sellerId,
        String productName,
        double unitPrice,
        int quantity,
        double subtotal,
        String status,
        String imageUrl
) {}
