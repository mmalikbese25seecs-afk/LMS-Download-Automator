package com.downloadc.downloadc.api;

import com.downloadc.downloadc.model.Course;
import com.downloadc.downloadc.model.MoodleConfig;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;


 // CourseService fetches the user's enrolled courses from Moodle.
 // Return a list
public class CourseService {

    // Fields
    private final MoodleApiClient apiClient;

    // Gets the user iD to pass as a parameter
    private final MoodleConfig config;


    // Constuctor
    public CourseService(MoodleApiClient apiClient, MoodleConfig config) {
        this.apiClient = apiClient;
        this.config = config;
    }

   //Methods
    public List<Course> getEnrolledCourses() {
        List<Course> courses = new ArrayList<>();
        System.out.println("Getting enrolled courses...");
                       // Executes APi call
            String extraParams = "userid=" + config.getUserId();
            JsonNode response = apiClient.callFunction("core_enrol_get_users_courses", extraParams);

            // Validate that the response is a JSON array
            if (response == null || !response.isArray()) {
                System.err.println("Error(Coursesrvice): Invalid API response format.");
                return courses;
            }

            // Map JSON nodes to Course objects
            for (JsonNode node : response) {
                int id = node.get("id").asInt();
                String fullName = node.get("fullname").asText();
                String shortName = node.get("shortname").asText();

                // For course summary
                String summary = node.has("summary") && !node.get("summary").isNull()
                        ? node.get("summary").asText()
                        : "";

                courses.add(new Course(id, fullName, shortName, summary));
            }

            System.out.println("Success " + courses.size() + " courses.");
      return courses;
}
