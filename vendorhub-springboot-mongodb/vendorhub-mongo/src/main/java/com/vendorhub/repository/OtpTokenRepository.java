package com.vendorhub.repository;

import com.vendorhub.document.OtpToken;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface OtpTokenRepository extends MongoRepository<OtpToken, String> {

    // Latest unused OTP dhundho
    Optional<OtpToken> findTopByEmailAndTypeAndUsedFalseOrderByCreatedAtDesc(
            String email, OtpToken.OtpType type);

    // Purane OTPs delete karo
    void deleteByEmailAndType(String email, OtpToken.OtpType type);
}