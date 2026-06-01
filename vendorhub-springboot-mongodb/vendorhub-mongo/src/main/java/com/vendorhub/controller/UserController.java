package com.vendorhub.controller;

import com.vendorhub.document.*;
import com.vendorhub.exception.ApiException;
import com.vendorhub.repository.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository              userRepo;
    private final ReviewRepository            reviewRepo;
    private final OrderRepository             orderRepo;
    private final WalletTransactionRepository walletRepo;

    // GET /api/users/profile
    @GetMapping("/profile")
    public ResponseEntity<?> profile(@AuthenticationPrincipal User user) {
        User fresh = userRepo.findById(user.getId()).orElseThrow();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id",        fresh.getId());
        data.put("name",      fresh.getName());
        data.put("email",     fresh.getEmail());
        data.put("phone",     fresh.getPhone());
        data.put("role",      fresh.getRole());
        data.put("wallet",    fresh.getWallet());
        data.put("avatar",    fresh.getAvatar());
        data.put("vehicle",   fresh.getVehicle());
        data.put("createdAt", fresh.getCreatedAt());

        // Role-specific extras
        if (fresh.getRole() == User.Role.CUSTOMER) {
            List<Order> orders = orderRepo.findByCustomerIdOrderByCreatedAtDesc(fresh.getId());
            double spent = orders.stream().mapToDouble(o -> o.getTotal().doubleValue()).sum();
            data.put("orderStats", Map.of("total", orders.size(), "spent", spent));
        }

        if (fresh.getRole() == User.Role.DELIVERY) {
            long done = orderRepo.countByDeliveryPartnerIdAndStatus(fresh.getId(), Order.OrderStatus.DELIVERED);
            data.put("deliveries", done);
        }

        return ResponseEntity.ok(Map.of("success", true, "data", data));
    }

    // PATCH /api/users/profile
    @PatchMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestBody UpdateProfileRequest req,
                                            @AuthenticationPrincipal User user) {
        User fresh = userRepo.findById(user.getId()).orElseThrow();
        if (req.getName()   != null) fresh.setName(req.getName());
        if (req.getPhone()  != null) fresh.setPhone(req.getPhone());
        if (req.getAvatar() != null) fresh.setAvatar(req.getAvatar());
        userRepo.save(fresh);
        return ResponseEntity.ok(Map.of("success", true, "message", "Profile updated", "data", fresh));
    }

    // GET /api/users/reviews  — customer's submitted reviews
    @GetMapping("/reviews")
    public ResponseEntity<?> reviews(@AuthenticationPrincipal User user) {
        List<Review> reviews = reviewRepo.findByCustomerIdOrderByCreatedAtDesc(user.getId());
        return ResponseEntity.ok(Map.of("success", true, "data", reviews));
    }

    // GET /api/users/addresses  — customer's recent delivery addresses
    @GetMapping("/addresses")
    public ResponseEntity<?> addresses(@AuthenticationPrincipal User user) {
        List<Order> orders = orderRepo.findByCustomerIdOrderByCreatedAtDesc(user.getId());
        List<String> addresses = orders.stream()
            .map(Order::getAddress)
            .filter(Objects::nonNull)
            .distinct()
            .limit(10)
            .toList();
        return ResponseEntity.ok(Map.of("success", true, "data", addresses));
    }

    @Data
    static class UpdateProfileRequest {
        private String name;
        private String phone;
        private String avatar;
    }
}
