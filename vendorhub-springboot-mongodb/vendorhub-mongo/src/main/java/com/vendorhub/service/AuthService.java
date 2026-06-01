package com.vendorhub.service;

import com.vendorhub.document.*;
import com.vendorhub.exception.ApiException;
import com.vendorhub.repository.*;
import com.vendorhub.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AuthService {
    // Existing inject karo ke saath yeh 2 add karo:
    private final OtpTokenRepository otpRepo;
    private final EmailService        emailService;

    private final UserRepository       userRepo;
    private final RestaurantRepository restRepo;
    private final WalletTransactionRepository walletRepo;
    private final PasswordEncoder      encoder;
    private final JwtUtil              jwt;

    public Map<String, Object> register(String name, String email, String phone, String password,
                                         User.Role role, String restName, String cuisine,
                                         String area, String vehicle) {
        if (userRepo.existsByEmail(email))
            throw ApiException.conflict("Email already registered");

        BigDecimal wallet = role == User.Role.CUSTOMER ? new BigDecimal("500") : BigDecimal.ZERO;

        User user = userRepo.save(User.builder()
            .name(name).email(email).phone(phone)
            .password(encoder.encode(password))
            .role(role).wallet(wallet).vehicle(vehicle)
            .build());

        // Create restaurant document for vendor
        String createdRestId = null;
        if (role == User.Role.VENDOR) {
            Restaurant rest = restRepo.save(Restaurant.builder()
                .ownerId(user.getId()).ownerName(user.getName())
                .name(restName != null ? restName : name + "'s Kitchen")
                .emoji("🍽️")
                .cuisine(cuisine != null ? cuisine : "Multi-cuisine")
                .area(area != null ? area : "Your Area")
                .build());
            createdRestId = rest.getId();
        }

        // Welcome bonus wallet credit
        if (role == User.Role.CUSTOMER) {
            walletRepo.save(WalletTransaction.builder()
                .userId(user.getId()).type(WalletTransaction.TxnType.CREDIT)
                .amount(wallet).balanceAfter(wallet)
                .description("Welcome bonus — ₹500 credited").build());
        }

        return buildResponse(user, createdRestId);
    }

    public Map<String, Object> login(String email, String password) {
        User user = userRepo.findByEmail(email)
            .orElseThrow(() -> ApiException.unauthorized("Invalid email or password"));

        if (!user.isActive())
            throw ApiException.forbidden("Account suspended. Contact support.");

        if (!encoder.matches(password, user.getPassword()))
            throw ApiException.unauthorized("Invalid email or password");

        return buildResponse(user, null);
    }

    // ── Private helpers ─────────────────────────────────────
    private Map<String, Object> buildResponse(User user, String restId) {
        String accessToken  = jwt.generateAccessToken(user.getId(), user.getRole().name(), user.getEmail());
        String refreshToken = jwt.generateRefreshToken(user.getId());

        Map<String, Object> userData = new LinkedHashMap<>();
        userData.put("id",           user.getId());
        userData.put("name",         user.getName());
        userData.put("email",        user.getEmail());
        userData.put("phone",        user.getPhone());
        userData.put("role",         user.getRole());
        userData.put("wallet",       user.getWallet());
        userData.put("avatar",       user.getAvatar());
        userData.put("vehicle",      user.getVehicle());
        userData.put("accessToken",  accessToken);
        userData.put("refreshToken", refreshToken);

        if (user.getRole() == User.Role.VENDOR) {
            Optional<Restaurant> rest = restRepo.findByOwnerId(user.getId());
            rest.ifPresent(r -> {
                Map<String, Object> restMap = new LinkedHashMap<>();
                restMap.put("id",    r.getId());
                restMap.put("name",  r.getName());
                restMap.put("emoji", r.getEmoji());
                restMap.put("open",  r.isOpen());
                userData.put("restaurant", restMap);
            });
        }

        return userData;
    }
    // ── Method 1: OTP Generate & Send ─────────────────────────
    public void sendOtp(String email, OtpToken.OtpType type) {

        // User exist karta hai ya nahi check karo
        if (type == OtpToken.OtpType.FORGOT_PASSWORD) {
            userRepo.findByEmail(email)
                    .orElseThrow(() -> ApiException.notFound("No account found with this email"));
        }

        // Purane OTPs delete karo
        otpRepo.deleteByEmailAndType(email, type);

        // 6-digit random OTP
        String otp = String.format("%06d", new java.util.Random().nextInt(999999));

        // Save karo
        otpRepo.save(OtpToken.builder()
                .email(email)
                .otp(otp)
                .type(type)
                .expiresAt(java.time.LocalDateTime.now().plusMinutes(10))
                .build());

        // Email bhejo
        emailService.sendOtp(email, otp, type.name());
    }

    // ── Method 2: OTP Verify ──────────────────────────────────
    public void verifyOtp(String email, String otp, OtpToken.OtpType type) {

        OtpToken token = otpRepo
                .findTopByEmailAndTypeAndUsedFalseOrderByCreatedAtDesc(email, type)
                .orElseThrow(() -> ApiException.badRequest("OTP not found or already used"));

        if (java.time.LocalDateTime.now().isAfter(token.getExpiresAt()))
            throw ApiException.badRequest("OTP has expired. Please request a new one.");

        if (!token.getOtp().equals(otp))
            throw ApiException.badRequest("Invalid OTP. Please check and try again.");

        // OTP mark as used
        token.setUsed(true);
        otpRepo.save(token);
    }

    // ── Method 3: Reset Password ──────────────────────────────
    public void resetPassword(String email, String otp, String newPassword) {

        // OTP verify karo
        verifyOtp(email, otp, OtpToken.OtpType.FORGOT_PASSWORD);

        // Password update karo
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> ApiException.notFound("User not found"));

        if (newPassword == null || newPassword.length() < 6)
            throw ApiException.badRequest("Password must be at least 6 characters");

        user.setPassword(encoder.encode(newPassword));
        userRepo.save(user);
    }
}
