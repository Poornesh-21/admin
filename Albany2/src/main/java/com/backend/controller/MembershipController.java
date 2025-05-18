package com.backend.controller;

import com.backend.dto.UserResponseDTO;
import com.backend.service.MembershipService;
import com.razorpay.RazorpayException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/membership")
public class MembershipController {

    @Autowired
    private MembershipService membershipService;

    @PostMapping("/create-order")
    public ResponseEntity<?> createPaymentOrder(@RequestParam String email) {
        try {
            Map<String, String> orderDetails = membershipService.createPaymentOrder(email);
            return ResponseEntity.ok(orderDetails);
        } catch (RazorpayException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/upgrade")
    public ResponseEntity<?> upgradeMembership(@RequestParam String email, @RequestParam String paymentId) {
        try {
            UserResponseDTO userResponse = membershipService.upgradeMembership(email, paymentId);
            return ResponseEntity.ok(userResponse);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}