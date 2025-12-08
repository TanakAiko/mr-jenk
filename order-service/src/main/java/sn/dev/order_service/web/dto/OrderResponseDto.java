package sn.dev.order_service.web.dto;

import java.time.Instant;
import java.util.List;

public record OrderResponseDto(
        String id,
        String userId,
        String status,
        String paymentMode,
        double totalPrice,
        Instant createdAt,
        Instant updatedAt,
        List<OrderItemDto> items
) {}
