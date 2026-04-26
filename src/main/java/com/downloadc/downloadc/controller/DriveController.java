package com.downloadc.downloadc.controller;

import com.downloadc.downloadc.api.GoogleDriveService;
import com.downloadc.downloadc.config.SessionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

// Controller that exposes all Google Drive endpoints to the frontend.
// The frontend calls: /api/drive/status, /api/drive/connect,
//                     /api/drive/disconnect, /api/drive/upload/{courseName}
@RestController
@RequestMapping("/api/drive")
public class DriveController {

    @Autowired
    private SessionManager sessionManager;

    @Autowired
    private GoogleDriveService driveService;

    // Called on every dashboard load so the UI knows whether to show Drive buttons.
    // Returns: configured (credentials.json present) + authorized (token saved)
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
        return ResponseEntity.ok(Map.of(
                "configured",  driveService.isConfigured(),
                "authorized",  driveService.isAuthorized()
        ));
    }

    // Triggers the OAuth browser flow. On first run the user logs in via Google;
    // afterward the saved token is reused silently.
    @PostMapping("/connect")
    public ResponseEntity<Map<String, Object>> connect() {
        if (!sessionManager.isLoggedIn())
            return ResponseEntity.status(401).body(Map.of("error", "Not logged in."));

        if (!driveService.isConfigured())
            return ResponseEntity.status(400).body(Map.of(
                    "error", "credentials.json not found. " +
                            "Download it from Google Cloud Console and place it in " +
                            "src/main/resources/credentials.json"));
        try {
            String message = driveService.authorize();
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", message
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // Clears the saved OAuth token so the user can switch Google accounts.
    @PostMapping("/disconnect")
    public ResponseEntity<Map<String, Object>> disconnect() {
        driveService.disconnect();
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Google Drive disconnected."
        ));
    }

    // Uploads all locally-downloaded files for a course to Drive.
    // Creates:  LMS Downloads/{courseName}/  inside the user's Drive.
    // Skips files that are already there (duplicate check by name).
    @PostMapping("/upload/{courseName}")
    public ResponseEntity<Map<String, Object>> uploadCourse(
            @PathVariable String courseName) {

        if (!sessionManager.isLoggedIn())
            return ResponseEntity.status(401).body(Map.of("error", "Not logged in."));

        if (!driveService.isAuthorized())
            return ResponseEntity.status(400).body(Map.of(
                    "error", "Google Drive not connected. Click 'Connect Drive' first."));

        try {
            GoogleDriveService.UploadResult result = driveService.uploadCourse(courseName);

            String message = String.format(
                    "Uploaded %d file(s) to Drive. Skipped %d (already there). Failed %d.",
                    result.uploaded(), result.skipped(), result.failed());

            return ResponseEntity.ok(Map.of(
                    "success",    true,
                    "message",    message,
                    "uploaded",   result.uploaded(),
                    "skipped",    result.skipped(),
                    "failed",     result.failed(),
                    "courseName", result.courseName()
            ));

        } catch (Exception e) {
            System.err.println("[DriveController] Upload failed: " + e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}