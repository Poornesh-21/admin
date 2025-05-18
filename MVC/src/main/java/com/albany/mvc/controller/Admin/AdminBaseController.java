package com.albany.mvc.controller.Admin;

import com.albany.mvc.controller.BaseController;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.ui.Model;

public abstract class AdminBaseController extends BaseController {

    protected void addCommonAttributes(Model model) {
        model.addAttribute("userName", "Arthur Morgan");
    }

    protected String handleInvalidToken() {
        return "redirect:/admin/login?error=session_expired";
    }
}