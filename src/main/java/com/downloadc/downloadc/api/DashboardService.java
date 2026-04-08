package com.downloadc.downloadc.api;

import com.downloadc.downloadc.model.CalendarEvent;
import com.downloadc.downloadc.model.Course;
import com.downloadc.downloadc.model.MoodleConfig;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

// Aggregates user info, courses, and calendar events into one dashboard response
public class DashboardService {

    private final MoodleApiClient apiClient;
    private final MoodleConfig    config;

    // Built per-request — token is only available at request time, not Spring startup
    public DashboardService(MoodleApiClient apiClient, MoodleConfig config) {
        this.apiClient = apiClient;
        this.config    = config;
    }

    // Makes 3 Moodle calls and returns combined summary for the dashboard
    public DashboardSummary getSummary() throws Exception {

        // Call 1: Get user's full name and site name
        JsonNode siteInfo = apiClient.callFunction("core_webservice_get_site_info");
        String fullName   = siteInfo.has("fullname")
                ? siteInfo.get("fullname").asText() : "Unknown";
        String siteName   = siteInfo.has("sitename")
                ? siteInfo.get("sitename").asText() : "NUST LMS";

        // Call 2: Get enrolled courses
        CourseService courseService = new CourseService(apiClient, config);
        List<Course> courses        = courseService.getEnrolledCourses();

        // Call 3: Get upcoming events for next 14 days
        CalendarService calendarService = new CalendarService(apiClient, config);
        List<CalendarEvent> allEvents   = calendarService.getUpcomingEvents(14);

        // Take only the 5 nearest deadlines for dashboard preview
        List<CalendarEvent> upcomingFive = allEvents.stream()
                .filter(e -> e.getTimeUntilDue() > 0)
                .sorted((a, b) -> Long.compare(a.getTimeUntilDue(), b.getTimeUntilDue()))
                .limit(5)
                .toList();