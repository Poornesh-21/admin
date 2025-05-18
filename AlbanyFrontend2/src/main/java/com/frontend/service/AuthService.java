package com.frontend.service;


import com.frontend.dto.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class AuthService {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private String backendUrl;

    // Send OTP for login
    public Map<String, Object> sendLoginOtp(String email) {
        String url = backendUrl + "/api/auth/login/send-otp";
        
        LoginRequestDTO request = new LoginRequestDTO();
        request.setEmail(email);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        HttpEntity<LoginRequestDTO> entity = new HttpEntity<>(request, headers);
        
        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                url, HttpMethod.POST, entity, Map.class);
            return response.getBody();
        } catch (HttpClientErrorException e) {
            throw new RuntimeException(e.getResponseBodyAsString());
        }
    }
    
    // Verify OTP for login
    public JwtResponseDTO verifyLoginOtp(String email, String otp) {
        String url = backendUrl + "/api/auth/login/verify-otp";
        
        OtpVerificationDTO request = new OtpVerificationDTO();
        request.setEmail(email);
        request.setOtp(otp);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        HttpEntity<OtpVerificationDTO> entity = new HttpEntity<>(request, headers);
        
        try {
            ResponseEntity<JwtResponseDTO> response = restTemplate.exchange(
                url, HttpMethod.POST, entity, JwtResponseDTO.class);
            return response.getBody();
        } catch (HttpClientErrorException e) {
            throw new RuntimeException(e.getResponseBodyAsString());
        }
    }
    
    // Send OTP for registration
    public Map<String, Object> sendRegistrationOtp(RegisterRequestDTO registerRequest) {
        String url = backendUrl + "/api/auth/register/send-otp";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        HttpEntity<RegisterRequestDTO> entity = new HttpEntity<>(registerRequest, headers);
        
        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                url, HttpMethod.POST, entity, Map.class);
            return response.getBody();
        } catch (HttpClientErrorException e) {
            throw new RuntimeException(e.getResponseBodyAsString());
        }
    }
    
    // Verify OTP for registration
    public JwtResponseDTO verifyRegistrationOtp(RegisterRequestDTO registerRequest, String otp) {
        String url = backendUrl + "/api/auth/register/verify-otp?otp=" + otp;
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        HttpEntity<RegisterRequestDTO> entity = new HttpEntity<>(registerRequest, headers);
        
        try {
            ResponseEntity<JwtResponseDTO> response = restTemplate.exchange(
                url, HttpMethod.POST, entity, JwtResponseDTO.class);
            return response.getBody();
        } catch (HttpClientErrorException e) {
            throw new RuntimeException(e.getResponseBodyAsString());
        }
    }
}