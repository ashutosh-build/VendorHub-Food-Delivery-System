package com.vendorhub.controller;

import com.vendorhub.document.*;
import com.vendorhub.exception.ApiException;
import com.vendorhub.repository.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.*;

@RestController
@RequestMapping("/api/restaurants")
@RequiredArgsConstructor
public class RestaurantController {

    private final RestaurantRepository restRepo;
    private final MenuItemRepository   menuRepo;
    private final ReviewRepository     reviewRepo;

    // GET /api/restaurants — public
    @GetMapping
    public ResponseEntity<?> list(@RequestParam(required = false) String cuisine,
                                   @RequestParam(required = false) String search,
                                   @RequestParam(required = false) String open) {
        List<Restaurant> restaurants;
        if (search != null && !search.isBlank()) {
            restaurants = restRepo.search(search);
        } else if (cuisine != null && !cuisine.isBlank()) {
            restaurants = restRepo.findByActiveTrueAndCuisineContainingIgnoreCaseOrderByRatingDesc(cuisine);
        } else if ("true".equals(open)) {
            restaurants = restRepo.findByActiveTrueAndOpenTrueOrderByRatingDesc();
        } else {
            restaurants = restRepo.findByActiveTrueOrderByRatingDesc();
        }
        return ResponseEntity.ok(Map.of("success", true, "data", restaurants));
    }

    // GET /api/restaurants/:id — public
    @GetMapping("/{id}")
    public ResponseEntity<?> getOne(@PathVariable String id) {
        Restaurant rest = restRepo.findById(id)
            .filter(Restaurant::isActive)
            .orElseThrow(() -> ApiException.notFound("Restaurant not found"));

        List<MenuItem> items = menuRepo.findByRestaurantIdAndAvailableTrueOrderByCategoryAscBestsellerDesc(id);

        // Group menu by category
        Map<String, List<MenuItem>> menu = new LinkedHashMap<>();
        items.forEach(item -> menu.computeIfAbsent(item.getCategory(), k -> new ArrayList<>()).add(item));

        List<Review> reviews = reviewRepo.findByRestaurantIdOrderByCreatedAtDesc(id);

        return ResponseEntity.ok(Map.of("success", true,
            "data", Map.of("restaurant", rest, "menu", menu, "reviews", reviews)));
    }

    // GET /api/restaurants/mine — vendor only
    @GetMapping("/me/mine")
    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<?> mine(@AuthenticationPrincipal User user) {
        Restaurant rest = restRepo.findByOwnerId(user.getId())
            .orElseThrow(() -> ApiException.notFound("No restaurant found for this account"));
        return ResponseEntity.ok(Map.of("success", true, "data", rest));
    }

    // PATCH /api/restaurants/:id — vendor only
    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<?> update(@PathVariable String id,
                                     @RequestBody UpdateRestRequest req,
                                     @AuthenticationPrincipal User user) {
        Restaurant rest = restRepo.findById(id)
            .orElseThrow(() -> ApiException.notFound("Restaurant not found"));
        if (!rest.getOwnerId().equals(user.getId()))
            throw ApiException.forbidden("Not your restaurant");

        if (req.getName()        != null) rest.setName(req.getName());
        if (req.getDescription() != null) rest.setDescription(req.getDescription());
        if (req.getEmoji()       != null) rest.setEmoji(req.getEmoji());
        if (req.getCuisine()     != null) rest.setCuisine(req.getCuisine());
        if (req.getAddress()     != null) rest.setAddress(req.getAddress());
        if (req.getArea()        != null) rest.setArea(req.getArea());
        if (req.getPhone()       != null) rest.setPhone(req.getPhone());
        if (req.getDeliveryTime()!= null) rest.setDeliveryTime(req.getDeliveryTime());
        if (req.getMinOrder()    != null) rest.setMinOrder(req.getMinOrder());

        return ResponseEntity.ok(Map.of("success", true, "data", restRepo.save(rest)));
    }

    // PATCH /api/restaurants/:id/toggle-open — vendor only
    @PatchMapping("/{id}/toggle-open")
    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<?> toggleOpen(@PathVariable String id,
                                         @AuthenticationPrincipal User user) {
        Restaurant rest = restRepo.findById(id)
            .orElseThrow(() -> ApiException.notFound("Restaurant not found"));
        if (!rest.getOwnerId().equals(user.getId()))
            throw ApiException.forbidden("Not your restaurant");

        rest.setOpen(!rest.isOpen());
        restRepo.save(rest);
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Restaurant is now " + (rest.isOpen() ? "Open 🟢" : "Closed 🔴"),
            "data", Map.of("id", rest.getId(), "name", rest.getName(), "open", rest.isOpen())
        ));
    }

    // GET /api/restaurants/:id/stats — vendor only
    @GetMapping("/{id}/stats")
    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<?> stats(@PathVariable String id,
                                    @AuthenticationPrincipal User user) {
        Restaurant rest = restRepo.findById(id)
            .orElseThrow(() -> ApiException.notFound("Restaurant not found"));
        if (!rest.getOwnerId().equals(user.getId()))
            throw ApiException.forbidden("Not your restaurant");

        long   totalOrders  = restRepo.findById(id).map(r -> r.getTotalReviews()).orElse(0).longValue();
        double avgRating    = rest.getRating();
        long   menuCount    = menuRepo.findByRestaurantIdAndAvailableTrueOrderByCategoryAscBestsellerDesc(id).size();
        List<MenuItem> topItems = menuRepo.findByRestaurantIdAndAvailableTrueOrderByCategoryAscBestsellerDesc(id)
            .stream().sorted((a, b) -> b.getTotalSold() - a.getTotalSold()).limit(5).toList();

        return ResponseEntity.ok(Map.of("success", true, "data",
            Map.of("avgRating", avgRating, "totalReviews", rest.getTotalReviews(),
                   "menuCount", menuCount, "topItems", topItems)));
    }

    @Data static class UpdateRestRequest {
        private String     name, description, emoji, cuisine, address, area, phone, deliveryTime;
        private BigDecimal minOrder;
    }
}
