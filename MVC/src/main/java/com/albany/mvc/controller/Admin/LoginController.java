package com.albany.mvc.controller.Admin;

import com.albany.mvc.controller.BaseController;
import com.albany.mvc.dto.AuthRequest;
import com.albany.mvc.dto.AuthResponse;
import com.albany.mvc.service.AuthenticationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

import java.util.Map;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
@Slf4j
public class LoginController extends BaseController {

    private final AuthenticationService authService;

    @GetMapping("/login")
    public String loginPage(
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "logout", required = false) String logout,
            Model model
    ) {
        if (error != null) {
            String errorMessage;
            switch (error) {
                case "session_expired":
                    errorMessage = "Your session has expired. Please log in again.";
                    break;
                case "unauthorized":
                    errorMessage = "You do not have permission to access this resource.";
                    break;
                case "invalid_credentials":
                    errorMessage = "Invalid email or password. Please check your credentials.";
                    break;
                case "server_error":
                    errorMessage = "Could not connect to authentication server. Please try again later.";
                    break;
                default:
                    errorMessage = "Authentication failed. Please verify your email and password.";
                    break;
            }
            model.addAttribute("error", errorMessage);
        }

        if (logout != null) {
            model.addAttribute("message", "You have been logged out successfully");
        }

        return "admin/login";
    }

    @PostMapping("/api/login")
    @ResponseBody
    public ResponseEntity<?> apiLogin(@RequestBody AuthRequest request) {
        try {
            AuthResponse authResponse = authService.authenticate(request);

            String role = authResponse.getRole();
            if (role != null) {
                role = role.toUpperCase().replace("\"", "").trim();
            }

            if (!"ADMIN".equals(role)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "You do not have permission to access the admin portal"));
            }

            return ResponseEntity.ok(authResponse);
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid email or password. Please try again."));
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED || e.getStatusCode() == HttpStatus.FORBIDDEN) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Invalid email or password. Please try again."));
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("message", "Authentication service error: " + e.getStatusCode()));
            }
        } catch (RestClientException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("message", "Unable to connect to authentication server. Please try again later."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "An unexpected error occurred: " + e.getMessage()));
        }
    }

    @GetMapping("/logout")
    public String logout() {
        return "redirect:/admin/login?logout=true";
    }
}