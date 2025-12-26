package sn.dev.product_service.services;

import java.util.List;

import sn.dev.product_service.data.entities.Product;

public interface ProductService {
    Product create(Product product);

    Product update(Product product);

    Product getById(String id);

    List<Product> getByUserId(String userId);

    List<Product> getAll();

    List<Product> search(String query);

    void delete(Product product);

    void deleteByUserId(String userId);

    void reduceQuantity(String id, int quantity);

    void restoreQuantity(String id, int quantity);
}
