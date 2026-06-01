package com.vendorhub.repository;

import com.vendorhub.document.MenuItem;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface MenuItemRepository extends MongoRepository<MenuItem, String> {

    List<MenuItem> findByRestaurantIdOrderByCategoryAscBestsellerDesc(String restaurantId);

    List<MenuItem> findByRestaurantIdAndAvailableTrueOrderByCategoryAscBestsellerDesc(String restaurantId);

    List<MenuItem> findByRestaurantIdAndCategory(String restaurantId, String category);

    List<MenuItem> findByRestaurantIdAndAvailableTrueAndVegTrue(String restaurantId);

    void deleteByRestaurantId(String restaurantId);
}
