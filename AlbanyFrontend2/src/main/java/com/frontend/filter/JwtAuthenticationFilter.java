package com.frontend.filter;

import com.frontend.dto.UserResponseDTO;
import com.frontend.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, 
            HttpServletResponse response, 
            FilterChain filterChain
    ) throws ServletException, IOException {
        
        // Get JWT token from cookies
        String token = jwtUtil.getJwtTokenFromCookies(request);
        
        // Get user from session
        HttpSession session = request.getSession(false);
        UserResponseDTO user = (session != null) ? jwtUtil.getUserFromSession(session) : null;
        
        // If token exists and user is in session and not already authenticated
        if (token != null && user != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            // Create authority from user role (singular)
            SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + user.getRole());
            
            // Create authentication token with a single authority
            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                    user.getEmail(), null, Collections.singletonList(authority));
            
            // Set details
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            
            // Set authentication in security context
            SecurityContextHolder.getContext().setAuthentication(authToken);
        }
        
        filterChain.doFilter(request, response);
    }
}