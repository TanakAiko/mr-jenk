package sn.dev.order_service.services;

import java.util.List;

import sn.dev.order_service.data.order.OrderDocument;
import sn.dev.order_service.data.order.OrderItemStatus;

public interface OrderService {

    List<OrderDocument> getOrdersForUser(String userId);

    List<OrderDocument> getOrdersForSeller(String sellerId);

    OrderDocument getOrderForUser(String userId, String orderId);

    OrderDocument checkout(String userId);

    OrderDocument cancelOrder(String userId, String orderId);

    OrderDocument redoOrderToCart(String userId, String orderId);

    OrderDocument updateOrderItemStatusForSeller(String sellerId, String orderId, String itemId,
            OrderItemStatus newStatus);
}
