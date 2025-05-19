package com.albany.mvc.service;

import com.albany.mvc.dto.AuthResponse;
import com.albany.mvc.dto.PasswordChangeDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class PasswordService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${api.base-url}")
    private String apiBaseUrl;

    /**
     * Change user password by calling the REST API
     */
    public AuthResponse changePassword(PasswordChangeDto passwordChangeDto, String token) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(token);

            HttpEntity<PasswordChangeDto> entity = new HttpEntity<>(passwordChangeDto, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    apiBaseUrl + "/auth/change-password",
                    entity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                // Parse the JSON properly
                AuthResponse authResponse = objectMapper.readValue(response.getBody(), AuthResponse.class);
                return authResponse;
            } else {
                throw new RuntimeException("Unexpected response from server");
            }
        } catch (HttpClientErrorException e) {
            // Try to parse the error message
            try {
                Map<String, String> errorResponse = objectMapper.readValue(
                        e.getResponseBodyAsString(),
                        objectMapper.getTypeFactory().constructMapType(Map.class, String.class, String.class)
                );

                String errorMessage = errorResponse.getOrDefault("error", "Password change failed");
                throw new RuntimeException(errorMessage);
            } catch (Exception ex) {
                throw new RuntimeException("Password change failed: " + e.getMessage());
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to change password: " + e.getMessage());
        }
    }
}