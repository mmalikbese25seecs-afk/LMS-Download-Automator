package com.downloadc.downloadc.controller;

import com.downloadc.downloadc.api.FileService;
import com.downloadc.downloadc.api.MoodleApiClient;
import com.downloadc.downloadc.config.SessionManager;
import com.downloadc.downloadc.model.Course;
import com.downloadc.downloadc.model.CourseFile;
import com.downloadc.downloadc.downloader.FileDownloader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;


 //F ile listing and downloading over Http

@RestController
@RequestMapping("/api")
public class FileController {

    @Autowired
    private SessionManager sessionManager;

    @GetMapping("/courses/{courseId}/files")
    public ResponseEntity<?> getFilesForCourse(@PathVariable int courseId) {
// Validate user session authorizatio
        if (!sessionManager.isLoggedIn()) {
            return ResponseEntity.status(401).body(
                    Map.of("error", "Not logged in.")
            );
        }

        try {
            // Initialize API client and service using the active session configuration
            MoodleApiClient apiClient = new MoodleApiClient(sessionManager.getActiveConfig());
            FileService fileService   = new FileService(apiClient);


             // FileService.getFilesForCourse() needs a Course object, not just an ID.

            Course course = new Course(courseId, "", String.valueOf(courseId), "");
            List<CourseFile> files = fileService.getFilesForCourse(course);

            return ResponseEntity.ok(files);

        } catch (Exception e) {
            return ResponseEntity.status(500).body(
                    Map.of("error", "Failed to fetch files: " + e.getMessage())
            );
        }
    }


    @PostMapping("/download/{courseId}")
    public ResponseEntity<?> downloadCourse(@PathVariable int courseId,
                                            @RequestBody Map<String, String> body) {

        // Ensure request is authorized before processing download
        if (!sessionManager.isLoggedIn()) {
            return ResponseEntity.status(401).body(
                    Map.of("error", "Not logged in.")
            );
        }

        try {
            MoodleApiClient apiClient    = new MoodleApiClient(sessionManager.getActiveConfig());
            FileService fileService      = new FileService(apiClient);
            FileDownloader fileDownloader = new FileDownloader(sessionManager.getActiveConfig());

            // Use the shortName passed from the frontend for proper folder naming
            String shortName = body.getOrDefault("shortName", String.valueOf(courseId));
            Course course    = new Course(courseId, "", shortName, "");

            List<CourseFile> files = fileService.getFilesForCourse(course);

            int downloaded = 0, skipped = 0, failed = 0;

            for (CourseFile file : files) {
                try {
                    if (fileDownloader.download(file)) downloaded++;
                    else skipped++;
                } catch (Exception e) {
                    failed++;
                    System.out.println("[FileController] Failed: "
                            + file.getFileName() + " — " + e.getMessage());
                }
            }

            return ResponseEntity.ok(Map.of(
                    "courseId",    courseId,
                    "downloaded",  downloaded,
                    "skipped",     skipped,
                    "failed",      failed,
                    "totalFiles",  files.size()
            ));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(
                    Map.of("error", "Download failed: " + e.getMessage())
            );
        }
    }
}
