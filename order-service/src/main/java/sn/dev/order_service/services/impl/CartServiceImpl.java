package sn.dev.order_service.services.impl;

import java.time.Instant;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import sn.dev.order_service.data.Product;
import sn.dev.order_service.data.cart.CartDocument;
import sn.dev.order_service.data.cart.CartItemDocument;
import sn.dev.order_service.data.cart.CartRepository;
import sn.dev.order_service.services.CartService;
import sn.dev.order_service.services.ProductServiceClient;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final ProductServiceClient productServiceClient;

    @Override
    public CartDocument getOrCreateCart(String userId) {
        return cartRepository.findByUserId(userId)
                .orElseGet(() -> {
                    CartDocument cart = new CartDocument();
                    cart.setUserId(userId);
                    cart.setCreatedAt(Instant.now());
                    cart.setUpdatedAt(Instant.now());
                    return cartRepository.save(cart);
                });
    }

    @Override
    public CartDocument addItem(String userId, String productId, int quantity) {
        CartDocument cart = getOrCreateCart(userId);
        boolean found = false;
        for (CartItemDocument item : cart.getItems()) {
            if (item.getProductId().equals(productId)) {
                item.setQuantity(item.getQuantity() + quantity);
                item.setUpdatedAt(Instant.now());
                found = true;
                break;
            }
        }
        if (!found) {
            Product product = productServiceClient.getProductById(productId);
            
            System.out.println("Adding product to cart: " + product);

            CartItemDocument newItem = new CartItemDocument();
            newItem.setProductId(productId);
            newItem.setQuantity(quantity);
            newItem.setPriceSnapshot(product.getPrice());
            newItem.setProductName(product.getName());
            if (product.getImages() != null && !product.getImages().isEmpty()) {
                newItem.setImageUrl(product.getImages().get(0).getImageUrl());
            }
            newItem.setSellerId(product.getUserId());
            newItem.setCreatedAt(Instant.now());
            newItem.setUpdatedAt(Instant.now());
            cart.getItems().add(newItem);

            System.out.println("New cart item added: " + newItem);
        }
        cart.setUpdatedAt(Instant.now());
        return cartRepository.save(cart);
    }

    @Override
    public CartDocument updateItemQuantity(String userId, String productId, int quantity) {
        CartDocument cart = getOrCreateCart(userId);
        cart.getItems().removeIf(item -> {
            if (item.getProductId().equals(productId) && quantity <= 0) {
                return true;
            }
            if (item.getProductId().equals(productId)) {
                item.setQuantity(quantity);
                item.setUpdatedAt(Instant.now());
            }
            return false;
        });
        cart.setUpdatedAt(Instant.now());
        return cartRepository.save(cart);
    }

    @Override
    public CartDocument removeItem(String userId, String productId) {
        CartDocument cart = getOrCreateCart(userId);
        cart.getItems().removeIf(item -> item.getProductId().equals(productId));
        cart.setUpdatedAt(Instant.now());
        return cartRepository.save(cart);
    }

    @Override
    public void clearCart(String userId) {
        cartRepository.deleteByUserId(userId);
    }
}
