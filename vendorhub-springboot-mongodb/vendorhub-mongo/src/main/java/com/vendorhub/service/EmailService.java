package com.vendorhub.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service @RequiredArgsConstructor @Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendOtp(String toEmail, String otp, String type) {
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(toEmail);
            msg.setFrom("noreply@vendorhub.com");

            if ("FORGOT_PASSWORD".equals(type)) {
                msg.setSubject("VendorHub — Password Reset OTP");
                msg.setText(
                        "Hello!\n\n" +
                                "Your OTP to reset password is:\n\n" +
                                "  " + otp + "\n\n" +
                                "This OTP expires in 10 minutes.\n" +
                                "If you didn't request this, ignore this email.\n\n" +
                                "— VendorHub Team"
                );
            } else {
                msg.setSubject("VendorHub — Your Login OTP");
                msg.setText(
                        "Hello!\n\n" +
                                "Your login OTP is:\n\n" +
                                "  " + otp + "\n\n" +
                                "This OTP expires in 10 minutes.\n\n" +
                                "— VendorHub Team"
                );
            }
            mailSender.send(msg);
            log.info("OTP sent to {}", toEmail);

        } catch (Exception e) {
            log.error("Email send failed to {}: {}", toEmail, e.getMessage());
            // Development mein console pe print karo
            log.info("DEV MODE — OTP for {}: {}", toEmail, otp);
        }
    }
}