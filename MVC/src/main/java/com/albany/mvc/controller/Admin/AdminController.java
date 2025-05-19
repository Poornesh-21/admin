package com.albany.mvc.controller.Admin;

import com.albany.mvc.controller.BaseController;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController extends BaseController {

    @GetMapping("/overview")
    public String adminOverview(
            @RequestParam(required = false) String token,
            Model model,
            HttpServletRequest request) {

        String validToken = getValidToken(token, request);
        if (validToken == null) {
            return "redirect:/admin/login?error=session_expired";
        }


        model.addAttribute("userName", "Admin User");

        return "admin/overview";
    }
    @GetMapping("/welcome")
    public String welcome(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("jwt-token") != null) {
            return "redirect:/admin/dashboard";
        }
        return "redirect:/admin/login";
    }
}