package sn.dev.order_service.web.controllers.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import sn.dev.order_service.data.order.OrderDocument;
import sn.dev.order_service.data.order.OrderItemDocument;
import sn.dev.order_service.data.order.OrderItemStatus;
import sn.dev.order_service.data.order.OrderStatus;
import sn.dev.order_service.data.order.PaymentMode;
import sn.dev.order_service.services.OrderService;

@SpringBootTest
@AutoConfigureMockMvc
public class OrderControllerImplTest {

    @MockitoBean
    private OrderService orderService;

    @Autowired
    private MockMvc mockMvc;

    private Jwt fakeJwt() {
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("userID", "user-123")
                .build();
    }

    @Test
    @WithMockUser(username = "user-123", authorities = { "USER" })
    void testGetMyOrders() throws Exception {
        OrderDocument order = new OrderDocument();
        order.setId("order-1");
        order.setUserId("user-123");
        order.setStatus(OrderStatus.PENDING);
        order.setPaymentMode(PaymentMode.PAY_ON_DELIVERY);
        order.setTotalPrice(100.0);
        order.setCreatedAt(Instant.now());
        order.setUpdatedAt(Instant.now());

        OrderItemDocument item = new OrderItemDocument();
        item.setProductId("prod-1");
        item.setProductName("Test Product");
        item.setUnitPrice(50.0);
        item.setQuantity(2);
        item.setSubtotal(100.0);
        item.setStatus(OrderItemStatus.PENDING);
        order.setItems(List.of(item));

        when(orderService.getOrdersForUser(anyString())).thenReturn(List.of(order));

        mockMvc.perform(get("/api/orders")
                .with(request -> {
                    request.setAttribute("org.springframework.security.oauth2.jwt.Jwt", fakeJwt());
                    return request;
                }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("order-1"))
                .andExpect(jsonPath("$[0].totalPrice").value(100.0))
                .andExpect(jsonPath("$[0].items[0].productName").value("Test Product"));
        
        System.out.println("✅ ORDER/CONTROLLER : testGetMyOrders() passed successfully.");
    }

    @Test
    @WithMockUser(username = "user-123", authorities = { "USER" })
    void testCheckout() throws Exception {
        OrderDocument order = new OrderDocument();
        order.setId("order-new");
        order.setUserId("user-123");
        order.setStatus(OrderStatus.PENDING);
        order.setPaymentMode(PaymentMode.PAY_ON_DELIVERY);
        order.setTotalPrice(200.0);
        order.setCreatedAt(Instant.now());
        order.setUpdatedAt(Instant.now());

        when(orderService.checkout(anyString())).thenReturn(order);

        mockMvc.perform(post("/api/orders/checkout")
                .with(request -> {
                    request.setAttribute("org.springframework.security.oauth2.jwt.Jwt", fakeJwt());
                    return request;
                }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("order-new"))
                .andExpect(jsonPath("$.totalPrice").value(200.0));

        System.out.println("✅ ORDER/CONTROLLER : testCheckout() passed successfully.");
    }

    @Test
    @WithMockUser(username = "user-123", authorities = { "USER" })
    void testSearchOrders() throws Exception {
        OrderDocument order = new OrderDocument();
        order.setId("order-search");
        order.setUserId("user-123");
        order.setStatus(OrderStatus.PENDING);
        order.setPaymentMode(PaymentMode.PAY_ON_DELIVERY);
        order.setTotalPrice(50.0);
        order.setCreatedAt(Instant.now());
        order.setUpdatedAt(Instant.now());

        when(orderService.searchOrdersForUser(anyString(), anyString())).thenReturn(List.of(order));

        mockMvc.perform(get("/api/orders/search")
                .param("query", "test")
                .with(request -> {
                    request.setAttribute("org.springframework.security.oauth2.jwt.Jwt", fakeJwt());
                    return request;
                }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("order-search"));

        System.out.println("✅ ORDER/CONTROLLER : testSearchOrders() passed successfully.");
    }

    @Test
    @WithMockUser(username = "user-123", authorities = { "USER" })
    void testCancelOrder() throws Exception {
        OrderDocument order = new OrderDocument();
        order.setId("order-cancel");
        order.setUserId("user-123");
        order.setStatus(OrderStatus.CANCELLED);
        order.setPaymentMode(PaymentMode.PAY_ON_DELIVERY);
        order.setTotalPrice(100.0);
        order.setCreatedAt(Instant.now());
        order.setUpdatedAt(Instant.now());
        
        when(orderService.cancelOrder(anyString(), anyString())).thenReturn(order);

        mockMvc.perform(patch("/api/orders/order-cancel/cancel")
                .with(request -> {
                    request.setAttribute("org.springframework.security.oauth2.jwt.Jwt", fakeJwt());
                    return request;
                }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("order-cancel"))
                .andExpect(jsonPath("$.status").value("CANCELLED"));
        
        System.out.println("✅ ORDER/CONTROLLER : testCancelOrder() passed successfully.");
    }

    @Test
    @WithMockUser(username = "user-123", authorities = { "USER" })
    void testRedoToCart() throws Exception {
        OrderDocument order = new OrderDocument();
        order.setId("order-redo");
        order.setUserId("user-123");
        order.setStatus(OrderStatus.DELIVERED);
        order.setPaymentMode(PaymentMode.PAY_ON_DELIVERY);
        order.setTotalPrice(100.0);
        order.setCreatedAt(Instant.now());
        order.setUpdatedAt(Instant.now());
        
        when(orderService.redoOrderToCart(anyString(), anyString())).thenReturn(order);

        mockMvc.perform(post("/api/orders/order-redo/redo-to-cart")
                .with(request -> {
                    request.setAttribute("org.springframework.security.oauth2.jwt.Jwt", fakeJwt());
                    return request;
                }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("order-redo"));

        System.out.println("✅ ORDER/CONTROLLER : testRedoToCart() passed successfully.");
    }

    @Test
    @WithMockUser(username = "user-123", authorities = { "SELLER" })
    void testGetOrdersForSeller() throws Exception {
        OrderDocument order = new OrderDocument();
        order.setId("order-seller");
        order.setUserId("user-buyer");
        order.setStatus(OrderStatus.PENDING);
        order.setPaymentMode(PaymentMode.PAY_ON_DELIVERY);
        order.setTotalPrice(50.0);
        order.setCreatedAt(Instant.now());
        order.setUpdatedAt(Instant.now());
        
        OrderItemDocument item = new OrderItemDocument();
        item.setSellerId("user-123");
        item.setSubtotal(50.0);
        item.setStatus(OrderItemStatus.PENDING);
        order.setItems(List.of(item));

        when(orderService.getOrdersForSeller(anyString())).thenReturn(List.of(order));

        mockMvc.perform(get("/api/orders/seller")
                .with(request -> {
                    request.setAttribute("org.springframework.security.oauth2.jwt.Jwt", fakeJwt());
                    return request;
                }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("order-seller"));

        System.out.println("✅ ORDER/CONTROLLER : testGetOrdersForSeller() passed successfully.");
    }
    
    @Test
    @WithMockUser(username = "user-123", authorities = { "SELLER" })
    void testUpdateItemStatus() throws Exception {
        OrderDocument order = new OrderDocument();
        order.setId("order-update");
        order.setUserId("user-buyer");
        order.setStatus(OrderStatus.PENDING);
        order.setPaymentMode(PaymentMode.PAY_ON_DELIVERY);
        order.setTotalPrice(10.0);
        order.setCreatedAt(Instant.now());
        order.setUpdatedAt(Instant.now());
        
        OrderItemDocument item = new OrderItemDocument();
        item.setProductId("prod-1");
        item.setSellerId("user-123");
        item.setStatus(OrderItemStatus.SHIPPED);
        item.setSubtotal(10.0);
        order.setItems(List.of(item));

        when(orderService.updateOrderItemStatusForSeller(anyString(), anyString(), anyString(), any(OrderItemStatus.class)))
            .thenReturn(order);

        String jsonRequest = "{\"status\": \"SHIPPED\"}";

        mockMvc.perform(patch("/api/orders/order-update/items/prod-1/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest)
                .with(request -> {
                    request.setAttribute("org.springframework.security.oauth2.jwt.Jwt", fakeJwt());
                    return request;
                }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].status").value("SHIPPED"));

        System.out.println("✅ ORDER/CONTROLLER : testUpdateItemStatus() passed successfully.");
    }
}
