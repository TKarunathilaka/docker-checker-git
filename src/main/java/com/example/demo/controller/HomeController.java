package com.example.demo.controller;

import com.example.demo.model.User;
import com.example.demo.service.FileDatabaseService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @Autowired
    private FileDatabaseService fileDatabaseService;

    @GetMapping("/")
    public String index(HttpSession session, Model model) {
        User currentUser = (User) session.getAttribute("currentUser");
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("userCount", fileDatabaseService.getAllUsers().size());
        model.addAttribute("dbSizeBytes", fileDatabaseService.getDatabaseSizeBytes());
        model.addAttribute("dbPath", fileDatabaseService.getDatabaseDirectoryPath());
        return "index";
    }
}
