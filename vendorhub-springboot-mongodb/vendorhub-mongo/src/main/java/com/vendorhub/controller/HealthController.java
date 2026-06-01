package com.vendorhub.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
public class HealthController {

    @GetMapping("/api/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok(Map.of(
            "success",     true,
            "service",     "VendorHub Food Delivery API",
            "version",     "1.0.0",
            "database",    "MongoDB",
            "framework",   "Spring Boot 3.2",
            "timestamp",   LocalDateTime.now().toString()
        ));
    }

    @GetMapping("/api")
    public ResponseEntity<?> index() {
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "🔥 VendorHub Food Delivery API",
            "endpoints", Map.of(
                "auth",          "/api/auth",
                "restaurants",   "/api/restaurants",
                "menu",          "/api/menu",
                "orders",        "/api/orders",
                "wallet",        "/api/wallet",
                "messages",      "/api/messages",
                "notifications", "/api/notifications",
                "delivery",      "/api/delivery",
                "users",         "/api/users"
            )
        ));
    }
}
