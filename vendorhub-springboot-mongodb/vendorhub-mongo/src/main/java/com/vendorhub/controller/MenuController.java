package com.vendorhub.controller;

import com.vendorhub.document.*;
import com.vendorhub.exception.ApiException;
import com.vendorhub.repository.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.*;

@RestController
@RequestMapping("/api/menu")
@RequiredArgsConstructor
public class MenuController {

    private final MenuItemRepository   menuRepo;
    private final RestaurantRepository restRepo;

    // GET /api/menu?restId=xxx  — public
    @GetMapping
    public ResponseEntity<?> list(@RequestParam String restId,
                                   @RequestParam(required = false) String category,
                                   @RequestParam(required = false) String veg) {
        List<MenuItem> items;
        if ("true".equals(veg)) {
            items = menuRepo.findByRestaurantIdAndAvailableTrueAndVegTrue(restId);
        } else if (category != null && !category.isBlank()) {
            items = menuRepo.findByRestaurantIdAndCategory(restId, category);
        } else {
            items = menuRepo.findByRestaurantIdAndAvailableTrueOrderByCategoryAscBestsellerDesc(restId);
        }
        return ResponseEntity.ok(Map.of("success", true, "data", items));
    }

    // GET /api/menu/:id  — public
    @GetMapping("/{id}")
    public ResponseEntity<?> getOne(@PathVariable String id) {
        MenuItem item = menuRepo.findById(id)
            .orElseThrow(() -> ApiException.notFound("Menu item not found"));
        return ResponseEntity.ok(Map.of("success", true, "data", item));
    }

    // POST /api/menu  — vendor only
    @PostMapping
    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<?> create(@Valid @RequestBody MenuItemRequest req,
                                     @AuthenticationPrincipal User user) {
        Restaurant rest = restRepo.findByOwnerId(user.getId())
            .orElseThrow(() -> ApiException.notFound("No restaurant found for this vendor"));

        MenuItem item = menuRepo.save(MenuItem.builder()
            .restaurantId(rest.getId())
            .restaurantName(rest.getName())
            .name(req.getName())
            .description(req.getDescription())
            .emoji(req.getEmoji() != null ? req.getEmoji() : "🍽️")
            .price(req.getPrice())
            .category(req.getCategory())
            .veg(req.isVeg())
            .bestseller(req.isBestseller())
            .build());

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(Map.of("success", true, "message", "Item added to menu", "data", item));
    }

    // PATCH /api/menu/:id  — vendor only
    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<?> update(@PathVariable String id,
                                     @RequestBody MenuItemRequest req,
                                     @AuthenticationPrincipal User user) {
        MenuItem item = menuRepo.findById(id)
            .orElseThrow(() -> ApiException.notFound("Item not found"));

        // Verify ownership
        Restaurant rest = restRepo.findByOwnerId(user.getId())
            .orElseThrow(() -> ApiException.forbidden("No restaurant found"));
        if (!item.getRestaurantId().equals(rest.getId()))
            throw ApiException.forbidden("Not your menu item");

        if (req.getName()        != null) item.setName(req.getName());
        if (req.getDescription() != null) item.setDescription(req.getDescription());
        if (req.getEmoji()       != null) item.setEmoji(req.getEmoji());
        if (req.getPrice()       != null) item.setPrice(req.getPrice());
        if (req.getCategory()    != null) item.setCategory(req.getCategory());

        // Primitive booleans — always update if present in body handled via wrapper
        item.setVeg(req.isVeg());
        item.setBestseller(req.isBestseller());

        return ResponseEntity.ok(Map.of("success", true, "data", menuRepo.save(item)));
    }

    // PATCH /api/menu/:id/toggle  — vendor only
    @PatchMapping("/{id}/toggle")
    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<?> toggle(@PathVariable String id,
                                     @AuthenticationPrincipal User user) {
        MenuItem item = menuRepo.findById(id)
            .orElseThrow(() -> ApiException.notFound("Item not found"));

        Restaurant rest = restRepo.findByOwnerId(user.getId())
            .orElseThrow(() -> ApiException.forbidden("No restaurant found"));
        if (!item.getRestaurantId().equals(rest.getId()))
            throw ApiException.forbidden("Not your menu item");

        item.setAvailable(!item.isAvailable());
        menuRepo.save(item);

        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", item.getName() + " is now " + (item.isAvailable() ? "Available ✅" : "Hidden 🚫"),
            "data", Map.of("id", item.getId(), "name", item.getName(), "available", item.isAvailable())
        ));
    }

    // DELETE /api/menu/:id  — vendor only
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<?> delete(@PathVariable String id,
                                     @AuthenticationPrincipal User user) {
        MenuItem item = menuRepo.findById(id)
            .orElseThrow(() -> ApiException.notFound("Item not found"));

        Restaurant rest = restRepo.findByOwnerId(user.getId())
            .orElseThrow(() -> ApiException.forbidden("No restaurant found"));
        if (!item.getRestaurantId().equals(rest.getId()))
            throw ApiException.forbidden("Not your menu item");

        menuRepo.delete(item);
        return ResponseEntity.ok(Map.of("success", true, "message", "Menu item deleted"));
    }

    // ── Request DTO ────────────────────────────────────────
    @Data
    static class MenuItemRequest {
        @NotBlank  private String     name;
        private String               description;
        private String               emoji;
        @NotNull @DecimalMin("0.01") private BigDecimal price;
        @NotBlank  private String     category;
        private boolean              veg        = true;
        private boolean            bestseller = false;
    }
}
