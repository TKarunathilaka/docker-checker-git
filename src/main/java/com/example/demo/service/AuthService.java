package com.example.demo.service;

import com.example.demo.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;

@Service
public class AuthService {

    private static final String SALT = "DockerDemoSecretSalt2026";

    @Autowired
    private FileDatabaseService fileDatabaseService;

    public static String hashPassword(String password) {
        if (password == null) password = "";
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((SALT + password).getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    public Optional<User> authenticate(String email, String rawPassword) {
        if (email == null || rawPassword == null) return Optional.empty();
        Optional<User> userOpt = fileDatabaseService.findUserByEmail(email);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            String hashedInput = hashPassword(rawPassword);
            if (hashedInput.equals(user.getPasswordHash())) {
                return Optional.of(user);
            }
        }
        return Optional.empty();
    }

    public boolean register(String name, String email, String rawPassword) {
        if (name == null || email == null || rawPassword == null) return false;
        name = name.trim();
        email = email.trim().toLowerCase();
        if (name.isEmpty() || email.isEmpty() || rawPassword.length() < 4) {
            return false;
        }

        if (fileDatabaseService.findUserByEmail(email).isPresent()) {
            return false; // Email already taken
        }

        String hashedPassword = hashPassword(rawPassword);
        User newUser = new User(name, email, hashedPassword);
        return fileDatabaseService.saveUser(newUser);
    }
}
