package com.downloadc.downloadc.controller;

import com.downloadc.downloadc.api.DownloadHistoryService;
import com.downloadc.downloadc.api.FileService;
import com.downloadc.downloadc.api.MoodleApiClient;
import com.downloadc.downloadc.config.SessionManager;
import com.downloadc.downloadc.downloader.FileDownloader;
import com.downloadc.downloadc.model.Course;
import com.downloadc.downloadc.model.CourseFile;
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

// Log downloads
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

            List<CourseFile> files = fileService.getFilesForCourse(course);

            return ResponseEntity.ok(files);

        } 
        catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // download all files of a course
    @PostMapping("/download/{courseId}")
    public ResponseEntity<?> downloadCourse(@PathVariable int courseId,@RequestBody Map<String, String> body) {

        if (!sessionManager.isLoggedIn()) {
            return ResponseEntity.status(401) .body(Map.of("error", "Not logged in."));
        }

        try {
            MoodleApiClient apiClient = new MoodleApiClient(sessionManager.getActiveConfig());
            FileService fileService = new FileService(apiClient);

            // Pass history service so downloads get saved
            FileDownloader fileDownloader =new FileDownloader(sessionManager.getActiveConfig(), historyService);

            String shortName = body.getOrDefault("shortName", String.valueOf(courseId));

            Course course = new Course(courseId, "", shortName, "");

            List<CourseFile> files = fileService.getFilesForCourse(course);

            int downloaded = 0;
            int skipped = 0;
            int failed = 0;

            // loop through all files
            for (CourseFile file : files) {
                try {
                    if (fileDownloader.download(file))
                        downloaded++;
                    else
                        skipped++;

                } catch (Exception e) {
                    failed++;

                    System.out.println("Download failed: "
                            + file.getFileName() + " - " + e.getMessage());
                }
            }

            return ResponseEntity.ok(Map.of(
                    "courseId", courseId,
                    "downloaded", downloaded,
                    "skipped", skipped,
                    "failed", failed,
                    "totalFiles", files.size()
            ));

        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
