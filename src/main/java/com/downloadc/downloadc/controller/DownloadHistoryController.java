package com.downloadc.downloadc.controller;

import com.downloadc.downloadc.api.DownloadHistoryService;
import com.downloadc.downloadc.config.SessionManager;
import com.downloadc.downloadc.model.DownloadRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

// Handles download history APIs
@RestController
@RequestMapping("/api/history")
public class DownloadHistoryController {

    @Autowired
    private SessionManager sessionManager;

    @Autowired
    private DownloadHistoryService historyService;

    //Get all download history
    @GetMapping
    public ResponseEntity<?> getHistory() {

        // Check if user logged in
        if (!sessionManager.isLoggedIn()) {
            return ResponseEntity.status(401).body(Map.of("error", "Not logged in"));
        }

        List<DownloadRecord> records = historyService.readAll();

        // Show newest first
        java.util.Collections.reverse(records);

        return ResponseEntity.ok(records);
    }

    // Gets. stats 
    @GetMapping("/stats")
    public ResponseEntity<?> getStats() {

        if (!sessionManager.isLoggedIn()) {
            return ResponseEntity.status(401).body(Map.of("error", "Not logged in."));
        }

        return ResponseEntity.ok(historyService.getStats());
    }

    // Clear all history
    @DeleteMapping
    public ResponseEntity<?> clearHistory() {

        if (!sessionManager.isLoggedIn()) {
            return ResponseEntity.status(401)
                    .body(Map.of("error", "Not log in"));
        }

        try {
            historyService.clearAll();
            return ResponseEntity.ok(Map.of("message", "History cleared"));

        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}

