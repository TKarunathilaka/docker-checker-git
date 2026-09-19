package com.example.demo.controller;

import com.example.demo.model.RecordItem;
import com.example.demo.model.User;
import com.example.demo.service.FileDatabaseService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/dashboard")
public class DashboardController {

    @Autowired
    private FileDatabaseService fileDatabaseService;

    @GetMapping
    public String dashboard(
            @RequestParam(required = false) String saved,
            @RequestParam(required = false) String deleted,
            HttpSession session,
            Model model
    ) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/login";
        }

        List<RecordItem> records = fileDatabaseService.getRecordsByUser(currentUser.getEmail());

        model.addAttribute("user", currentUser);
        model.addAttribute("records", records);
        model.addAttribute("recordCount", records.size());
        model.addAttribute("totalUsers", fileDatabaseService.getAllUsers().size());
        model.addAttribute("dbSizeBytes", fileDatabaseService.getDatabaseSizeBytes());
        model.addAttribute("dbDirectory", fileDatabaseService.getDatabaseDirectoryPath());

        if ("true".equals(saved)) {
            model.addAttribute("successMessage", "New record persisted to text file database!");
        }
        if ("true".equals(deleted)) {
            model.addAttribute("infoMessage", "Record successfully removed from text file database.");
        }

        return "dashboard";
    }

    @PostMapping("/records/create")
    public String createRecord(
            @RequestParam String title,
            @RequestParam String content,
            @RequestParam(required = false, defaultValue = "General") String category,
            HttpSession session
    ) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/login";
        }

        if (title != null && !title.trim().isEmpty() && content != null && !content.trim().isEmpty()) {
            RecordItem item = new RecordItem(currentUser.getEmail(), title.trim(), content.trim(), category);
            fileDatabaseService.saveRecord(item);
        }

        return "redirect:/dashboard?saved=true";
    }

    @PostMapping("/records/delete/{id}")
    public String deleteRecord(
            @PathVariable String id,
            HttpSession session
    ) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/login";
        }

        fileDatabaseService.deleteRecord(id, currentUser.getEmail());
        return "redirect:/dashboard?deleted=true";
    }

    @GetMapping(value = "/api/raw-file", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getRawFile(@RequestParam String file, HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        String content = fileDatabaseService.getRawFileContent(file);
        Map<String, Object> response = new HashMap<>();
        response.put("filename", file);
        response.put("content", content);
        response.put("path", fileDatabaseService.getDatabaseDirectoryPath() + "/" + file);
        return ResponseEntity.ok(response);
    }
}
