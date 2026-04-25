package com.downloadc.downloadc.controller;

import com.downloadc.downloadc.api.DownloadHistoryService;
import com.downloadc.downloadc.api.FileService;
import com.downloadc.downloadc.api.MoodleApiClient;
import com.downloadc.downloadc.config.SessionManager;
import com.downloadc.downloadc.downloader.FileDownloader;
import com.downloadc.downloadc.model.Course;
import com.downloadc.downloadc.model.CourseFile;
import com.downloadc.downloadc.model.DownloadStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

// Controller for course files and downloading
@RestController
@RequestMapping("/api")
public class FileController {

    @Autowired
    private SessionManager sessionManager;

    @Autowired
    private DownloadHistoryService historyService;

    // Get all files for a course
    @GetMapping("/courses/{courseId}/files")
    public ResponseEntity<?> getFilesForCourse(@PathVariable int courseId) {

        // check login
        if (!sessionManager.isLoggedIn()) {
            return ResponseEntity.status(401)
                    .body(Map.of("error", "Not logged in."));
        }

        try {
            MoodleApiClient apiClient = new MoodleApiClient(sessionManager.getActiveConfig());
            FileService fileService = new FileService(apiClient);

            // create course object
            Course course = new Course(courseId, "", String.valueOf(courseId), "");

            // return files
            return ResponseEntity.ok(fileService.getFilesForCourse(course));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // download all files of a course
    @PostMapping("/download/{courseId}")
    public ResponseEntity<?> downloadCourse(
            @PathVariable int courseId,
            @RequestBody Map<String, String> body) {

        // check login
        if (!sessionManager.isLoggedIn())
            return ResponseEntity.status(401).body(Map.of("error", "Not logged in."));

        try {
            // setup api + downloader
            MoodleApiClient apiClient = new MoodleApiClient(sessionManager.getActiveConfig());
            FileService fileService = new FileService(apiClient);
            FileDownloader fileDownloader = new FileDownloader(
                    sessionManager.getActiveConfig(), historyService);

            // get course name
            String shortName = body.getOrDefault("shortName", String.valueOf(courseId));
            Course course = new Course(courseId, "", shortName, "");

            // get all files
            List<CourseFile> files = fileService.getFilesForCourse(course);

            // counters
            int downloaded = 0, updated = 0, resumed = 0, skipped = 0, failed = 0;

            // loop through files
            for (CourseFile file : files) {
                try {
                    // call downloader and count result
                    switch (fileDownloader.download(file)) {
                        case DOWNLOADED -> downloaded++;
                        case UPDATED    -> updated++;
                        case RESUMED    -> resumed++;
                        case SKIPPED    -> skipped++;
                        case FAILED     -> failed++;
                    }
                } catch (Exception e) {
                    failed++;
                    System.out.println("Failed : "
                            + file.getFileName() + " — " + e.getMessage());
                }
            }

            // return summary
            return ResponseEntity.ok(Map.of(
                    "courseId",    courseId,
                    "totalFiles",  files.size(),
                    "downloaded",  downloaded,
                    "updated",     updated,
                    "resumed",     resumed,
                    "skipped",     skipped,
                    "failed",      failed
            ));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("Error", e.getMessage()));
        }
    }
}