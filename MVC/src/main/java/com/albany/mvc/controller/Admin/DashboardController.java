package com.albany.mvc.controller.Admin;

import com.albany.mvc.dto.DashboardDTO;
import com.albany.mvc.service.DashboardService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
@Slf4j
public class DashboardController extends AdminBaseController {

    private final DashboardService dashboardService;

    @GetMapping("/dashboard")
    public String dashboard(
            @RequestParam(required = false) String token,
            Model model,
            HttpServletRequest request) {
        
        String validToken = getValidToken(token, request);
        if (validToken == null) {
            return handleInvalidToken();
        }

        DashboardDTO dashboardStats = dashboardService.getDashboardData(validToken);
        if (dashboardStats != null) {
            model.addAttribute("dashboardStats", dashboardStats);
        }

        addCommonAttributes(model);
        return "admin/dashboard";
    }

    @GetMapping("/dashboard/api/data")
    @ResponseBody
    public ResponseEntity<?> getDashboardData(
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        String validToken = getValidToken(token, authHeader, request);
        if (validToken == null) {
            return createUnauthorizedResponse();
        }

        try {
            DashboardDTO dashboardData = dashboardService.getDashboardData(validToken);
            if (dashboardData == null) {
                return createErrorResponse("Failed to fetch dashboard data");
            }
            return ResponseEntity.ok(dashboardData);
        } catch (Exception e) {
            return createErrorResponse("Error fetching dashboard data: " + e.getMessage());
        }
    }

    @PutMapping("/dashboard/api/assign/{requestId}")
    public ResponseEntity<?> assignServiceAdvisor(
            @PathVariable Integer requestId,
            @RequestParam Integer advisorId,
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        String validToken = getValidToken(token, authHeader, request);
        if (validToken == null) {
            return createUnauthorizedResponse();
        }

        try {
            var updatedRequest = dashboardService.assignServiceAdvisor(requestId, advisorId, validToken);
            if (updatedRequest == null) {
                return createErrorResponse("Failed to assign service advisor");
            }
            return ResponseEntity.ok(updatedRequest);
        } catch (Exception e) {
            return createErrorResponse("Error assigning service advisor: " + e.getMessage());
        }
    }
}