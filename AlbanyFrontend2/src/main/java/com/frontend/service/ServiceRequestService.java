package com.frontend.service;

import com.frontend.dto.CreateServiceRequestDTO;
import com.frontend.dto.ServiceRequestDTO;
import com.frontend.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;

@Service
public class ServiceRequestService {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private String backendUrl;

    @Autowired
    private JwtUtil jwtUtil;

    public ServiceRequestDTO createServiceRequest(CreateServiceRequestDTO requestDTO, HttpServletRequest request) {
        String jwt = jwtUtil.getJwtTokenFromCookies(request);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + jwt);

        HttpEntity<CreateServiceRequestDTO> entity = new HttpEntity<>(requestDTO, headers);

        ResponseEntity<ServiceRequestDTO> response = restTemplate.exchange(
                backendUrl + "/api/service-requests",
                HttpMethod.POST,
                entity,
                ServiceRequestDTO.class
        );

        return response.getBody();
    }

    public List<ServiceRequestDTO> getUserServiceRequests(HttpServletRequest request) {
        String jwt = jwtUtil.getJwtTokenFromCookies(request);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + jwt);

        HttpEntity<?> entity = new HttpEntity<>(headers);

        ResponseEntity<List<ServiceRequestDTO>> response = restTemplate.exchange(
                backendUrl + "/api/service-requests",
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<>() {}
        );

        return response.getBody() != null ? response.getBody() : Collections.emptyList();
    }

    public ServiceRequestDTO getServiceRequestById(Long id, HttpServletRequest request) {
        String jwt = jwtUtil.getJwtTokenFromCookies(request);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + jwt);

        HttpEntity<?> entity = new HttpEntity<>(headers);

        ResponseEntity<ServiceRequestDTO> response = restTemplate.exchange(
                backendUrl + "/api/service-requests/" + id,
                HttpMethod.GET,
                entity,
                ServiceRequestDTO.class
        );

        return response.getBody();
    }

    public List<String> getServiceTypes(HttpServletRequest request) {
        String jwt = jwtUtil.getJwtTokenFromCookies(request);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + jwt);

        HttpEntity<?> entity = new HttpEntity<>(headers);

        ResponseEntity<List<String>> response = restTemplate.exchange(
                backendUrl + "/api/service-requests/service-types",
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<>() {}
        );

        return response.getBody() != null ? response.getBody() : Collections.emptyList();
    }

    public List<String> getVehicleTypes(HttpServletRequest request) {
        String jwt = jwtUtil.getJwtTokenFromCookies(request);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + jwt);

        HttpEntity<?> entity = new HttpEntity<>(headers);

        ResponseEntity<List<String>> response = restTemplate.exchange(
                backendUrl + "/api/service-requests/vehicle-types",
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<>() {}
        );

        return response.getBody() != null ? response.getBody() : Collections.emptyList();
    }
}