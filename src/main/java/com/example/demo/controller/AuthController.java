package com.example.demo.controller;

import com.example.demo.model.User;
import com.example.demo.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

@Controller
public class AuthController {

    @Autowired
    private AuthService authService;

    @GetMapping("/login")
    public String loginPage(
            @RequestParam(required = false) String error,
            @RequestParam(required = false) String registered,
            @RequestParam(required = false) String loggedOut,
            HttpSession session,
            Model model
    ) {
        if (session != null && session.getAttribute("currentUser") != null) {
            return "redirect:/dashboard";
        }
        if (error != null) {
            if ("invalid_credentials".equals(error)) {
                model.addAttribute("errorMessage", "Invalid email or password. Please try again.");
            } else if ("session_expired".equals(error)) {
                model.addAttribute("errorMessage", "Please log in to access your dashboard.");
            } else {
                model.addAttribute("errorMessage", "An error occurred. Please try again.");
            }
        }
        if ("true".equals(registered)) {
            model.addAttribute("successMessage", "Account created successfully! You can now log in.");
        }
        if ("true".equals(loggedOut)) {
            model.addAttribute("infoMessage", "You have been logged out securely.");
        }
        return "login";
    }

    @PostMapping("/login")
    public String processLogin(
            @RequestParam String email,
            @RequestParam String password,
            HttpServletRequest request,
            Model model
    ) {
        Optional<User> userOpt = authService.authenticate(email, password);
        if (userOpt.isPresent()) {
            HttpSession session = request.getSession(true);
            session.setAttribute("currentUser", userOpt.get());
            return "redirect:/dashboard";
        }
        return "redirect:/login?error=invalid_credentials";
    }

    @GetMapping("/register")
    public String registerPage(HttpSession session, Model model) {
        if (session != null && session.getAttribute("currentUser") != null) {
            return "redirect:/dashboard";
        }
        return "register";
    }

    @PostMapping("/register")
    public String processRegister(
            @RequestParam String name,
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam String confirmPassword,
            Model model
    ) {
        if (name == null || name.trim().isEmpty()) {
            model.addAttribute("errorMessage", "Please enter your full name.");
            return "register";
        }
        if (email == null || email.trim().isEmpty() || !email.contains("@")) {
            model.addAttribute("errorMessage", "Please enter a valid email address.");
            return "register";
        }
        if (password == null || password.length() < 4) {
            model.addAttribute("errorMessage", "Password must be at least 4 characters.");
            return "register";
        }
        if (!password.equals(confirmPassword)) {
            model.addAttribute("errorMessage", "Passwords do not match.");
            return "register";
        }

        boolean created = authService.register(name, email, password);
        if (!created) {
            model.addAttribute("errorMessage", "An account with that email already exists.");
            return "register";
        }

        return "redirect:/login?registered=true";
    }

    @GetMapping("/logout")
    public String logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return "redirect:/login?loggedOut=true";
    }
}
