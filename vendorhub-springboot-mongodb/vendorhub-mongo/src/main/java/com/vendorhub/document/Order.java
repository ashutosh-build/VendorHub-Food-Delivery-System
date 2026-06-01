package com.vendorhub.document;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "orders")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Order {

    @Id
    private String id;

    // Customer info
    @Indexed private String customerId;
    private String customerName;
    private String customerPhone;

    // Restaurant info
    @Indexed private String restaurantId;
    private String restaurantName;
    private String restaurantEmoji;

    // Delivery Partner
    private String deliveryPartnerId;
    private String deliveryPartnerName;

    @Builder.Default
    private OrderStatus status = OrderStatus.PENDING;

    // Pricing
    private BigDecimal subtotal;
    @Builder.Default private BigDecimal deliveryFee = new BigDecimal("40.00");
    private BigDecimal total;

    // Delivery
    private String  address;
    private Double  lat;
    private Double  lng;
    private String  instructions;
    private String  cancelReason;
    private Integer estimatedMinutes;

    // Payment
    @Builder.Default private String        paymentMethod = "wallet";
    @Builder.Default private PaymentStatus paymentStatus = PaymentStatus.PAID;

    // Embedded — no separate collection needed (MongoDB advantage!)
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    @Builder.Default
    private List<TimelineEntry> timeline = new ArrayList<>();

    @CreatedDate    private LocalDateTime createdAt;
    @LastModifiedDate private LocalDateTime updatedAt;

    // ── Embedded classes ────────────────────────────

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class OrderItem {
        private String     menuItemId;
        private String     name;
        private String     emoji;
        private BigDecimal price;
        private Integer    quantity;
        private BigDecimal subtotal;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class TimelineEntry {
        private OrderStatus     status;
        private String          note;
        private String          actorId;
        private LocalDateTime   time;
    }

    public enum OrderStatus  { PENDING, CONFIRMED, PREPARING, READY, PICKED, DELIVERED, CANCELLED }
    public enum PaymentStatus { PENDING, PAID, REFUNDED, FAILED }
}
