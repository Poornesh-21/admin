package com.backend.service;

import com.backend.model.OtpEntity;
import com.backend.repository.OtpRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class OtpService {

    private final OtpRepository otpRepository;
    private final EmailService emailService;

    /**
     * Generate a 4-digit OTP
     */
    private String generateOtp() {
        Random random = new Random();
        int otp = 1000 + random.nextInt(9000); // 4-digit OTP
        return String.valueOf(otp);
    }

    /**
     * Generate, save and send OTP to email
     */
    public OtpEntity saveOtp(String email) {
        String otpCode = generateOtp();
        LocalDateTime expiryTime = LocalDateTime.now().plusMinutes(5);

        Optional<OtpEntity> existingOtpOpt = otpRepository.findTopByEmailOrderByCreatedAtDesc(email);

        OtpEntity otpEntity = existingOtpOpt.orElseGet(OtpEntity::new);
        otpEntity.setEmail(email);
        otpEntity.setOtp(otpCode);
        otpEntity.setExpiryTime(expiryTime);
        otpEntity.setCreatedAt(LocalDateTime.now());
        otpEntity.setVerified(false);

        OtpEntity savedOtp = otpRepository.save(otpEntity);

        // Send OTP email
        emailService.sendOtpEmail(email, otpCode);

        return savedOtp;
    }

    /**
     * Verify user OTP
     */
    public boolean verifyOtp(String email, String otp) {
        Optional<OtpEntity> otpEntityOpt = otpRepository.findByEmailAndOtp(email, otp);

        if (otpEntityOpt.isPresent()) {
            OtpEntity otpEntity = otpEntityOpt.get();

            if (LocalDateTime.now().isBefore(otpEntity.getExpiryTime()) && !otpEntity.isVerified()) {
                otpEntity.setVerified(true);
                otpRepository.save(otpEntity);
                return true;
            }
        }

        return false;
    }

    /**
     * Check if OTP is expired
     */
    public boolean isOtpExpired(String email, String otp) {
        Optional<OtpEntity> otpEntityOpt = otpRepository.findByEmailAndOtp(email, otp);
        
        if (otpEntityOpt.isPresent()) {
            OtpEntity otpEntity = otpEntityOpt.get();
            return LocalDateTime.now().isAfter(otpEntity.getExpiryTime());
        }
        
        return true;
    }

    /**
     * Get latest OTP for email - For testing only, should be removed in production
     */
    public String getLatestOtpForEmail(String email) {
        return otpRepository.findTopByEmailOrderByCreatedAtDesc(email)
                .map(OtpEntity::getOtp)
                .orElse(null);
    }
}
