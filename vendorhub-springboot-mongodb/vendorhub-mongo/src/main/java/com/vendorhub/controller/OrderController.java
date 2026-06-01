package com.vendorhub.controller;

import com.vendorhub.document.*;
import com.vendorhub.exception.ApiException;
import com.vendorhub.repository.*;
import com.vendorhub.service.OrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService       orderService;
    private final OrderRepository    orderRepo;
    private final RestaurantRepository restRepo;
    private final ReviewRepository   reviewRepo;
    private final WalletTransactionRepository walletRepo;

    // POST /api/orders  — customer places an order
    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> placeOrder(@Valid @RequestBody PlaceOrderRequest req,
                                         @AuthenticationPrincipal User customer) {
        // Convert request items to List<Map>
        List<Map<String, Object>> items = req.getItems().stream()
            .map(i -> Map.<String, Object>of("itemId", i.getItemId(), "quantity", i.getQuantity()))
            .toList();

        Order order = orderService.placeOrder(
            customer, req.getRestId(), items,
            req.getAddress(), req.getLat(), req.getLng(), req.getInstructions()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(Map.of("success", true, "message", "Order placed successfully!", "data", order));
    }

    // GET /api/orders  — role-aware list
    @GetMapping
    public ResponseEntity<?> list(@RequestParam(required = false) String status,
                                   @AuthenticationPrincipal User user) {
        List<Order> orders = switch (user.getRole()) {
            case CUSTOMER -> orderRepo.findByCustomerIdOrderByCreatedAtDesc(user.getId());
            case VENDOR   -> {
                Restaurant rest = restRepo.findByOwnerId(user.getId())
                    .orElse(null);
                yield rest != null
                    ? orderRepo.findByRestaurantIdOrderByCreatedAtDesc(rest.getId())
                    : List.of();
            }
            case DELIVERY -> orderRepo.findByDeliveryPartnerIdOrderByCreatedAtDesc(user.getId());
        };

        // Filter by status if provided
        if (status != null && !status.isBlank()) {
            Order.OrderStatus s = Order.OrderStatus.valueOf(status.toUpperCase());
            orders = orders.stream().filter(o -> o.getStatus() == s).toList();
        }

        return ResponseEntity.ok(Map.of("success", true, "data", orders));
    }

    // GET /api/orders/available  — delivery partner sees ready orders
    @GetMapping("/available")
    @PreAuthorize("hasRole('DELIVERY')")
    public ResponseEntity<?> available() {
        List<Order> orders = orderRepo.findAvailableForDelivery();
        return ResponseEntity.ok(Map.of("success", true, "data", orders));
    }

    // GET /api/orders/:id  — get single order
    @GetMapping("/{id}")
    public ResponseEntity<?> getOne(@PathVariable String id,
                                     @AuthenticationPrincipal User user) {
        Order order = orderRepo.findById(id)
            .orElseThrow(() -> ApiException.notFound("Order not found"));

        // Access check
        boolean isCustomer  = order.getCustomerId().equals(user.getId());
        boolean isPartner   = user.getId().equals(order.getDeliveryPartnerId());
        boolean isVendor    = user.getRole() == User.Role.VENDOR &&
            restRepo.findByOwnerId(user.getId())
                .map(r -> r.getId().equals(order.getRestaurantId()))
                .orElse(false);

        if (!isCustomer && !isPartner && !isVendor)
            throw ApiException.forbidden("Not authorized to view this order");

        return ResponseEntity.ok(Map.of("success", true, "data", order));
    }

    // PATCH /api/orders/:id/status  — update order status
    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable String id,
                                           @Valid @RequestBody StatusRequest req,
                                           @AuthenticationPrincipal User user) {
        Order updated = orderService.updateStatus(id, req.getStatus(), req.getNote(), user);
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Order status updated → " + updated.getStatus(),
            "data", updated
        ));
    }

    // POST /api/orders/:id/review  — customer reviews delivered order
    @PostMapping("/{id}/review")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> review(@PathVariable String id,
                                     @Valid @RequestBody ReviewRequest req,
                                     @AuthenticationPrincipal User user) {
        Order order = orderRepo.findById(id)
            .orElseThrow(() -> ApiException.notFound("Order not found"));

        if (!order.getCustomerId().equals(user.getId()))
            throw ApiException.forbidden("Not your order");
        if (order.getStatus() != Order.OrderStatus.DELIVERED)
            throw ApiException.badRequest("Can only review delivered orders");
        if (reviewRepo.existsByOrderId(id))
            throw ApiException.conflict("Already reviewed this order");

        Review review = reviewRepo.save(Review.builder()
            .orderId(id)
            .customerId(user.getId())
            .customerName(user.getName())
            .restaurantId(order.getRestaurantId())
            .rating(req.getRating())
            .comment(req.getComment())
            .build());

        // Recalculate restaurant average rating
        long  count = reviewRepo.countByRestaurantId(order.getRestaurantId());
        List<Review> allReviews = reviewRepo.findByRestaurantIdOrderByCreatedAtDesc(order.getRestaurantId());
        double avg = allReviews.stream().mapToInt(Review::getRating).average().orElse(4.0);

        restRepo.findById(order.getRestaurantId()).ifPresent(rest -> {
            rest.setRating(Math.round(avg * 10.0) / 10.0);
            rest.setTotalReviews((int) count);
            restRepo.save(rest);
        });

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(Map.of("success", true, "message", "Review submitted! Thank you ⭐", "data", review));
    }

    // ── Request DTOs ─────────────────────────────────────
    @Data
    static class PlaceOrderRequest {
        @NotBlank  private String restId;
        @NotEmpty  private List<Item> items;
        @NotBlank  private String address;
        private Double lat, lng;
        private String instructions;

        @Data
        static class Item {
            @NotBlank private String  itemId;
            @Min(1)   private Integer quantity;
        }
    }

    @Data
    static class StatusRequest {
        @NotNull private Order.OrderStatus status;
        private String note;
    }

    @Data
    static class ReviewRequest {
        @NotNull @Min(1) @Max(5) private Integer rating;
        private String comment;
    }
}
