package com.vendorhub.repository;

import com.vendorhub.document.Restaurant;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import java.util.List;
import java.util.Optional;

public interface RestaurantRepository extends MongoRepository<Restaurant, String> {

    Optional<Restaurant> findByOwnerId(String ownerId);

    List<Restaurant> findByActiveTrueOrderByRatingDesc();

    List<Restaurant> findByActiveTrueAndOpenTrueOrderByRatingDesc();

    List<Restaurant> findByActiveTrueAndCuisineContainingIgnoreCaseOrderByRatingDesc(String cuisine);

    @Query("{ 'active': true, $or: [ " +
           "  { 'name':    { $regex: ?0, $options: 'i' } }, " +
           "  { 'cuisine': { $regex: ?0, $options: 'i' } }, " +
           "  { 'area':    { $regex: ?0, $options: 'i' } } " +
           "] }")
    List<Restaurant> search(String query);
}
