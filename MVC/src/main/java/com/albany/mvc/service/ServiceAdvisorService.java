package com.albany.mvc.service;

import com.albany.mvc.dto.ServiceAdvisorDto;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ServiceAdvisorService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${api.base-url}")
    private String apiBaseUrl;

    public List<ServiceAdvisorDto> getAllServiceAdvisors(String token) {
        String url = apiBaseUrl + "/service-advisors";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                return objectMapper.readValue(
                        response.getBody(),
                        new TypeReference<List<ServiceAdvisorDto>>() {}
                );
            }
        } catch (Exception e) {
            // Basic error handling without excessive logging
        }

        return Collections.emptyList();
    }

    public ServiceAdvisorDto getServiceAdvisorById(Integer id, String token) {
        String url = apiBaseUrl + "/service-advisors/" + id;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<ServiceAdvisorDto> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    ServiceAdvisorDto.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                return response.getBody();
            }
        } catch (Exception e) {
            // Basic error handling
        }

        return null;
    }

    public ServiceAdvisorDto createServiceAdvisor(ServiceAdvisorDto advisorDto, String token) {
        String url = apiBaseUrl + "/service-advisors";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<ServiceAdvisorDto> entity = new HttpEntity<>(advisorDto, headers);

        try {
            ResponseEntity<ServiceAdvisorDto> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    ServiceAdvisorDto.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                return response.getBody();
            }
        } catch (Exception e) {
            // Basic error handling
        }

        return null;
    }

    public ServiceAdvisorDto updateServiceAdvisor(Integer id, ServiceAdvisorDto advisorDto, String token) {
        String url = apiBaseUrl + "/service-advisors/" + id;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<ServiceAdvisorDto> entity = new HttpEntity<>(advisorDto, headers);

        try {
            ResponseEntity<ServiceAdvisorDto> response = restTemplate.exchange(
                    url,
                    HttpMethod.PUT,
                    entity,
                    ServiceAdvisorDto.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                return response.getBody();
            }
        } catch (Exception e) {
            // Basic error handling
        }

        return null;
    }

    public boolean deleteServiceAdvisor(Integer id, String token) {
        String url = apiBaseUrl + "/service-advisors/" + id;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Void> response = restTemplate.exchange(
                    url,
                    HttpMethod.DELETE,
                    entity,
                    Void.class
            );

            return response.getStatusCode() == HttpStatus.NO_CONTENT;
        } catch (Exception e) {
            // Basic error handling
        }

        return false;
    }
}