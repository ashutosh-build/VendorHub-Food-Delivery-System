package com.vendorhub.config;

import com.vendorhub.document.*;
import com.vendorhub.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component @RequiredArgsConstructor @Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UserRepository       userRepo;
    private final RestaurantRepository restRepo;
    private final MenuItemRepository   menuRepo;
    private final PasswordEncoder      encoder;

    @Override
    public void run(String... args) {
        if (userRepo.count() > 0) {
            log.info("MongoDB already has data — skipping seed.");
            return;
        }
        log.info("🌱 Seeding MongoDB with sample data...");

        // ── Customers ──────────────────────────────────
        User c1 = userRepo.save(User.builder()
            .name("Rahul Sharma").email("rahul@example.com").phone("9876543210")
            .password(encoder.encode("password123")).role(User.Role.CUSTOMER)
            .wallet(new BigDecimal("800")).build());

        User c2 = userRepo.save(User.builder()
            .name("Priya Nair").email("priya@example.com").phone("9876543211")
            .password(encoder.encode("password123")).role(User.Role.CUSTOMER)
            .wallet(new BigDecimal("1200")).build());

        // ── Vendors ────────────────────────────────────
        User v1 = userRepo.save(User.builder()
            .name("Amit Joshi").email("amit@spicegarden.com").phone("9811111111")
            .password(encoder.encode("vendor123")).role(User.Role.VENDOR)
            .wallet(new BigDecimal("2400")).build());

        User v2 = userRepo.save(User.builder()
            .name("Sara Khan").email("sara@burgerbarn.com").phone("9822222222")
            .password(encoder.encode("vendor123")).role(User.Role.VENDOR)
            .wallet(new BigDecimal("1800")).build());

        User v3 = userRepo.save(User.builder()
            .name("Lakshmi S").email("lakshmi@dosa.com").phone("9855555555")
            .password(encoder.encode("vendor123")).role(User.Role.VENDOR)
            .wallet(new BigDecimal("4200")).build());

        // ── Delivery Partners ──────────────────────────
        User d1 = userRepo.save(User.builder()
            .name("Ravi Kumar").email("ravi@deliver.com").phone("9866666666")
            .password(encoder.encode("rider123")).role(User.Role.DELIVERY)
            .vehicle("Scooter").wallet(new BigDecimal("340")).build());

        // ── Restaurants ────────────────────────────────
        Restaurant r1 = restRepo.save(Restaurant.builder()
            .ownerId(v1.getId()).ownerName(v1.getName())
            .name("Spice Garden").emoji("🍛")
            .description("Authentic North Indian cuisine with rich gravies and tandoor specialties")
            .cuisine("North Indian").address("12, CP Inner Circle, Connaught Place")
            .area("Connaught Place").phone("9811111111")
            .rating(4.5).totalReviews(320).deliveryTime("25-35")
            .minOrder(new BigDecimal("149")).deliveryFee(new BigDecimal("40")).build());

        Restaurant r2 = restRepo.save(Restaurant.builder()
            .ownerId(v2.getId()).ownerName(v2.getName())
            .name("Burger Barn").emoji("🍔")
            .description("Gourmet smash burgers — American comfort food done right")
            .cuisine("American").address("88, South Extension Part II")
            .area("Lajpat Nagar").phone("9822222222")
            .rating(4.3).totalReviews(210).deliveryTime("20-30")
            .minOrder(new BigDecimal("99")).deliveryFee(new BigDecimal("40")).build());

        Restaurant r3 = restRepo.save(Restaurant.builder()
            .ownerId(v3.getId()).ownerName(v3.getName())
            .name("Dosa Delight").emoji("🫓")
            .description("Traditional South Indian tiffin since 1998")
            .cuisine("South Indian").address("14-A, Rajouri Garden Market")
            .area("Rajouri Garden").phone("9855555555")
            .rating(4.7).totalReviews(720).deliveryTime("15-25")
            .minOrder(new BigDecimal("79")).deliveryFee(new BigDecimal("40")).build());

        // ── Menu: Spice Garden ─────────────────────────
        saveItem(r1, "Butter Chicken",    "Creamy tomato-based chicken curry",               "🍗", "280", "Main Course", false, true);
        saveItem(r1, "Dal Makhani",       "Slow-cooked black lentils with butter and cream", "🫘", "199", "Main Course", true,  true);
        saveItem(r1, "Paneer Tikka",      "Grilled cottage cheese in spiced tomato gravy",   "🧀", "229", "Main Course", true,  false);
        saveItem(r1, "Mutton Biryani",    "Aromatic basmati rice with slow-cooked mutton",   "🍚", "349", "Rice",        false, true);
        saveItem(r1, "Garlic Naan",       "Tandoor-baked flatbread with garlic butter",      "🫓", "49",  "Breads",      true,  false);
        saveItem(r1, "Mango Lassi",       "Chilled yoghurt drink with Alphonso mango",       "🥭", "89",  "Drinks",      true,  false);

        // ── Menu: Burger Barn ──────────────────────────
        saveItem(r2, "Classic Smash Burger",   "Double smash patty, cheddar, pickles",          "🍔", "199", "Burgers", false, true);
        saveItem(r2, "Crispy Chicken Burger",  "Southern-fried chicken, slaw, chipotle mayo",   "🍗", "229", "Burgers", false, true);
        saveItem(r2, "Veggie Burger",          "Plant-based patty, avocado crema, arugula",     "🌿", "179", "Burgers", true,  false);
        saveItem(r2, "Loaded Fries",           "Cheese sauce, jalapeños, sour cream",           "🍟", "149", "Sides",   true,  false);
        saveItem(r2, "Oreo Milkshake",         "Thick Oreo shake with whipped cream",           "🥤", "149", "Drinks",  true,  false);

        // ── Menu: Dosa Delight ─────────────────────────
        saveItem(r3, "Masala Dosa",       "Crispy rice crepe with spiced potato filling",    "🫓", "89",  "Dosas",     true, true);
        saveItem(r3, "Onion Rava Dosa",   "Lacy semolina dosa with caramelised onions",      "🫓", "99",  "Dosas",     true, true);
        saveItem(r3, "Idli Sambar (3pcs)","Soft steamed rice cakes with sambar & chutneys",  "🍱", "69",  "Breakfast", true, true);
        saveItem(r3, "Medu Vada (2pcs)",  "Crispy lentil doughnuts with coconut chutney",    "🍩", "59",  "Breakfast", true, false);
        saveItem(r3, "Filter Coffee",     "Authentic South Indian filter coffee",             "☕", "39",  "Drinks",    true, true);

        log.info("✅ MongoDB seeded: 3 restaurants, 16 menu items, 6 users");
        log.info("   Customer  → rahul@example.com    / password123");
        log.info("   Vendor    → amit@spicegarden.com / vendor123");
        log.info("   Delivery  → ravi@deliver.com     / rider123");
    }

    private void saveItem(Restaurant r, String name, String desc, String emoji,
                          String price, String cat, boolean veg, boolean best) {
        menuRepo.save(MenuItem.builder()
            .restaurantId(r.getId()).restaurantName(r.getName())
            .name(name).description(desc).emoji(emoji)
            .price(new BigDecimal(price)).category(cat)
            .veg(veg).bestseller(best).build());
    }
}
