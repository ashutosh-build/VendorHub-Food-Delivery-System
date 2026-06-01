package com.vendorhub.document;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Document(collection = "reviews")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Review {

    @Id
    private String id;

    @Indexed private String  orderId;
    @Indexed private String  customerId;
    private String           customerName;
    @Indexed private String  restaurantId;

    private Integer rating;
    private String  comment;

    @CreatedDate
    private LocalDateTime createdAt;
}
