package com.backend.service;

import com.backend.dto.UserResponseDTO;
import com.backend.model.User;
import com.backend.model.User.MembershipType;
import com.backend.repository.UserRepository;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class MembershipService {

    @Autowired
    private RazorpayClient razorpayClient;

    @Autowired
    private UserRepository userRepository;

    public Map<String, String> createPaymentOrder(String email) throws RazorpayException {
        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", 120000); // ₹1200 in paise
        orderRequest.put("currency", "INR");
        orderRequest.put("receipt", "receipt_" + email);
        orderRequest.put("payment_capture", 1); // Auto-capture payment

        Order order = razorpayClient.orders.create(orderRequest);
        Map<String, String> response = new HashMap<>();
        response.put("orderId", order.get("id"));
        response.put("amount", String.valueOf(order.get("amount")));
        response.put("currency", order.get("currency"));
        return response;
    }

    public UserResponseDTO upgradeMembership(String email, String paymentId) throws RazorpayException {
        // Verify payment (optional: use Razorpay API to verify payment status)
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Update membership details
        user.setMembershipType(MembershipType.PREMIUM);
        user.setMembershipStartDate(LocalDateTime.now());
        user.setMembershipEndDate(LocalDateTime.now().plusYears(2));
        userRepository.save(user);

        // Map to DTO
        UserResponseDTO userResponse = new UserResponseDTO();
        userResponse.setEmail(user.getEmail());
        userResponse.setFirstName(user.getFirstName());
        userResponse.setLastName(user.getLastName());
        userResponse.setMembershipType(user.getMembershipType().toString());
        return userResponse;
    }
}