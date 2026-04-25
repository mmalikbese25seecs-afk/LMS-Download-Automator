package com.downloadc.downloadc;

import com.downloadc.downloadc.api.*;
import com.downloadc.downloadc.downloader.*;
import com.downloadc.downloadc.model.*;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

public class Main {

    public static void main(String[] args) {

        MoodleConfig config = new MoodleConfig("ID", "PASSword");

        System.out.println("LMS Automator");

        try {
            // Login
            AuthService authService = new AuthService();
            String token = authService.getToken(config);
            config.setToken(token);

            MoodleApiClient apiClient = new MoodleApiClient(config);

            // Get userId from site info
            JsonNode siteInfo = apiClient.callFunction("core_webservice_get_site_info");
            config.setUserId(siteInfo.get("userid").asInt());
            System.out.println("Logged in as: " + siteInfo.get("fullname").asText());

            // Fetch courses
            CourseService    courseService = new CourseService(apiClient, config);
            List<Course>     courses       = courseService.getEnrolledCourses();
            System.out.println("Courses found: " + courses.size() + "\n");

            // Download files from every course
            FileService           fileService    = new FileService(apiClient);
            DownloadHistoryService historyService = new DownloadHistoryService();
            FileDownloader         fileDownloader = new FileDownloader(config, historyService);

            int totalDownloaded = 0;
            int totalUpdated    = 0;
            int totalResumed    = 0;
            int totalSkipped    = 0;
            int totalFailed     = 0;

            for (Course course : courses) {

                List<CourseFile> files = fileService.getFilesForCourse(course);

                for (CourseFile file : files) {
                    try {
                        switch (fileDownloader.download(file)) {
                            case DOWNLOADED -> totalDownloaded++;
                            case UPDATED    -> totalUpdated++;
                            case RESUMED    -> totalResumed++;
                            case SKIPPED    -> totalSkipped++;
                            case FAILED     -> totalFailed++;
                        }
                    } catch (Exception e) {
                        // One failed file should not stop the entire download run
                        totalFailed++;
                        System.out.println("Failed to download "
                                + file.getFileName() + ": " + e.getMessage());
                    }
                }
            }

            System.out.println("\nDownload Complete");
            System.out.printf("Downloaded : %d file(s)%n",              totalDownloaded);
            System.out.printf("Updated    : %d file(s)%n",              totalUpdated);
            System.out.printf("Resumed    : %d file(s)%n",              totalResumed);
            System.out.printf("Skipped    : %d file(s) (already exist)%n", totalSkipped);
            System.out.printf("Failed     : %d file(s)%n",              totalFailed);
            System.out.println("Location   : ./downloads/");

        } catch (Exception e) {
            System.out.println("\nFatal error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}