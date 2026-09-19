package com.example.demo.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RecordItem {
    private String id;
    private String userEmail;
    private String title;
    private String content;
    private String category;
    private String createdAt;

    public RecordItem() {
    }

    public RecordItem(String userEmail, String title, String content, String category) {
        this.id = "rec_" + UUID.randomUUID().toString().substring(0, 8);
        this.userEmail = userEmail.toLowerCase().trim();
        this.title = title;
        this.content = content;
        this.category = (category == null || category.trim().isEmpty()) ? "General" : category.trim();
        this.createdAt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
