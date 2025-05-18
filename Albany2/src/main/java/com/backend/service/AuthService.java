package com.backend.service;

import com.backend.dto.*;
import com.backend.model.OtpEntity;
import com.backend.model.User;
import com.backend.model.Role;
import com.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Optional;

@Service
public class AuthService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OtpService otpService;

    @Autowired
    private JwtService jwtService;
    
    @Autowired
    private EmailService emailService;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
    }

    public String sendLoginOtp(String email) {
        if (!userRepository.existsByEmail(email)) {
            throw new UsernameNotFoundException("User not found with email: " + email);
        }

        otpService.saveOtp(email); // This will generate & send the OTP

        return "OTP sent successfully";
    }
    
    public String sendRegistrationOtp(RegisterRequestDTO registerRequest) {
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new IllegalArgumentException("Email already registered");
        }

        otpService.saveOtp(registerRequest.getEmail()); // This will generate & send the OTP

        return "OTP sent successfully";
    }

    public JwtResponseDTO verifyLoginOtp(OtpVerificationDTO otpVerification) {
        boolean isVerified = otpService.verifyOtp(otpVerification.getEmail(), otpVerification.getOtp());
        
        if (!isVerified) {
            if (otpService.isOtpExpired(otpVerification.getEmail(), otpVerification.getOtp())) {
                throw new IllegalArgumentException("OTP has expired. Please request a new one.");
            } else {
                throw new IllegalArgumentException("Invalid OTP. Please try again.");
            }
        }

        Optional<User> userOpt = userRepository.findByEmail(otpVerification.getEmail());
        if (userOpt.isEmpty()) {
            throw new UsernameNotFoundException("User not found");
        }

        UserDetails userDetails = loadUserByUsername(otpVerification.getEmail());
        String jwtToken = jwtService.generateToken(userDetails);

        return new JwtResponseDTO(jwtToken, mapToDto(userOpt.get()));
    }

    @Transactional
    public JwtResponseDTO verifyRegistrationOtp(RegisterRequestDTO registerRequest, String otp) {
        boolean isVerified = otpService.verifyOtp(registerRequest.getEmail(), otp);

        if (!isVerified) {
            if (otpService.isOtpExpired(registerRequest.getEmail(), otp)) {
                throw new IllegalArgumentException("OTP has expired. Please request a new one.");
            } else {
                throw new IllegalArgumentException("Invalid OTP. Please try again.");
            }
        }

        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new IllegalArgumentException("Email already registered.");
        }

        // Create and save user
        User user = new User();
        user.setEmail(registerRequest.getEmail());
        user.setFirstName(registerRequest.getFirstName());
        user.setLastName(registerRequest.getLastName());
        user.setPhoneNumber(registerRequest.getPhoneNumber());
        user.setPassword("otp-login"); // Using a placeholder password for OTP-based auth
        user.setRole(Role.CUSTOMER); // Default role

        userRepository.save(user);

        // Send welcome email
        emailService.sendWelcomeEmail(user.getEmail(), user.getFirstName());

        // Generate JWT
        UserDetails userDetails = new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
        
        String jwtToken = jwtService.generateToken(userDetails);

        return new JwtResponseDTO(jwtToken, mapToDto(user));
    }

    // Temporary DTO mapping (move this to UserService if needed later)
    private UserResponseDTO mapToDto(User user) {
        UserResponseDTO dto = new UserResponseDTO();
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setPhoneNumber(user.getPhoneNumber());
        dto.setRole(user.getRole().toString());
        return dto;
    }
}