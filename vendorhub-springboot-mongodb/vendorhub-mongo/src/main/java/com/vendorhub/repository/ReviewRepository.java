package com.vendorhub.repository;

import com.vendorhub.document.Review;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface ReviewRepository extends MongoRepository<Review, String> {
    List<Review> findByRestaurantIdOrderByCreatedAtDesc(String restaurantId);
    List<Review> findByCustomerIdOrderByCreatedAtDesc(String customerId);
    boolean      existsByOrderId(String orderId);
    long         countByRestaurantId(String restaurantId);
}
