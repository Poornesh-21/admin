package com.albany.mvc.controller.serviceAdvisor;

import com.albany.mvc.controller.BaseController;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.ui.Model;

public abstract class ServiceAdvisorBaseController extends BaseController {

    protected void addCommonAttributes(Model model, HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            String firstName = (String) session.getAttribute("firstName");
            String lastName = (String) session.getAttribute("lastName");
            if (firstName != null && lastName != null) {
                model.addAttribute("userName", firstName + " " + lastName);
            } else {
                model.addAttribute("userName", "Service Advisor");
            }
        } else {
            model.addAttribute("userName", "Service Advisor");
        }
    }

    protected String handleInvalidToken() {
        return "redirect:/serviceAdvisor/login?error=session_expired";
    }
}