package sn.dev.order_service.services.impl;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import sn.dev.order_service.data.cart.CartDocument;
import sn.dev.order_service.data.cart.CartItemDocument;
import sn.dev.order_service.data.cart.CartRepository;
import sn.dev.order_service.data.order.OrderDocument;
import sn.dev.order_service.data.order.OrderItemDocument;
import sn.dev.order_service.data.order.OrderItemStatus;
import sn.dev.order_service.data.order.OrderRepository;
import sn.dev.order_service.data.order.OrderStatus;
import sn.dev.order_service.data.order.PaymentMode;
import sn.dev.order_service.services.ProductServiceClient;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements sn.dev.order_service.services.OrderService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final ProductServiceClient productServiceClient;

    @Override
    public List<OrderDocument> getOrdersForUser(String userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    public List<OrderDocument> getOrdersForSeller(String sellerId) {
        return orderRepository.findByItemsSellerIdOrderByCreatedAtDesc(sellerId);
    }

    @Override
    public OrderDocument getOrderForUser(String userId, String orderId) {
        if (orderId == null) {
            throw new IllegalArgumentException("orderId must not be null");
        }
        
        OrderDocument order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        if (!order.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Access denied to this order");
        }
        return order;
    }

    @Override
    public OrderDocument checkout(String userId) {
        CartDocument cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("Cart is empty"));

        if (cart.getItems().isEmpty()) {
            throw new IllegalStateException("Cart is empty");
        }

        OrderDocument order = new OrderDocument();
        order.setId(UUID.randomUUID().toString());
        order.setUserId(userId);
        order.setPaymentMode(PaymentMode.PAY_ON_DELIVERY);
        order.setStatus(OrderStatus.PENDING);
        order.setCreatedAt(Instant.now());
        order.setUpdatedAt(Instant.now());

        double total = 0.0;
        // List to keep track of items for which quantity was successfully reduced
        java.util.List<CartItemDocument> processedItems = new java.util.ArrayList<>();

        try {
            for (CartItemDocument cartItem : cart.getItems()) {
                // Reduce quantity in product service
                productServiceClient.reduceQuantity(cartItem.getProductId(), cartItem.getQuantity());
                processedItems.add(cartItem);

                OrderItemDocument item = new OrderItemDocument();
                item.setProductId(cartItem.getProductId());
                item.setSellerId(cartItem.getSellerId());
                item.setProductName(cartItem.getProductName());
                item.setUnitPrice(cartItem.getPriceSnapshot());
                item.setQuantity(cartItem.getQuantity());
                item.setSubtotal(cartItem.getPriceSnapshot() * cartItem.getQuantity());
                item.setStatus(OrderItemStatus.PENDING);
                item.setImageUrl(cartItem.getImageUrl());
                total += item.getSubtotal();
                order.getItems().add(item);
            }
        } catch (Exception e) {
            // Compensation: Restore quantity for items that were already processed
            for (CartItemDocument processed : processedItems) {
                try {
                    productServiceClient.restoreQuantity(processed.getProductId(), processed.getQuantity());
                } catch (Exception ex) {
                    // Log error: Failed to compensate transaction
                    System.err.println("CRITICAL: Failed to restore quantity for product " + processed.getProductId() + " during checkout rollback.");
                }
            }
            throw new IllegalStateException("Checkout failed due to product service error: " + e.getMessage(), e);
        }

        order.setTotalPrice(total);

        OrderDocument saved = orderRepository.save(order);

        cartRepository.deleteByUserId(userId);
        return saved;
    }

    @Override
    public OrderDocument cancelOrder(String userId, String orderId) {
        if (orderId == null) {
            throw new IllegalArgumentException("orderId must not be null");
        }

        OrderDocument order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        if (!order.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Access denied to this order");
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException("Only pending orders can be cancelled");
        }

        // Restore quantities
        for (OrderItemDocument item : order.getItems()) {
            try {
                productServiceClient.restoreQuantity(item.getProductId(), item.getQuantity());
            } catch (Exception e) {
                System.err.println("Failed to restore quantity for product " + item.getProductId() + ": " + e.getMessage());
                // We continue to cancel the order even if restore fails, or we could throw exception.
                // For now, we proceed but log the error.
            }
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.getItems().forEach(i -> i.setStatus(OrderItemStatus.CANCELLED));
        order.setUpdatedAt(Instant.now());
        return orderRepository.save(order);
    }

    @Override
    public OrderDocument redoOrderToCart(String userId, String orderId) {
        if (orderId == null) {
            throw new IllegalArgumentException("orderId must not be null");
        }

        OrderDocument order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        if (!order.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Access denied to this order");
        }

        CartDocument cart = cartRepository.findByUserId(userId)
                .orElseGet(() -> {
                    CartDocument c = new CartDocument();
                    c.setUserId(userId);
                    c.setCreatedAt(Instant.now());
                    c.setUpdatedAt(Instant.now());
                    return c;
                });

        cart.getItems().clear();
        order.getItems().forEach(orderItem -> {
            CartItemDocument cartItem = new CartItemDocument();
            cartItem.setProductId(orderItem.getProductId());
            cartItem.setSellerId(orderItem.getSellerId());
            cartItem.setProductName(orderItem.getProductName());
            cartItem.setPriceSnapshot(orderItem.getUnitPrice());
            cartItem.setQuantity(orderItem.getQuantity());
            cartItem.setImageUrl(orderItem.getImageUrl());
            cartItem.setCreatedAt(Instant.now());
            cartItem.setUpdatedAt(Instant.now());
            cart.getItems().add(cartItem);
        });
        cart.setUpdatedAt(Instant.now());
        cartRepository.save(cart);

        return order;
    }

    @Override
    public OrderDocument updateOrderItemStatusForSeller(String sellerId, String orderId, String itemId,
            OrderItemStatus newStatus) {
        if (orderId == null) {
            throw new IllegalArgumentException("orderId must not be null");
        }

        OrderDocument order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        boolean found = false;
        for (OrderItemDocument item : order.getItems()) {
            if (item.getProductId().equals(itemId) && item.getSellerId().equals(sellerId)) {
                item.setStatus(newStatus);
                found = true;
            }
        }
        if (!found) {
            throw new IllegalArgumentException("Order item not found or does not belong to this seller");
        }

        // Update overall order status based on all item statuses
        boolean allPending = order.getItems().stream()
                .allMatch(i -> i.getStatus() == OrderItemStatus.PENDING);
        boolean allConfirmed = order.getItems().stream()
                .allMatch(i -> i.getStatus() == OrderItemStatus.CONFIRMED);
        boolean allShipped = order.getItems().stream()
                .allMatch(i -> i.getStatus() == OrderItemStatus.SHIPPED);
        boolean allDelivered = order.getItems().stream()
                .allMatch(i -> i.getStatus() == OrderItemStatus.DELIVERED);

        if (allDelivered) {
            order.setStatus(OrderStatus.DELIVERED);
        } else if (allShipped) {
            order.setStatus(OrderStatus.SHIPPED);
        } else if (allConfirmed) {
            order.setStatus(OrderStatus.CONFIRMED);
        } else if (allPending) {
            order.setStatus(OrderStatus.PENDING);
        }

        order.setUpdatedAt(Instant.now());
        return orderRepository.save(order);
    }
}
