package com.vendorhub.service;

import com.vendorhub.document.*;
import com.vendorhub.exception.ApiException;
import com.vendorhub.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository      orderRepo;
    private final RestaurantRepository restRepo;
    private final MenuItemRepository   menuRepo;
    private final UserRepository       userRepo;
    private final WalletService        walletService;

    @Value("${app.delivery.earning:40}")
    private BigDecimal deliveryEarning;

    @Value("${app.platform.fee-percent:15}")
    private double platformFeePct;

    // ── Place Order ────────────────────────────────────────
    public Order placeOrder(User customer,
                            String restId,
                            List<Map<String, Object>> items,
                            String address, Double lat, Double lng,
                            String instructions) {

        Restaurant rest = restRepo.findById(restId)
            .filter(Restaurant::isActive)
            .orElseThrow(() -> ApiException.notFound("Restaurant not found"));

        if (!rest.isOpen())
            throw ApiException.badRequest("Restaurant is currently closed");

        // Resolve items
        BigDecimal subtotal = BigDecimal.ZERO;
        List<Order.OrderItem> orderItems = new ArrayList<>();

        for (Map<String, Object> itemReq : items) {
            String  menuItemId = (String)  itemReq.get("itemId");
            Integer quantity   = (Integer) itemReq.get("quantity");

            MenuItem item = menuRepo.findById(menuItemId)
                .filter(MenuItem::isAvailable)
                .orElseThrow(() -> ApiException.badRequest("Item " + menuItemId + " not available"));

            if (!item.getRestaurantId().equals(restId))
                throw ApiException.badRequest("Item does not belong to this restaurant");

            BigDecimal lineTotal = item.getPrice().multiply(BigDecimal.valueOf(quantity));
            subtotal = subtotal.add(lineTotal);

            orderItems.add(Order.OrderItem.builder()
                .menuItemId(item.getId()).name(item.getName())
                .emoji(item.getEmoji()).price(item.getPrice())
                .quantity(quantity).subtotal(lineTotal)
                .build());

            // Update sold count
            item.setTotalSold(item.getTotalSold() + quantity);
            menuRepo.save(item);
        }

        if (subtotal.compareTo(rest.getMinOrder()) < 0)
            throw ApiException.badRequest("Minimum order is ₹" + rest.getMinOrder());

        BigDecimal total = subtotal.add(rest.getDeliveryFee());

        // Check wallet
        User freshCustomer = userRepo.findById(customer.getId()).orElseThrow();
        if (freshCustomer.getWallet().compareTo(total) < 0)
            throw ApiException.badRequest(
                "Insufficient wallet balance. Have ₹" + freshCustomer.getWallet() + ", need ₹" + total);

        // Estimated delivery
        int[] times = parseTimes(rest.getDeliveryTime());
        int estimated = times[0] + (times[1] - times[0]) / 2;

        // Build and save order
        Order order = Order.builder()
            .customerId(customer.getId()).customerName(customer.getName())
            .customerPhone(customer.getPhone())
            .restaurantId(rest.getId()).restaurantName(rest.getName())
            .restaurantEmoji(rest.getEmoji())
            .subtotal(subtotal).deliveryFee(rest.getDeliveryFee()).total(total)
            .address(address).lat(lat).lng(lng).instructions(instructions)
            .estimatedMinutes(estimated).items(orderItems)
            .timeline(new ArrayList<>(List.of(
                Order.TimelineEntry.builder()
                    .status(Order.OrderStatus.PENDING)
                    .note("Order placed by customer")
                    .actorId(customer.getId())
                    .time(LocalDateTime.now())
                    .build()
            )))
            .build();

        Order saved = orderRepo.save(order);

        // Deduct from customer wallet
        walletService.debit(customer.getId(), total,
            "Order #" + saved.getId() + " — " + rest.getName(), saved.getId());

        // Notify vendor
        walletService.pushNotification(rest.getOwnerId(),
            "New Order! 🛍️",
            "Order from " + customer.getName() + " — ₹" + total.setScale(0, RoundingMode.HALF_UP),
            "🛍️");

        return saved;
    }

    // ── Update Order Status ────────────────────────────────
    public Order updateStatus(String orderId, Order.OrderStatus newStatus, String note, User actor) {

        Order order = orderRepo.findById(orderId)
            .orElseThrow(() -> ApiException.notFound("Order not found"));

        // Permission check
        List<Order.OrderStatus> vendorAllowed    = List.of(
            Order.OrderStatus.CONFIRMED, Order.OrderStatus.PREPARING,
            Order.OrderStatus.READY,     Order.OrderStatus.CANCELLED);
        List<Order.OrderStatus> deliveryAllowed  = List.of(
            Order.OrderStatus.PICKED, Order.OrderStatus.DELIVERED);

        switch (actor.getRole()) {
            case VENDOR -> {
                Restaurant rest = restRepo.findByOwnerId(actor.getId())
                    .orElseThrow(() -> ApiException.forbidden("No restaurant found"));
                if (!order.getRestaurantId().equals(rest.getId()))
                    throw ApiException.forbidden("Not your order");
                if (!vendorAllowed.contains(newStatus))
                    throw ApiException.badRequest("Vendor cannot set status: " + newStatus);
            }
            case DELIVERY -> {
                if (!deliveryAllowed.contains(newStatus))
                    throw ApiException.badRequest("Delivery partner cannot set status: " + newStatus);
                if (newStatus == Order.OrderStatus.PICKED) {
                    // Assign this partner to the order
                    if (order.getDeliveryPartnerId() != null)
                        throw ApiException.conflict("Order already assigned to another partner");
                    if (order.getStatus() != Order.OrderStatus.READY)
                        throw ApiException.conflict("Order not ready for pickup");
                    order.setDeliveryPartnerId(actor.getId());
                    order.setDeliveryPartnerName(actor.getName());
                }
                if (newStatus == Order.OrderStatus.DELIVERED) {
                    if (!actor.getId().equals(order.getDeliveryPartnerId()))
                        throw ApiException.forbidden("Not your delivery");
                }
            }
            case CUSTOMER -> {
                if (newStatus != Order.OrderStatus.CANCELLED)
                    throw ApiException.forbidden("Customer can only cancel orders");
                if (!actor.getId().equals(order.getCustomerId()))
                    throw ApiException.forbidden("Not your order");
                if (!List.of(Order.OrderStatus.PENDING, Order.OrderStatus.CONFIRMED)
                        .contains(order.getStatus()))
                    throw ApiException.badRequest("Cannot cancel at this stage");
            }
        }

        // Update status
        order.setStatus(newStatus);
        order.getTimeline().add(Order.TimelineEntry.builder()
            .status(newStatus).note(note).actorId(actor.getId())
            .time(LocalDateTime.now()).build());

        Order saved = orderRepo.save(order);

        // Notifications and wallet side-effects
        String customerMsg = switch (newStatus) {
            case CONFIRMED -> order.getRestaurantName() + " confirmed your order! 🎉";
            case PREPARING -> order.getRestaurantName() + " is cooking your food! 👨‍🍳";
            case READY     -> "Your order is packed and ready for pickup! 📦";
            case PICKED    -> actor.getName() + " picked up your order and is on the way! 🛵";
            case DELIVERED -> "Delivered! Enjoy your meal! 🎉";
            case CANCELLED -> "Your order has been cancelled.";
            default        -> newStatus.name();
        };
        walletService.pushNotification(order.getCustomerId(), "Order Update 🍽️", customerMsg, order.getRestaurantEmoji());

        if (newStatus == Order.OrderStatus.CANCELLED) {
            // Full refund to customer
            walletService.credit(order.getCustomerId(), order.getTotal(),
                "Refund — Order #" + order.getId(), order.getId());
            walletService.pushNotification(order.getCustomerId(),
                "Refund Processed 💳",
                "₹" + order.getTotal() + " refunded to your wallet", "💳");
        }

        if (newStatus == Order.OrderStatus.DELIVERED) {
            // Pay delivery partner
            walletService.credit(order.getDeliveryPartnerId(), deliveryEarning,
                "Delivery #" + order.getId() + " — " + order.getRestaurantName(), order.getId());
            walletService.pushNotification(order.getDeliveryPartnerId(),
                "Earnings Credited! 💰",
                "₹" + deliveryEarning + " added to wallet for delivery #" + order.getId(), "💰");

            // Pay vendor (85% of order total)
            BigDecimal vendorCut = order.getTotal()
                .multiply(BigDecimal.valueOf(1 - platformFeePct / 100.0))
                .setScale(2, RoundingMode.HALF_UP);
            walletService.credit(order.getRestaurantId().isEmpty() ? order.getRestaurantId() :
                restRepo.findById(order.getRestaurantId())
                    .map(r -> r.getOwnerId()).orElse(""),
                vendorCut,
                "Revenue — Order #" + order.getId(), order.getId());

            walletService.pushNotification(
                restRepo.findById(order.getRestaurantId())
                    .map(Restaurant::getOwnerId).orElse(""),
                "Payment Received! 💰",
                "₹" + vendorCut + " credited for Order #" + order.getId(), "💰");
        }

        if (newStatus == Order.OrderStatus.PICKED) {
            walletService.pushNotification(
                restRepo.findById(order.getRestaurantId())
                    .map(Restaurant::getOwnerId).orElse(""),
                "Order Picked Up 🛵",
                actor.getName() + " picked up Order #" + order.getId(), "🛵");
        }

        return saved;
    }

    private int[] parseTimes(String deliveryTime) {
        try {
            String[] p = deliveryTime.split("-");
            return new int[]{ Integer.parseInt(p[0].trim()), Integer.parseInt(p[1].trim()) };
        } catch (Exception e) { return new int[]{ 30, 45 }; }
    }
}
