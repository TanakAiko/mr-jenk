package sn.dev.order_service.services;

import sn.dev.order_service.data.cart.CartDocument;

public interface CartService {

    CartDocument getOrCreateCart(String userId);

    CartDocument addItem(String userId, String productId, int quantity);

    CartDocument updateItemQuantity(String userId, String productId, int quantity);

    CartDocument removeItem(String userId, String productId);

    void clearCart(String userId);
}
