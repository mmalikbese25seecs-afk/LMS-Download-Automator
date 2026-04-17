package com.downloadc.downloadc.api;

import com.downloadc.downloadc.model.Announcement;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

// Fetches instructor announcements from Moodle's notification API and maps them to Announcement objects.
public class AnnouncementService {

    private final MoodleApiClient apiClient;

    // PKT timezone so timestamps display correctly for NUST students.
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")
                    .withZone(ZoneId.of("Asia/Karachi"));

    public AnnouncementService(MoodleApiClient apiClient) {
        this.apiClient = apiClient;
    }

    // Returns up to `limit` unread notifications for the given user, sorted newest first.
    public List<Announcement> getRecentAnnouncements(int userId, int limit) throws Exception {

        System.out.println("[AnnouncementService] Fetching announcements for userId=" + userId);

        // Notifications-only (excludes private chats); limit capped at 50 per NUST LMS.
        JsonNode response = apiClient.callFunction(
                "core_message_get_messages",
                "useridto=" + userId
                        + "&type=notifications"
                        + "&read=0"
                        + "&limitfrom=0"
                        + "&limitnum=" + Math.min(limit, 50)
        );

        List<Announcement> announcements = new ArrayList<>();

        // Guard against a null or malformed response before iterating.
        if (response == null || !response.has("messages")) {
            System.out.println("[AnnouncementService] No messages key in response");
            return announcements;
        }

        JsonNode messages = response.get("messages");
        System.out.println("[AnnouncementService] Received " + messages.size() + " messages");

        // Reverse-sort so the newest announcement appears at the top of the list.
        announcements.sort((a, b) -> Long.compare(b.getTimeSent(), a.getTimeSent()));

        System.out.println("[AnnouncementService] Returning "
                + announcements.size() + " announcements");

        return announcements;
    }

    // Maps common subject keywords to emojis so the notification list is scannable at a glance.
    private String resolveIcon(String subject) {
        String lower = subject.toLowerCase();
        if (lower.contains("quiz") || lower.contains("test"))       return "📋";
        if (lower.contains("assignment") || lower.contains("task")) return "📝";
        if (lower.contains("exam") || lower.contains("final"))      return "📚";
        if (lower.contains("lab"))                                   return "🔬";
        if (lower.contains("cancel") || lower.contains("holiday"))  return "🚫";
        if (lower.contains("result") || lower.contains("grade"))    return "🎯";
        if (lower.contains("deadline") || lower.contains("due"))    return "⏰";
        return "📢";
    }
}