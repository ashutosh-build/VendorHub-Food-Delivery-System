package com.vendorhub.controller;

import com.vendorhub.document.OtpToken;
import com.vendorhub.document.User;
import com.vendorhub.service.AuthService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // POST /api/auth/register
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody RegisterRequest req) {
        Map<String, Object> result = authService.register(
            req.getName(), req.getEmail(), req.getPhone(), req.getPassword(),
            req.getRole(), req.getRestName(), req.getCuisine(), req.getArea(), req.getVehicle()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("success", true, "data", result));
    }

    // POST /api/auth/login
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req) {
        Map<String, Object> result = authService.login(req.getEmail(), req.getPassword());
        return ResponseEntity.ok(Map.of("success", true, "data", result));
    }

    // GET /api/auth/me
    @GetMapping("/me")
    public ResponseEntity<?> me(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(Map.of("success", true, "data", user));
    }

    // ── Request DTOs ────────────────────────────────────
    @Data
    static class RegisterRequest {
        @NotBlank  private String    name;
        @Email @NotBlank private String email;
        private String    phone;
        @NotBlank @Size(min = 6) private String password;
        @NotNull   private User.Role role;
        private String    restName;
        private String    cuisine;
        private String    area;
        private String    vehicle;
    }

    @Data
    static class LoginRequest {
        @Email @NotBlank private String email;
        @NotBlank        private String password;
    }
    // ── POST /api/auth/send-otp ───────────────────────────────
    @PostMapping("/send-otp")
    public ResponseEntity<?> sendOtp(@Valid @RequestBody SendOtpRequest req) {
        authService.sendOtp(req.getEmail(), req.getType());
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "OTP sent to " + req.getEmail()
        ));
    }

    // ── POST /api/auth/verify-otp ─────────────────────────────
    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@Valid @RequestBody VerifyOtpRequest req) {
        authService.verifyOtp(req.getEmail(), req.getOtp(), req.getType());
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "OTP verified successfully"
        ));
    }

    // ── POST /api/auth/reset-password ────────────────────────
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        authService.resetPassword(req.getEmail(), req.getOtp(), req.getNewPassword());
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Password reset successfully! Please login."
        ));
    }

    // ── Inner Request DTOs ────────────────────────────────────
    @Data
    static class SendOtpRequest {
        @Email @NotBlank private String email;
        @NotNull private OtpToken.OtpType type;  // LOGIN ya FORGOT_PASSWORD
    }

    @Data
    static class VerifyOtpRequest {
        @Email @NotBlank private String email;
        @NotBlank @Size(min=6, max=6) private String otp;
        @NotNull private OtpToken.OtpType type;
    }

    @Data
    static class ResetPasswordRequest {
        @Email @NotBlank private String email;
        @NotBlank @Size(min=6, max=6) private String otp;
        @NotBlank @Size(min=6) private String newPassword;
    }
}
