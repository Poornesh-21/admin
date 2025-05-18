package com.frontend.util;

import com.frontend.filter.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // Public resources
                .requestMatchers("/", "/css/**", "/js/**", "/images/**", "/assets/**", "/fonts/**").permitAll()
                // Authentication endpoints
                .requestMatchers("/authentication/**").permitAll()
                // Membership endpoints
                .requestMatchers("/api/membership/**").authenticated()
                // Public pages
                .requestMatchers("/customer/index").permitAll()
                // Protected pages
                .requestMatchers("/customer/bookService").authenticated()
                // Allow all other requests with authentication
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/authentication/login")
                .defaultSuccessUrl("/customer/bookService", true)
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/authentication/logout")
                .logoutSuccessUrl("/")
                .clearAuthentication(true)
                .invalidateHttpSession(true)
                .deleteCookies("jwt_token")
                .permitAll()
            )
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.ALWAYS))
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
            
        return http.build();
    }
}