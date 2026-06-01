package com.vendorhub.repository;

import com.vendorhub.document.WalletTransaction;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface WalletTransactionRepository extends MongoRepository<WalletTransaction, String> {
    List<WalletTransaction> findByUserIdOrderByCreatedAtDesc(String userId);
    List<WalletTransaction> findByUserIdAndType(String userId, WalletTransaction.TxnType type);
}
