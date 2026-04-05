package com.downloadc.downloadc.api;

import com.downloadc.downloadc.model.CalendarEvent;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class CalendarService {

    private final MoodleApiClient apiClient;

    private static final DateTimeFormatter ISO_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
                    .withZone(ZoneOffset.UTC);

    private static final long DAY = 86400;

    public CalendarService(MoodleApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public List<CalendarEvent> getUpcomingEvents(int days) throws Exception {

        long now    = Instant.now().getEpochSecond();
        long future = now + (days * DAY);

        JsonNode response = apiClient.callFunction(
                "core_calendar_get_action_events_by_timesort",
                "timesortfrom=" + now +
                        "&timesortto="  + future +
                        "&limitnum=300"
        );

        List<CalendarEvent> events = new ArrayList<>();

        if (response == null || !response.has("events")) {
            return events;
        }

        for (JsonNode e : response.get("events")) {

            String id        = e.has("id")        ? e.get("id").asText()        : null;
            String title     = e.has("name")       ? e.get("name").asText()      : null;
            long   timeStart = e.has("timestart")  ? e.get("timestart").asLong() : now;

            String type = e.has("modulename")
                    ? e.get("modulename").asText().toLowerCase()
                    : "event";

            String course = e.has("course") && e.get("course").has("fullname")
                    ? e.get("course").get("fullname").asText()
                    : "";

            String desc = e.has("action") && e.get("action").has("name")
                    ? e.get("action").get("name").asText()
                    : type;

            String iso = ISO_FORMATTER.format(Instant.ofEpochSecond(timeStart));
            long   due = timeStart - now;
            String color = resolveColor(type, due);

            events.add(new CalendarEvent(
                    id, title, iso, color, course, type, desc, due
            ));
        }
        return events;
    }

    private String resolveColor(String type, long due) {
        if ("quiz".equals(type))   return "#9b59b6";
        if (due <= 3 * DAY)        return "#e74c3c";
        if (due <= 7 * DAY)        return "#e67e22";
        if ("assign".equals(type)) return "#3498db";
        return "#27ae60";
    }
}