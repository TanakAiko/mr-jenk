package sn.dev.order_service.data.cart;

import java.time.Instant;

import lombok.Data;

@Data
public class CartItemDocument {

    private String productId;
    private String sellerId;
    private String productName;
    private double priceSnapshot;
    private int quantity;
    private String imageUrl;
    private Instant createdAt;
    private Instant updatedAt;

    public double getTotalPrice() {
        return priceSnapshot * quantity;
    }
}
