package com.albany.mvc.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CustomAuthenticationProvider implements AuthenticationProvider {

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        // This provider doesn't actually do any authentication
        // It just exists to prevent the automatic creation of a DaoAuthenticationProvider
        // All real authentication is handled by our LoginController and the JwtAuthenticationFilter
        return null;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        // Only support UsernamePasswordAuthenticationToken
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}