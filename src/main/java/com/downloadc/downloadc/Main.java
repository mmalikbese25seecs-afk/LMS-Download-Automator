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
            CourseService courseService   = new CourseService(apiClient, config);
            List<Course> courses          = courseService.getEnrolledCourses();
            System.out.println("Courses found: " + courses.size() + "\n");

            // Download files from every course
            FileService    fileService    = new FileService(apiClient);
            FileDownloader fileDownloader = new FileDownloader(config);

            int totalDownloaded = 0;
            int totalSkipped    = 0;

            for (Course course : courses) {

                List<CourseFile> files = fileService.getFilesForCourse(course);

                for (CourseFile file : files) {
                    try {
                        boolean downloaded = fileDownloader.download(file);
                        if (downloaded) totalDownloaded++;
                        else            totalSkipped++;
                    } catch (Exception e) {
                        // One failed file should not stop the entire download run
                        System.out.println("Failed to download "
                                + file.getFileName() + ": " + e.getMessage());
                    }
                }
            }

            //

            System.out.println("Download Complete");
            System.out.printf("Downloaded : %d file(s)%n", totalDownloaded);
            System.out.printf("Skipped : %d file(s) (already existed)%n", totalSkipped);
            System.out.println("Location : ./downloads/");


        } catch (Exception e) {
            System.out.println("\nFatal error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
