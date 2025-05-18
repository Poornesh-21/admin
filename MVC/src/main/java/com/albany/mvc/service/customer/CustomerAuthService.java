package com.albany.mvc.service.customer;

import com.albany.mvc.dto.customer.JwtResponseDTO;
import com.albany.mvc.dto.customer.LoginRequestDTO;
import com.albany.mvc.dto.customer.OtpVerificationDTO;
import com.albany.mvc.dto.customer.RegisterRequestDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerAuthService {

    private final RestTemplate restTemplate;

    @Value("${api.base-url}")
    private String apiBaseUrl;

    /**
     * Send OTP for login
     */
    public Map<String, Object> sendLoginOtp(String email) {
        String url = apiBaseUrl + "/api/auth/login/send-otp";
        
        LoginRequestDTO request = new LoginRequestDTO();
        request.setEmail(email);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        HttpEntity<LoginRequestDTO> entity = new HttpEntity<>(request, headers);
        
        try {
            log.info("Sending login OTP request for email: {}", email);
            ResponseEntity<Map> response = restTemplate.exchange(
                url, HttpMethod.POST, entity, Map.class);
                
            log.info("Login OTP response status: {}", response.getStatusCode());
            return response.getBody();
        } catch (HttpClientErrorException e) {
            log.error("Error from API when sending login OTP: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException(e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Unexpected error sending login OTP: {}", e.getMessage(), e);
            throw new RuntimeException("Error sending OTP: " + e.getMessage());
        }
    }
    
    /**
     * Verify OTP for login
     */
    public JwtResponseDTO verifyLoginOtp(String email, String otp) {
        String url = apiBaseUrl + "/api/auth/login/verify-otp";
        
        OtpVerificationDTO request = new OtpVerificationDTO();
        request.setEmail(email);
        request.setOtp(otp);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        HttpEntity<OtpVerificationDTO> entity = new HttpEntity<>(request, headers);
        
        try {
            log.info("Verifying login OTP for email: {}", email);
            ResponseEntity<JwtResponseDTO> response = restTemplate.exchange(
                url, HttpMethod.POST, entity, JwtResponseDTO.class);
                
            log.info("OTP verification response status: {}", response.getStatusCode());
            return response.getBody();
        } catch (HttpClientErrorException e) {
            log.error("Error from API when verifying OTP: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException(e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Unexpected error verifying OTP: {}", e.getMessage(), e);
            throw new RuntimeException("Error verifying OTP: " + e.getMessage());
        }
    }
    
    /**
     * Send OTP for registration
     */
    public Map<String, Object> sendRegistrationOtp(RegisterRequestDTO registerRequest) {
        String url = apiBaseUrl + "/api/auth/register/send-otp";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        HttpEntity<RegisterRequestDTO> entity = new HttpEntity<>(registerRequest, headers);
        
        try {
            log.info("Sending registration OTP for email: {}", registerRequest.getEmail());
            ResponseEntity<Map> response = restTemplate.exchange(
                url, HttpMethod.POST, entity, Map.class);
                
            log.info("Registration OTP response status: {}", response.getStatusCode());
            return response.getBody();
        } catch (HttpClientErrorException e) {
            log.error("Error from API when sending registration OTP: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException(e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Unexpected error sending registration OTP: {}", e.getMessage(), e);
            throw new RuntimeException("Error sending registration OTP: " + e.getMessage());
        }
    }
    
    /**
     * Verify OTP for registration
     */
    public JwtResponseDTO verifyRegistrationOtp(RegisterRequestDTO registerRequest, String otp) {
        String url = apiBaseUrl + "/api/auth/register/verify-otp?otp=" + otp;
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        HttpEntity<RegisterRequestDTO> entity = new HttpEntity<>(registerRequest, headers);
        
        try {
            log.info("Verifying registration OTP for email: {}", registerRequest.getEmail());
            ResponseEntity<JwtResponseDTO> response = restTemplate.exchange(
                url, HttpMethod.POST, entity, JwtResponseDTO.class);
                
            log.info("Registration verification response status: {}", response.getStatusCode());
            return response.getBody();
        } catch (HttpClientErrorException e) {
            log.error("Error from API when verifying registration: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException(e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Unexpected error verifying registration: {}", e.getMessage(), e);
            throw new RuntimeException("Error verifying registration: " + e.getMessage());
        }
    }
}