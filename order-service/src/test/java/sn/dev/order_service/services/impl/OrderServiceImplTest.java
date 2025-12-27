package sn.dev.order_service.services.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import sn.dev.order_service.data.cart.CartDocument;
import sn.dev.order_service.data.cart.CartRepository;
import sn.dev.order_service.data.order.OrderDocument;
import sn.dev.order_service.data.order.OrderItemDocument;
import sn.dev.order_service.data.order.OrderRepository;
import sn.dev.order_service.data.order.OrderStatus;
import sn.dev.order_service.services.ProductServiceClient;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private ProductServiceClient productServiceClient;

    @InjectMocks
    private OrderServiceImpl orderService;

    @Test
    void testGetOrdersForUser() {
        String userId = "user-1";
        OrderDocument order = new OrderDocument();
        order.setId("order-1");
        when(orderRepository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of(order));

        List<OrderDocument> result = orderService.getOrdersForUser(userId);

        assertEquals(1, result.size());
        assertEquals("order-1", result.get(0).getId());
    }

    @Test
    void testCheckout_EmptyCart() {
        String userId = "user-1";
        CartDocument cart = new CartDocument();
        cart.setUserId(userId);
        cart.setItems(Collections.emptyList());

        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));

        assertThrows(IllegalStateException.class, () -> orderService.checkout(userId));
    }

    @Test
    void testCancelOrder_Success() {
        String userId = "user-1";
        String orderId = "order-1";
        OrderDocument order = new OrderDocument();
        order.setId(orderId);
        order.setUserId(userId);
        order.setStatus(OrderStatus.PENDING);
        
        OrderItemDocument item = new OrderItemDocument();
        item.setProductId("prod-1");
        item.setQuantity(2);
        order.setItems(List.of(item));

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(OrderDocument.class))).thenAnswer(i -> i.getArgument(0));

        OrderDocument result = orderService.cancelOrder(userId, orderId);

        assertEquals(OrderStatus.CANCELLED, result.getStatus());
        verify(productServiceClient).restoreQuantity("prod-1", 2);
    }

    @Test
    void testRedoOrderToCart() {
        String userId = "user-1";
        String orderId = "order-1";
        OrderDocument order = new OrderDocument();
        order.setId(orderId);
        order.setUserId(userId);
        
        OrderItemDocument item = new OrderItemDocument();
        item.setProductId("prod-1");
        item.setQuantity(2);
        item.setUnitPrice(10.0);
        order.setItems(List.of(item));

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.empty());
        
        orderService.redoOrderToCart(userId, orderId);

        verify(cartRepository).save(any(CartDocument.class));
    }
}
