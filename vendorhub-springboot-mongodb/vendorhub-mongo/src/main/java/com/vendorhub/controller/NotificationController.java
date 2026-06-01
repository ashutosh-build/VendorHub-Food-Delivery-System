package com.vendorhub.controller;

import com.vendorhub.document.*;
import com.vendorhub.exception.ApiException;
import com.vendorhub.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationRepository notifRepo;

    // GET /api/notifications
    @GetMapping
    public ResponseEntity<?> list(@RequestParam(required = false) String unread,
                                   @AuthenticationPrincipal User user) {
        List<Notification> all = notifRepo.findByUserIdOrderByCreatedAtDesc(user.getId());
        List<Notification> result = "true".equals(unread)
            ? all.stream().filter(n -> !n.isRead()).toList()
            : all;
        long unreadCount = all.stream().filter(n -> !n.isRead()).count();
        return ResponseEntity.ok(Map.of("success", true,
            "data", Map.of("notifications", result, "unreadCount", unreadCount)));
    }

    // PATCH /api/notifications/:id/read
    @PatchMapping("/{id}/read")
    public ResponseEntity<?> markRead(@PathVariable String id,
                                       @AuthenticationPrincipal User user) {
        Notification n = notifRepo.findById(id)
            .orElseThrow(() -> ApiException.notFound("Notification not found"));
        if (!n.getUserId().equals(user.getId()))
            throw ApiException.forbidden("Not your notification");
        n.setRead(true);
        notifRepo.save(n);
        return ResponseEntity.ok(Map.of("success", true, "message", "Marked as read"));
    }

    // PATCH /api/notifications/read/all
    @PatchMapping("/read/all")
    public ResponseEntity<?> markAllRead(@AuthenticationPrincipal User user) {
        List<Notification> all = notifRepo.findByUserIdOrderByCreatedAtDesc(user.getId());
        all.forEach(n -> n.setRead(true));
        notifRepo.saveAll(all);
        return ResponseEntity.ok(Map.of("success", true, "message", "All notifications marked as read"));
    }

    // DELETE /api/notifications/:id
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable String id,
                                     @AuthenticationPrincipal User user) {
        Notification n = notifRepo.findById(id)
            .orElseThrow(() -> ApiException.notFound("Notification not found"));
        if (!n.getUserId().equals(user.getId()))
            throw ApiException.forbidden("Not your notification");
        notifRepo.delete(n);
        return ResponseEntity.ok(Map.of("success", true, "message", "Deleted"));
    }

    // DELETE /api/notifications/clear/all
    @DeleteMapping("/clear/all")
    public ResponseEntity<?> clearAll(@AuthenticationPrincipal User user) {
        notifRepo.deleteByUserId(user.getId());
        return ResponseEntity.ok(Map.of("success", true, "message", "All notifications cleared"));
    }
}
