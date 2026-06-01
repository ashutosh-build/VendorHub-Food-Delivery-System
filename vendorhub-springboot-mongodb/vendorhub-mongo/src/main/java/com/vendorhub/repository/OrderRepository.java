package com.vendorhub.repository;

import com.vendorhub.document.Order;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import java.util.List;

public interface OrderRepository extends MongoRepository<Order, String> {

    List<Order> findByCustomerIdOrderByCreatedAtDesc(String customerId);

    List<Order> findByRestaurantIdOrderByCreatedAtDesc(String restaurantId);

    List<Order> findByDeliveryPartnerIdOrderByCreatedAtDesc(String partnerId);

    // Available for delivery — status READY and no partner assigned
    @Query("{ 'status': 'READY', 'deliveryPartnerId': null }")
    List<Order> findAvailableForDelivery();

    long countByDeliveryPartnerIdAndStatus(String partnerId, Order.OrderStatus status);

    long countByRestaurantId(String restaurantId);

    @Query("{ 'restaurantId': ?0, 'status': 'DELIVERED' }")
    List<Order> findDeliveredByRestaurant(String restaurantId);
}
