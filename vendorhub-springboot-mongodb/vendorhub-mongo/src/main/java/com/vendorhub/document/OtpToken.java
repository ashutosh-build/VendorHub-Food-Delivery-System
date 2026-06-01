package com.vendorhub.document;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Document(collection = "otp_tokens")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OtpToken {

    @Id
    private String id;

    @Indexed
    private String email;

    private String otp;          // 6 digit OTP
    private OtpType type;        // LOGIN ya FORGOT_PASSWORD

    @Builder.Default
    private boolean used = false;

    // Auto expire — 10 minutes baad
    @Indexed(expireAfterSeconds = 600)
    private LocalDateTime expiresAt;

    @CreatedDate
    private LocalDateTime createdAt;

    public enum OtpType { LOGIN, FORGOT_PASSWORD }
}