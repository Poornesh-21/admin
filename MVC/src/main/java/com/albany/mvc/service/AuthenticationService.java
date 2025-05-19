package com.albany.mvc.service;

import com.albany.mvc.dto.AuthRequest;
import com.albany.mvc.dto.AuthResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${api.base-url}")
    private String apiBaseUrl;

    public AuthResponse authenticate(AuthRequest request) {
        String url = apiBaseUrl + "/auth/login";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<AuthRequest> entity = new HttpEntity<>(request, headers);

        try {
            ResponseEntity<String> rawResponse = restTemplate.postForEntity(
                    url,
                    entity,
                    String.class
            );

            if (rawResponse.getStatusCode().is2xxSuccessful() && rawResponse.getBody() != null) {
                try {
                    JsonNode rootNode = objectMapper.readTree(rawResponse.getBody());

                    AuthResponse authResponse = new AuthResponse();

                    if (rootNode.has("token")) {
                        authResponse.setToken(rootNode.get("token").asText());
                    }

                    if (rootNode.has("userId")) {
                        authResponse.setUserId(rootNode.get("userId").asInt());
                    }

                    if (rootNode.has("email")) {
                        authResponse.setEmail(rootNode.get("email").asText());
                    }

                    if (rootNode.has("firstName")) {
                        authResponse.setFirstName(rootNode.get("firstName").asText());
                    }

                    if (rootNode.has("lastName")) {
                        authResponse.setLastName(rootNode.get("lastName").asText());
                    }

                    if (rootNode.has("role")) {
                        if (rootNode.get("role").isTextual()) {
                            authResponse.setRole(rootNode.get("role").asText());
                        } else {
                            authResponse.setRole(rootNode.get("role").toString());
                        }
                    }

                    return authResponse;
                } catch (Exception e) {
                    throw new RuntimeException("Failed to parse authentication response");
                }
            } else {
                throw new RuntimeException("Invalid response from authentication server");
            }
        } catch (HttpClientErrorException e) {
            throw new RuntimeException("Authentication failed: " + e.getMessage());
        } catch (RestClientException e) {
            throw new RuntimeException("Error connecting to authentication server: " + e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException("Authentication failed due to an unexpected error: " + e.getMessage());
        }
    }
}