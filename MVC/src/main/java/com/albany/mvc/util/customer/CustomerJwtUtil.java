package com.albany.mvc.util.customer;

import com.albany.mvc.dto.customer.UserResponseDTO;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class CustomerJwtUtil {

    // Store JWT token in cookie
    public void storeJwtToken(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie("customer_jwt_token", token);
        cookie.setMaxAge(24 * 60 * 60); // 24 hours
        cookie.setPath("/"); // Make sure the cookie is accessible from the entire app
        cookie.setHttpOnly(true); // For security, preventing JS access
        response.addCookie(cookie);
        log.debug("JWT token stored in cookie: {}", token.substring(0, Math.min(10, token.length())) + "...");
    }

    // Get JWT token from cookies
    public String getJwtTokenFromCookies(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("customer_jwt_token".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
    
    // Remove JWT token from cookies (logout)
    public void removeJwtToken(HttpServletResponse response) {
        Cookie cookie = new Cookie("customer_jwt_token", null);
        cookie.setMaxAge(0);
        cookie.setPath("/");
        response.addCookie(cookie);
        log.debug("JWT token removed from cookies");
    }
    
    // Store user in session
    public void storeUserInSession(HttpSession session, UserResponseDTO user) {
        session.setAttribute("customerUser", user);
        log.debug("User stored in session: {}", user);
    }
    
    // Get user from session
    public UserResponseDTO getUserFromSession(HttpSession session) {
        UserResponseDTO user = (UserResponseDTO) session.getAttribute("customerUser");
        return user;
    }
    
    // Clear user from session
    public void clearUserFromSession(HttpSession session) {
        session.removeAttribute("customerUser");
        log.debug("User cleared from session");
    }
}