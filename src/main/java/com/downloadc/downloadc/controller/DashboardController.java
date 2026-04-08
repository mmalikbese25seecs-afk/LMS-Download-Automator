package com.downloadc.downloadc.controller;

import com.downloadc.downloadc.api.DashboardService;
import com.downloadc.downloadc.api.MoodleApiClient;
import com.downloadc.downloadc.config.SessionManager;
import com.downloadc.downloadc.model.MoodleConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired
    private SessionManager sessionManager;

    // DashboardService is built per-request — needs user token from active session
    @GetMapping("/summary")
    public ResponseEntity<?> getSummary() {

        // Reject if no active session
        if (!sessionManager.isLoggedIn()) {
            return ResponseEntity.status(401)
                    .body(Map.of("error", "Not logged in"));
        }