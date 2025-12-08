package sn.dev.order_service.data.order;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface OrderRepository extends MongoRepository<OrderDocument, String> {

    List<OrderDocument> findByUserIdOrderByCreatedAtDesc(String userId);

    List<OrderDocument> findByItemsSellerIdOrderByCreatedAtDesc(String sellerId);
}
