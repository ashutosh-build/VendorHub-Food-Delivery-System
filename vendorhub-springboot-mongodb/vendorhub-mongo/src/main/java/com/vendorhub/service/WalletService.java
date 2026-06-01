package com.vendorhub.service;

import com.vendorhub.document.*;
import com.vendorhub.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class WalletService {

    private final UserRepository              userRepo;
    private final WalletTransactionRepository walletRepo;
    private final NotificationRepository      notifRepo;

    public BigDecimal credit(String userId, BigDecimal amount, String description, String refOrderId) {
        User user = userRepo.findById(userId).orElseThrow();
        BigDecimal after = user.getWallet().add(amount);
        user.setWallet(after);
        userRepo.save(user);

        walletRepo.save(WalletTransaction.builder()
            .userId(userId)
            .type(WalletTransaction.TxnType.CREDIT)
            .amount(amount).balanceAfter(after)
            .description(description).refOrderId(refOrderId)
            .build());

        return after;
    }

    public BigDecimal debit(String userId, BigDecimal amount, String description, String refOrderId) {
        User user = userRepo.findById(userId).orElseThrow();
        BigDecimal after = user.getWallet().subtract(amount);
        user.setWallet(after);
        userRepo.save(user);

        walletRepo.save(WalletTransaction.builder()
            .userId(userId)
            .type(WalletTransaction.TxnType.DEBIT)
            .amount(amount).balanceAfter(after)
            .description(description).refOrderId(refOrderId)
            .build());

        return after;
    }

    public void pushNotification(String userId, String title, String body, String emoji) {
        notifRepo.save(Notification.builder()
            .userId(userId).title(title).body(body).emoji(emoji)
            .build());
    }
}
