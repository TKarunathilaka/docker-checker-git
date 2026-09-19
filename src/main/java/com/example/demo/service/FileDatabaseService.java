package com.example.demo.service;

import com.example.demo.model.RecordItem;
import com.example.demo.model.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Service
public class FileDatabaseService {

    @Value("${app.database.dir:./data}")
    private String databaseDir;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ReentrantReadWriteLock usersLock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock recordsLock = new ReentrantReadWriteLock();

    private Path usersFilePath;
    private Path recordsFilePath;

    @PostConstruct
    public void init() {
        try {
            Path dir = Paths.get(databaseDir).toAbsolutePath().normalize();
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }
            usersFilePath = dir.resolve("users.txt");
            recordsFilePath = dir.resolve("user_records.txt");

            if (!Files.exists(usersFilePath)) {
                Files.createFile(usersFilePath);
                // Seed initial demo user
                // Hash for 'Password123!'
                String defaultHash = AuthService.hashPassword("Password123!");
                User demoUser = new User("Demo Explorer", "demo@example.com", defaultHash);
                demoUser.setId("usr_demo01");
                saveUser(demoUser);
            }

            if (!Files.exists(recordsFilePath)) {
                Files.createFile(recordsFilePath);
                // Seed demo records
                RecordItem seed1 = new RecordItem(
                        "demo@example.com",
                        "Welcome to Text File Database Demo!",
                        "Every single item on this dashboard is persisted directly into a plain text file (data/user_records.txt) on your disk. No SQL server needed!",
                        "System"
                );
                RecordItem seed2 = new RecordItem(
                        "demo@example.com",
                        "Docker Migration Roadmap",
                        "1. Build Spring Boot JAR with Maven wrapper\n2. Create multi-stage Dockerfile\n3. Mount the ./data directory as a Docker volume to persist data across container restarts!",
                        "DevOps"
                );
                saveRecord(seed1);
                saveRecord(seed2);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize File Database storage at " + databaseDir, e);
        }
    }

    public List<User> getAllUsers() {
        usersLock.readLock().lock();
        try {
            List<User> users = new ArrayList<>();
            if (!Files.exists(usersFilePath)) {
                return users;
            }
            List<String> lines = Files.readAllLines(usersFilePath, StandardCharsets.UTF_8);
            for (String line : lines) {
                line = line.trim();
                if (!line.isEmpty()) {
                    try {
                        users.add(objectMapper.readValue(line, User.class));
                    } catch (Exception ignored) {
                    }
                }
            }
            return users;
        } catch (IOException e) {
            return Collections.emptyList();
        } finally {
            usersLock.readLock().unlock();
        }
    }

    public Optional<User> findUserByEmail(String email) {
        if (email == null) return Optional.empty();
        String target = email.trim().toLowerCase();
        return getAllUsers().stream()
                .filter(u -> target.equalsIgnoreCase(u.getEmail()))
                .findFirst();
    }

    public boolean saveUser(User user) {
        usersLock.writeLock().lock();
        try {
            String json = objectMapper.writeValueAsString(user);
            Files.writeString(
                    usersFilePath,
                    json + System.lineSeparator(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );
            return true;
        } catch (IOException e) {
            return false;
        } finally {
            usersLock.writeLock().unlock();
        }
    }

    public List<RecordItem> getRecordsByUser(String email) {
        recordsLock.readLock().lock();
        try {
            List<RecordItem> list = new ArrayList<>();
            if (!Files.exists(recordsFilePath)) {
                return list;
            }
            List<String> lines = Files.readAllLines(recordsFilePath, StandardCharsets.UTF_8);
            String target = email.trim().toLowerCase();
            for (String line : lines) {
                line = line.trim();
                if (!line.isEmpty()) {
                    try {
                        RecordItem item = objectMapper.readValue(line, RecordItem.class);
                        if (target.equalsIgnoreCase(item.getUserEmail())) {
                            list.add(item);
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
            // Sort newest first
            Collections.reverse(list);
            return list;
        } catch (IOException e) {
            return Collections.emptyList();
        } finally {
            recordsLock.readLock().unlock();
        }
    }

    public boolean saveRecord(RecordItem record) {
        recordsLock.writeLock().lock();
        try {
            String json = objectMapper.writeValueAsString(record);
            Files.writeString(
                    recordsFilePath,
                    json + System.lineSeparator(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );
            return true;
        } catch (IOException e) {
            return false;
        } finally {
            recordsLock.writeLock().unlock();
        }
    }

    public boolean deleteRecord(String recordId, String userEmail) {
        recordsLock.writeLock().lock();
        try {
            if (!Files.exists(recordsFilePath)) {
                return false;
            }
            List<String> lines = Files.readAllLines(recordsFilePath, StandardCharsets.UTF_8);
            List<String> updatedLines = new ArrayList<>();
            String targetUser = userEmail.trim().toLowerCase();
            boolean removed = false;

            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) continue;
                try {
                    RecordItem item = objectMapper.readValue(trimmed, RecordItem.class);
                    if (recordId.equals(item.getId()) && targetUser.equalsIgnoreCase(item.getUserEmail())) {
                        removed = true;
                        continue; // skip writing this one
                    }
                } catch (Exception ignored) {
                }
                updatedLines.add(line);
            }

            if (removed) {
                Files.write(recordsFilePath, updatedLines, StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            }
            return removed;
        } catch (IOException e) {
            return false;
        } finally {
            recordsLock.writeLock().unlock();
        }
    }

    public long getDatabaseSizeBytes() {
        long total = 0;
        try {
            if (usersFilePath != null && Files.exists(usersFilePath)) {
                total += Files.size(usersFilePath);
            }
            if (recordsFilePath != null && Files.exists(recordsFilePath)) {
                total += Files.size(recordsFilePath);
            }
        } catch (IOException ignored) {
        }
        return total;
    }

    public String getRawFileContent(String filename) {
        try {
            Path target;
            if ("users.txt".equalsIgnoreCase(filename)) {
                target = usersFilePath;
            } else if ("user_records.txt".equalsIgnoreCase(filename)) {
                target = recordsFilePath;
            } else {
                return "Unauthorized or invalid file requested.";
            }

            if (target != null && Files.exists(target)) {
                return Files.readString(target, StandardCharsets.UTF_8);
            }
            return "File does not exist yet.";
        } catch (IOException e) {
            return "Error reading file: " + e.getMessage();
        }
    }

    public String getDatabaseDirectoryPath() {
        return (usersFilePath != null && usersFilePath.getParent() != null)
                ? usersFilePath.getParent().toString()
                : databaseDir;
    }
}
