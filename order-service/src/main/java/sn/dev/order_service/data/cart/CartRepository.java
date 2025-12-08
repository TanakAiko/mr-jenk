package sn.dev.order_service.data.cart;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface CartRepository extends MongoRepository<CartDocument, String> {

    Optional<CartDocument> findByUserId(String userId);

    void deleteByUserId(String userId);
}
