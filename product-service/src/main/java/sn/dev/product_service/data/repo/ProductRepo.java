package sn.dev.product_service.data.repo;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import sn.dev.product_service.data.entities.Product;

public interface ProductRepo extends MongoRepository<Product, String> {
    List<Product> findByUserId(String userId);
    void deleteByUserId(String userId);
    
}
