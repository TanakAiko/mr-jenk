package sn.dev.order_service.data.order;

import lombok.Data;

@Data
public class OrderItemDocument {

    private String productId;
    private String sellerId;
    private String productName;
    private double unitPrice;
    private int quantity;
    private double subtotal;
    private OrderItemStatus status;
    private String imageUrl;

}
