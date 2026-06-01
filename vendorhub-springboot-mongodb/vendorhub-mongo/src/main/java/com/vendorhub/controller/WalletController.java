package com.vendorhub.controller;

import com.vendorhub.document.*;
import com.vendorhub.exception.ApiException;
import com.vendorhub.repository.*;
import com.vendorhub.service.WalletService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.*;

@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final UserRepository              userRepo;
    private final WalletTransactionRepository txnRepo;
    private final WalletService               walletService;

    // GET /api/wallet  — balance + recent transactions
    @GetMapping
    public ResponseEntity<?> balance(@AuthenticationPrincipal User user) {
        User fresh = userRepo.findById(user.getId()).orElseThrow();
        List<WalletTransaction> txns = txnRepo.findByUserIdOrderByCreatedAtDesc(user.getId());
        return ResponseEntity.ok(Map.of("success", true, "data",
            Map.of("balance", fresh.getWallet(), "transactions", txns)));
    }

    // POST /api/wallet/topup  — add money
    @PostMapping("/topup")
    public ResponseEntity<?> topup(@Valid @RequestBody TopupRequest req,
                                    @AuthenticationPrincipal User user) {
        BigDecimal newBalance = walletService.credit(
            user.getId(), req.getAmount(),
            "Wallet top-up via " + (req.getMethod() != null ? req.getMethod() : "card"),
            null
        );
        walletService.pushNotification(user.getId(),
            "Money Added! 💳",
            "₹" + req.getAmount() + " added to your VendorHub wallet",
            "💳");

        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "₹" + req.getAmount() + " added to wallet!",
            "data", Map.of("balance", newBalance)
        ));
    }

    // GET /api/wallet/transactions  — paginated history
    @GetMapping("/transactions")
    public ResponseEntity<?> transactions(@RequestParam(defaultValue = "0")  int page,
                                           @RequestParam(defaultValue = "20") int size,
                                           @RequestParam(required = false) String type,
                                           @AuthenticationPrincipal User user) {
        List<WalletTransaction> all = type != null
            ? txnRepo.findByUserIdAndType(user.getId(),
                WalletTransaction.TxnType.valueOf(type.toUpperCase()))
            : txnRepo.findByUserIdOrderByCreatedAtDesc(user.getId());

        int from  = page * size;
        int to    = Math.min(from + size, all.size());
        List<WalletTransaction> paged = from >= all.size() ? List.of() : all.subList(from, to);

        return ResponseEntity.ok(Map.of("success", true,
            "data", Map.of("transactions", paged, "total", all.size(), "page", page)));
    }

    // GET /api/wallet/summary
    @GetMapping("/summary")
    public ResponseEntity<?> summary(@AuthenticationPrincipal User user) {
        User fresh = userRepo.findById(user.getId()).orElseThrow();
        List<WalletTransaction> all  = txnRepo.findByUserIdOrderByCreatedAtDesc(user.getId());
        double credited = all.stream().filter(t -> t.getType() == WalletTransaction.TxnType.CREDIT)
            .mapToDouble(t -> t.getAmount().doubleValue()).sum();
        double debited  = all.stream().filter(t -> t.getType() == WalletTransaction.TxnType.DEBIT)
            .mapToDouble(t -> t.getAmount().doubleValue()).sum();

        return ResponseEntity.ok(Map.of("success", true, "data",
            Map.of("balance", fresh.getWallet(),
                   "totalCredited", credited,
                   "totalDebited",  debited,
                   "transactionCount", all.size())));
    }

    @Data
    static class TopupRequest {
        @NotNull @DecimalMin("1.0") @DecimalMax("50000.0")
        private BigDecimal amount;
        private String method = "card";
    }
}
