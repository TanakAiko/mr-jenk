package sn.dev.order_service.data.order;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@Document(collection = "orders")
public class OrderDocument {

    @Id
    private String id;

    private String userId;
    private OrderStatus status;
    private PaymentMode paymentMode;
    private List<OrderItemDocument> items = new ArrayList<>();
    private double totalPrice;
    private Instant createdAt;
    private Instant updatedAt;

}
