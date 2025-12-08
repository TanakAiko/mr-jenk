package sn.dev.order_service.data.cart;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@Document(collection = "carts")
public class CartDocument {

    @Id
    private String id;

    private String userId;
    private List<CartItemDocument> items = new ArrayList<>();
    private Instant createdAt;
    private Instant updatedAt;

    public double getTotalAmount() {
        return items.stream()
                .mapToDouble(CartItemDocument::getTotalPrice)
                .sum();
    }
}
