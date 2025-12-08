package sn.dev.order_service.web.dto;

import java.util.List;

public record CartResponseDto(
        String userId,
        List<CartItemDto> items,
        double totalPrice
) {}
