package com.backend.service;

import com.backend.dto.RegisterRequestDTO;
import com.backend.dto.UserResponseDTO;
import com.backend.dto.UserUpdateDTO;
import com.backend.model.Role;
import com.backend.model.User;
import com.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // Placeholder for OTP storage (replace with your OTP service)
    private String storedEmailOtp;
    private String storedPhoneOtp;

    @Autowired
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UserResponseDTO findByEmail(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        return userOpt.map(this::mapToDto).orElseThrow(() -> new RuntimeException("User not found"));
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    public UserResponseDTO registerUser(RegisterRequestDTO registerRequest) {
        String randomPassword = UUID.randomUUID().toString();
        
        User user = new User();
        user.setEmail(registerRequest.getEmail());
        user.setPassword(passwordEncoder.encode(randomPassword));
        user.setFirstName(registerRequest.getFirstName());
        user.setLastName(registerRequest.getLastName());
        user.setPhoneNumber(registerRequest.getPhoneNumber());
        user.setRole(Role.CUSTOMER);
        
        User savedUser = userRepository.save(user);
        return mapToDto(savedUser);
    }

    public UserResponseDTO updateUser(String email, UserUpdateDTO updateDTO) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setFirstName(updateDTO.getFirstName());
        user.setLastName(updateDTO.getLastName());
        user.setStreet(updateDTO.getStreet());
        user.setCity(updateDTO.getCity());
        user.setState(updateDTO.getState());
        user.setPostalCode(updateDTO.getPostalCode());
        User updatedUser = userRepository.save(user);
        return mapToDto(updatedUser);
    }

    public void sendEmailOtp(String currentEmail, String newEmail) {
        if (userRepository.existsByEmail(newEmail) && !newEmail.equals(currentEmail)) {
            throw new RuntimeException("Email is already in use");
        }
        // Placeholder OTP generation (replace with actual email service)
        storedEmailOtp = generateOtp();
        // Send OTP to newEmail (e.g., via email service like SendGrid)
        System.out.println("Sending OTP " + storedEmailOtp + " to " + newEmail);
    }

    public UserResponseDTO updateEmail(String currentEmail, String newEmail, String otp) {
        if (!otp.equals(storedEmailOtp)) {
            throw new RuntimeException("Invalid OTP");
        }
        User user = userRepository.findByEmail(currentEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (userRepository.existsByEmail(newEmail) && !newEmail.equals(currentEmail)) {
            throw new RuntimeException("Email is already in use");
        }
        user.setEmail(newEmail);
        User updatedUser = userRepository.save(user);
        storedEmailOtp = null; // Clear OTP
        return mapToDto(updatedUser);
    }

    public void sendPhoneOtp(String currentEmail, String newPhone) {
        // Placeholder OTP generation (replace with actual SMS service)
        storedPhoneOtp = generateOtp();
        // Send OTP to newPhone (e.g., via Twilio)
        System.out.println("Sending OTP " + storedPhoneOtp + " to " + newPhone);
    }

    public UserResponseDTO updatePhone(String currentEmail, String newPhone, String otp) {
        if (!otp.equals(storedPhoneOtp)) {
            throw new RuntimeException("Invalid OTP");
        }
        User user = userRepository.findByEmail(currentEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setPhoneNumber(newPhone);
        User updatedUser = userRepository.save(user);
        storedPhoneOtp = null; // Clear OTP
        return mapToDto(updatedUser);
    }

    private String generateOtp() {
        // Placeholder OTP generation (replace with secure OTP service)
        return String.valueOf((int) (Math.random() * 9000) + 1000); // 4-digit OTP
    }

    private UserResponseDTO mapToDto(User user) {
        UserResponseDTO dto = new UserResponseDTO();
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setPhoneNumber(user.getPhoneNumber());
        dto.setStreet(user.getStreet());
        dto.setCity(user.getCity());
        dto.setState(user.getState());
        dto.setPostalCode(user.getPostalCode());
        dto.setRole(user.getRole().toString());
        return dto;
    }
}