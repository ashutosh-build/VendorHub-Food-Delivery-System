package com.vendorhub.document;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Document(collection = "restaurants")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Restaurant {

    @Id
    private String id;

    private String ownerId;      // User ka ID (vendor)
    private String ownerName;

    private String name;
    private String description;

    @Builder.Default private String emoji       = "🍽️";
    @Builder.Default private String cuisine     = "Multi-cuisine";

    private String address;
    private String area;
    private String phone;
    private String imageUrl;

    @Builder.Default private Double  rating       = 4.0;
    @Builder.Default private Integer totalReviews = 0;
    @Builder.Default private String  deliveryTime = "30-45";

    @Builder.Default private BigDecimal minOrder    = new BigDecimal("99.00");
    @Builder.Default private BigDecimal deliveryFee = new BigDecimal("40.00");

    @Builder.Default private boolean open   = true;
    @Builder.Default private boolean active = true;

    @CreatedDate  private LocalDateTime createdAt;
    @LastModifiedDate private LocalDateTime updatedAt;
}
