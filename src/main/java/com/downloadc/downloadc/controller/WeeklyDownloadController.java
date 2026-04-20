
package com.downloadc.downloadc.controller;

import com.downloadc.downloadc.api.DownloadHistoryService;
import com.downloadc.downloadc.api.MoodleApiClient;
import com.downloadc.downloadc.api.WeeklyFileService;
import com.downloadc.downloadc.config.SessionManager;
import com.downloadc.downloadc.downloader.FileDownloader;
import com.downloadc.downloadc.model.CourseFile;
import com.downloadc.downloadc.model.WeeklySection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

// Shows course content by week

@RestController
@RequestMapping("/api/courses")

public class WeeklyDownloadController {

    @Autowired private SessionManager sessionManager;
    @Autowired private DownloadHistoryService historyService;


//get all weeks with file counts
    @GetMapping("/{courseId}/weeks")
    public ResponseEntity<?> getWeeks(
            @PathVariable int courseId,
            @RequestParam String shortName) {

        // Check if user is logged in first

        if (!sessionManager.isLoggedIn())
            return ResponseEntity.status(401).body(Map.of("error", "Not logged n"));

        try {
            // Make api client and fetch weekly sections

            MoodleApiClient  client = new MoodleApiClient(sessionManager.getActiveConfig());
            WeeklyFileService service =new WeeklyFileService(client);
            List<WeeklySection> weeks = service.getWeeklySections(courseId, shortName);
            return ResponseEntity.ok(weeks);

        } 
       catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    //download specific weeks

    @PostMapping("/{courseId}/weeks/download")
    public ResponseEntity<?> downloadWeeks(
            @PathVariable int courseId,
            @RequestBody  Map<String, Object> body) {

        // Should be logged in
        if (!sessionManager.isLoggedIn())
            return ResponseEntity.status(401).body(Map.of("error", "Not logged in."));

        String shortName = (String) body.getOrDefault("shortName",String.valueOf(courseId));

        // Get week numbers from request body

        @SuppressWarnings("unchecked")

        List<Integer> requested = (List<Integer>) body.get("sectionNumbers");

        // Make sure user actually gave some week numbers
        if (requested == null || requested.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "No section numbers provided"));
        }

        try {
            // setup everything for downloading
            MoodleApiClient   client= new MoodleApiClient(sessionManager.getActiveConfig());
            WeeklyFileService wService = new WeeklyFileService(client);
            FileDownloader    dl = new FileDownloader(
                    sessionManager.getActiveConfig(), historyService);

            // Get all weeks first
            List<WeeklySection> allWeeks =
                    wService.getWeeklySections(courseId, shortName);

            int downloaded = 0, skipped = 0, failed = 0;

            // loop through weeks but only download selected ones

            for (WeeklySection week : allWeeks) {

                if (!requested.contains(week.getSectionNumber())) continue;

                for (CourseFile file : week.getFiles()) {
                    try {
                        if (dl.download(file)){
                         downloaded++;
                          }
                        else{
                         skipped++;
                            }
                    }
                     catch (Exception e) {
                        failed++;
                        // Print error, don't crash everything
                        System.out.println("[WeeklyDownloadController] Failed: "
                                + file.getFileName() + " — " + e.getMessage());
                    }
                }
            }

            // Send back results
            return ResponseEntity.ok(Map.of(
                "downloaded", downloaded,
                "skipped",skipped,
                "failed", failed
            ));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}
