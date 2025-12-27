package sn.dev.product_service.services.impl;

import java.util.List;

import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import sn.dev.product_service.data.entities.Product;
import sn.dev.product_service.data.repo.ProductRepo;
import sn.dev.product_service.services.ProductService;

@Service
@AllArgsConstructor
public class ProductServiceImpl implements ProductService {
    private final ProductRepo productRepo;

    @Override
    public Product create(Product product) {
        return productRepo.save(product);
    }

    @Override
    public List<Product> getAll() {
        return productRepo.findAll();
    }

    @Override
    public List<Product> search(String query) {
        return productRepo.findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(query, query);
    }

    @Override
    public Product getById(String id) {
        return productRepo.findById(id)
                .orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found with id: " + id));
    }

    @Override
    public List<Product> getByUserId(String userId) {
        return productRepo.findByUserId(userId);
    }

    @Override
    public Product update(Product product) {
        return productRepo.save(product);
    }

    @Override
    public void delete(Product product) {
        productRepo.delete(product);
    }

    @Override
    public void deleteByUserId(String userId) {
        productRepo.deleteByUserId(userId);
    }

    @Override
    public void reduceQuantity(String id, int quantity) {
        Product product = getById(id);
        if (product.getQuantity() < quantity) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient quantity for product: " + id);
        }
        product.setQuantity(product.getQuantity() - quantity);
        productRepo.save(product);
    }

    @Override
    public void restoreQuantity(String id, int quantity) {
        Product product = getById(id);
        product.setQuantity(product.getQuantity() + quantity);
        productRepo.save(product);
    }
}
