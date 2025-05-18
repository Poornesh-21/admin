package com.frontend.util;

import com.frontend.dto.UserResponseDTO;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;

@Component
public class JwtUtil {

    // Store JWT token
    public void storeJwtToken(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie("jwt_token", token);
        cookie.setMaxAge(24 * 60 * 60); // 24 hours
        cookie.setPath("/"); // Make sure the cookie is accessible from the entire app
        cookie.setHttpOnly(true); // For security, preventing JS access
        // cookie.setSecure(true); // Uncomment in production to only send over HTTPS
        response.addCookie(cookie);
        System.out.println("JWT token stored in cookie: " + token.substring(0, Math.min(10, token.length())) + "...");
    }

    // Get JWT token from cookies
    public String getJwtTokenFromCookies(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("jwt_token".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
    
    // Remove JWT token from cookies (logout)
    public void removeJwtToken(HttpServletResponse response) {
        Cookie cookie = new Cookie("jwt_token", null);
        cookie.setMaxAge(0);
        cookie.setPath("/");
        response.addCookie(cookie);
        System.out.println("JWT token removed from cookies");
    }
    
    // Store user in session
    public void storeUserInSession(HttpSession session, UserResponseDTO user) {
        session.setAttribute("currentUser", user);
        System.out.println("User stored in session: " + user);
    }
    
    // Get user from session
    public UserResponseDTO getUserFromSession(HttpSession session) {
        UserResponseDTO user = (UserResponseDTO) session.getAttribute("currentUser");
        System.out.println("Retrieved user from session: " + user);
        return user;
    }
    
    // Clear user from session
    public void clearUserFromSession(HttpSession session) {
        session.removeAttribute("currentUser");
        System.out.println("User cleared from session");
    }
}