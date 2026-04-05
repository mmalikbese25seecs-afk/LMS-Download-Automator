package com.downloadc.downloadc.controller;

import com.downloadc.downloadc.api.CalendarService;
import com.downloadc.downloadc.api.MoodleApiClient;
import com.downloadc.downloadc.config.SessionManager;
import com.downloadc.downloadc.model.CalendarEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/calendar")
public class CalendarController {

    @Autowired
    private SessionManager sessionManager;

    @GetMapping("/events")
    public ResponseEntity<?> getEvents(
            @RequestParam(defaultValue = "180") int days) {

        if (!sessionManager.isLoggedIn()) {
            return ResponseEntity.status(401)
                    .body(Map.of("error", "Not logged in"));
        }

        try {
            MoodleApiClient client  = new MoodleApiClient(sessionManager.getActiveConfig());
            CalendarService service = new CalendarService(client);
            List<CalendarEvent> events = service.getUpcomingEvents(days);
            return ResponseEntity.ok(events);

        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}