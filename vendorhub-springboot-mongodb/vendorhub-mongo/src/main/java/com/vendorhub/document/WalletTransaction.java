package com.vendorhub.document;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Document(collection = "wallet_transactions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WalletTransaction {

    @Id
    private String id;

    @Indexed
    private String userId;

    private TxnType    type;
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private String     description;
    private String     refOrderId;   // Order reference (optional)

    @CreatedDate
    private LocalDateTime createdAt;

    public enum TxnType { CREDIT, DEBIT }
}
