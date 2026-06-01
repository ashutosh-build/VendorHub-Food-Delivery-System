package com.vendorhub.document;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Document(collection = "menu_items")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MenuItem {

    @Id
    private String id;

    @Indexed
    private String restaurantId;
    private String restaurantName;

    private String name;
    private String description;

    @Builder.Default private String     emoji      = "🍽️";
    private BigDecimal price;
    @Builder.Default private String     category   = "Main Course";
    @Builder.Default private boolean    veg        = true;
    @Builder.Default private boolean    bestseller = false;
    @Builder.Default private boolean    available  = true;
    @Builder.Default private Double     rating     = 4.0;
    @Builder.Default private Integer    totalSold  = 0;
    private String imageUrl;

    @CreatedDate    private LocalDateTime createdAt;
    @LastModifiedDate private LocalDateTime updatedAt;
}
