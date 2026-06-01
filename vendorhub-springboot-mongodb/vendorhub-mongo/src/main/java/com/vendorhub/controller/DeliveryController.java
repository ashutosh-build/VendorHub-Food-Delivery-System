package com.vendorhub.controller;

import com.vendorhub.document.*;
import com.vendorhub.exception.ApiException;
import com.vendorhub.repository.*;
import com.vendorhub.service.WalletService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/delivery")
@RequiredArgsConstructor
public class DeliveryController {

    private final OrderRepository             orderRepo;
    private final UserRepository              userRepo;
    private final WalletTransactionRepository walletRepo;
    private final WalletService               walletService;

    // GET /api/delivery/available  — all orders ready for pickup
    @GetMapping("/available")
    @PreAuthorize("hasRole('DELIVERY')")
    public ResponseEntity<?> available() {
        List<Order> orders = orderRepo.findAvailableForDelivery();
        return ResponseEntity.ok(Map.of("success", true, "data", orders));
    }

    // GET /api/delivery/active  — current active delivery of this partner
    @GetMapping("/active")
    @PreAuthorize("hasRole('DELIVERY')")
    public ResponseEntity<?> active(@AuthenticationPrincipal User user) {
        List<Order> active = orderRepo.findByDeliveryPartnerIdOrderByCreatedAtDesc(user.getId())
            .stream()
            .filter(o -> o.getStatus() == Order.OrderStatus.PICKED)
            .toList();
        return ResponseEntity.ok(Map.of("success", true, "data", active));
    }

    // GET /api/delivery/history  — all past deliveries
    @GetMapping("/history")
    @PreAuthorize("hasRole('DELIVERY')")
    public ResponseEntity<?> history(@AuthenticationPrincipal User user) {
        List<Order> all = orderRepo.findByDeliveryPartnerIdOrderByCreatedAtDesc(user.getId());
        return ResponseEntity.ok(Map.of("success", true, "data", all));
    }

    // GET /api/delivery/earnings  — wallet + stats
    @GetMapping("/earnings")
    @PreAuthorize("hasRole('DELIVERY')")
    public ResponseEntity<?> earnings(@AuthenticationPrincipal User user) {
        User fresh = userRepo.findById(user.getId()).orElseThrow();

        long delivered = orderRepo.countByDeliveryPartnerIdAndStatus(
            user.getId(), Order.OrderStatus.DELIVERED);

        List<WalletTransaction> txns = walletRepo.findByUserIdOrderByCreatedAtDesc(user.getId());

        double totalEarned = txns.stream()
            .filter(t -> t.getType() == WalletTransaction.TxnType.CREDIT)
            .mapToDouble(t -> t.getAmount().doubleValue()).sum();

        double todayEarnings = txns.stream()
            .filter(t -> t.getType() == WalletTransaction.TxnType.CREDIT
                && t.getCreatedAt() != null
                && t.getCreatedAt().toLocalDate().equals(java.time.LocalDate.now()))
            .mapToDouble(t -> t.getAmount().doubleValue()).sum();

        // Monthly breakdown
        Map<String, Double> monthly = new LinkedHashMap<>();
        txns.stream()
            .filter(t -> t.getType() == WalletTransaction.TxnType.CREDIT)
            .forEach(t -> {
                if (t.getCreatedAt() != null) {
                    String month = t.getCreatedAt().getYear() + "-"
                        + String.format("%02d", t.getCreatedAt().getMonthValue());
                    monthly.merge(month, t.getAmount().doubleValue(), Double::sum);
                }
            });

        return ResponseEntity.ok(Map.of("success", true, "data", Map.of(
            "walletBalance",       fresh.getWallet(),
            "totalDeliveries",     delivered,
            "totalEarned",         totalEarned,
            "todayEarnings",       todayEarnings,
            "earningPerDelivery",  40,
            "monthlyBreakdown",    monthly,
            "recentTransactions",  txns.stream().limit(20).toList()
        )));
    }

    // GET /api/delivery/stats  — quick dashboard numbers
    @GetMapping("/stats")
    @PreAuthorize("hasRole('DELIVERY')")
    public ResponseEntity<?> stats(@AuthenticationPrincipal User user) {
        User fresh = userRepo.findById(user.getId()).orElseThrow();

        long delivered = orderRepo.countByDeliveryPartnerIdAndStatus(user.getId(), Order.OrderStatus.DELIVERED);
        long active    = orderRepo.countByDeliveryPartnerIdAndStatus(user.getId(), Order.OrderStatus.PICKED);
        long available = orderRepo.findAvailableForDelivery().size();

        List<WalletTransaction> txns = walletRepo.findByUserIdOrderByCreatedAtDesc(user.getId());
        double todayEarnings = txns.stream()
            .filter(t -> t.getType() == WalletTransaction.TxnType.CREDIT
                && t.getCreatedAt() != null
                && t.getCreatedAt().toLocalDate().equals(java.time.LocalDate.now()))
            .mapToDouble(t -> t.getAmount().doubleValue()).sum();

        return ResponseEntity.ok(Map.of("success", true, "data", Map.of(
            "delivered",     delivered,
            "active",        active,
            "available",     available,
            "todayEarnings", todayEarnings,
            "wallet",        fresh.getWallet()
        )));
    }
}
