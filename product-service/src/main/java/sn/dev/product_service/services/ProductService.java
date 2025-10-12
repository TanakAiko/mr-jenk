package sn.dev.product_service.services;

import java.util.List;

import sn.dev.product_service.data.entities.Product;

public interface ProductService {
    Product create(Product product);

    Product update(Product product);

    Product getById(String id);

    List<Product> getByUserId(String userId);

    List<Product> getAll();

    void delete(Product product);

    void deleteByUserId(String userId);
}
