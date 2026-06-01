package com.vendorhub.controller;

import com.vendorhub.document.*;
import com.vendorhub.exception.ApiException;
import com.vendorhub.repository.*;
import com.vendorhub.service.WalletService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageRepository messageRepo;
    private final UserRepository    userRepo;
    private final WalletService     walletService;

    // GET /api/messages/conversations
    @GetMapping("/conversations")
    public ResponseEntity<?> conversations(@AuthenticationPrincipal User user) {
        List<Message> all = messageRepo.findAllMessages(user.getId());

        // Find unique partner IDs
        Set<String> partnerIds = new LinkedHashSet<>();
        all.forEach(m -> {
            if (!m.getSenderId().equals(user.getId()))   partnerIds.add(m.getSenderId());
            if (!m.getReceiverId().equals(user.getId())) partnerIds.add(m.getReceiverId());
        });

        List<Map<String, Object>> conversations = new ArrayList<>();
        for (String pid : partnerIds) {
            userRepo.findById(pid).ifPresent(partner -> {
                List<Message> conv = messageRepo.findConversation(user.getId(), pid);
                Message last = conv.isEmpty() ? null : conv.get(conv.size() - 1);
                long unread  = conv.stream().filter(m -> m.getReceiverId().equals(user.getId()) && !m.isRead()).count();

                Map<String, Object> c = new LinkedHashMap<>();
                c.put("partner",     Map.of("id", partner.getId(), "name", partner.getName(), "role", partner.getRole()));
                c.put("lastMessage", last);
                c.put("unread",      unread);
                conversations.add(c);
            });
        }

        return ResponseEntity.ok(Map.of("success", true, "data", conversations));
    }

    // GET /api/messages/:partnerId
    @GetMapping("/{partnerId}")
    public ResponseEntity<?> conversation(@PathVariable String partnerId,
                                           @AuthenticationPrincipal User user) {
        List<Message> messages = messageRepo.findConversation(user.getId(), partnerId);
        messages.stream()
            .filter(m -> m.getReceiverId().equals(user.getId()) && !m.isRead())
            .forEach(m -> { m.setRead(true); messageRepo.save(m); });
        return ResponseEntity.ok(Map.of("success", true, "data", messages));
    }

    // POST /api/messages
    @PostMapping
    public ResponseEntity<?> send(@Valid @RequestBody SendRequest req,
                                   @AuthenticationPrincipal User sender) {
        User receiver = userRepo.findById(req.getToId())
            .orElseThrow(() -> ApiException.notFound("Recipient not found"));

        Message msg = messageRepo.save(Message.builder()
            .senderId(sender.getId()).senderName(sender.getName())
            .receiverId(receiver.getId()).receiverName(receiver.getName())
            .orderId(req.getOrderId()).text(req.getText()).build());

        walletService.pushNotification(receiver.getId(),
            "Message from " + sender.getName(),
            req.getText().length() > 80 ? req.getText().substring(0, 80) + "..." : req.getText(),
            "💬");

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(Map.of("success", true, "data", msg));
    }

    // GET /api/messages/unread/count
    @GetMapping("/unread/count")
    public ResponseEntity<?> unreadCount(@AuthenticationPrincipal User user) {
        long count = messageRepo.countByReceiverIdAndIsReadFalse(user.getId());
        return ResponseEntity.ok(Map.of("success", true, "data", Map.of("count", count)));
    }

    @Data
    static class SendRequest {
        @NotBlank private String toId;
        @NotBlank private String text;
        private String orderId;
    }
}
