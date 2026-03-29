package com.downloadc.downloadc;

import com.downloadc.downloadc.api.AuthService;
import com.downloadc.downloadc.api.CourseService;
import com.downloadc.downloadc.api.MoodleApiClient;
import com.downloadc.downloadc.model.Course;
import com.downloadc.downloadc.model.MoodleConfig;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

public class Main {

    public static void main(String[] args) {

        // Config with credentials
        MoodleConfig config = new MoodleConfig("Id", "passs");
        System.out.println();
        System.out.println("LMS Automator");
        System.out.println();

        try {
            // Login to get the token
            AuthService authService = new AuthService();
            String token = authService.getToken(config);
            config.setToken(token);

            // core_webservice_get_site_info returns the logged-in user's details
            // including their numeric userId which we need for course fetching
            MoodleApiClient apiClient = new MoodleApiClient(config);
            JsonNode siteInfo = apiClient.callFunction("core_webservice_get_site_info");

            int userId = siteInfo.get("userid").asInt();
            String fullName = siteInfo.get("fullname").asText();
            config.setUserId(userId);

            System.out.println("\n Logged in as: " + fullName);
            System.out.println("User ID: " + userId);

            // Fetch enrolled courses
            CourseService courseService = new CourseService(apiClient, config);
            List<Course> courses = courseService.getEnrolledCourses();

            // Print all coursrs

            System.out.println("  YOUR ENROLLED COURSES (" + courses.size() + ")");


            for (int i = 0; i < courses.size(); i++) {
                Course course = courses.get(i);
                System.out.printf("  %2d. [ID: %5d] %s%n",
                        (i + 1),
                        course.getId(),
                        course.getFullName());
            }


        } catch (Exception e) {
            // Print the errors
            System.out.println("\n Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
