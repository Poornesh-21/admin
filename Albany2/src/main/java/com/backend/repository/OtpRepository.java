package com.backend.repository;

import com.backend.model.OtpEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface OtpRepository extends JpaRepository<OtpEntity, Long> {
    Optional<OtpEntity> findByEmailAndOtp(String email, String otp);
    Optional<OtpEntity> findTopByEmailOrderByCreatedAtDesc(String email);
}