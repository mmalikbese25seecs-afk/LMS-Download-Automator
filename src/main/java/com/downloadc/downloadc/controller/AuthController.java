package com.downloadc.downloadc.controller;

import com.downloadc.downloadc.api.AuthService;
import com.downloadc.downloadc.api.MoodleApiClient;
import com.downloadc.downloadc.config.SessionManager;
import com.downloadc.downloadc.model.MoodleConfig;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;


//AuthController authentication related to HTTP endpoints;

//@RestController handles incoming HTTP requests

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    // Session manager to maintain state across requests
    @Autowired
    private SessionManager sessionManager;

    // Handles user login,token and session
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> body) {

        String username = body.get("username");
        String password = body.get("password");

        // Basic validation — reject empty credentials before hitting Moodle
        if (username == null || username.isBlank()
                || password == null || password.isBlank()) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Username and password are required.")
            );
        }

        try {
            MoodleConfig config = new MoodleConfig(username, password);

            // Authenticate and store token
            AuthService authService = new AuthService();
            String token = authService.getToken(config);
            config.setToken(token);

            // Fetch userId and display name from Moodle
            MoodleApiClient apiClient = new MoodleApiClient(config);
            JsonNode siteInfo = apiClient.callFunction("core_webservice_get_site_info");

            config.setUserId(siteInfo.get("userid").asInt());
            String fullName = siteInfo.get("fullname").asText();

            // Store the fully initialized config in the session
            sessionManager.setActiveConfig(config);

            System.out.println("[AuthController] Login successful for: " + fullName);

            //Map.of() creates an immutable map and JSON response without creating a separate class

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("fullName", fullName);
            response.put("userId", config.getUserId());
            response.put("message", "Login successful. Welcome, " + fullName + "!");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.out.println("[AuthController] Login failed: " + e.getMessage());
            return ResponseEntity.status(401).body(
                    Map.of("error", e.getMessage())
            );
        }
    }
    //Clears the active session

    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout() {
        sessionManager.clearSession();
        return ResponseEntity.ok(Map.of("message", "Logged out successfully."));
    }

    //Check if a user is currently log in in front end

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
        if (sessionManager.isLoggedIn()) {
            MoodleConfig config = sessionManager.getActiveConfig();
            return ResponseEntity.ok(Map.of(
                    "loggedIn", true,
                    "userId", config.getUserId()
            ));
        }
        return ResponseEntity.ok(Map.of("loggedIn", false));
    }
}