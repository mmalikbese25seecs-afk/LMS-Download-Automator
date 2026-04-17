package com.downloadc.downloadc.controller;

import com.downloadc.downloadc.api.GeminiSummarizerService;
import com.downloadc.downloadc.api.PdfSummarizerService;
import com.downloadc.downloadc.config.SessionManager;
import com.downloadc.downloadc.model.SummaryResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

// Handle pdf summarization requests
@RestController
@RequestMapping("/api/summarize")
public class SummarizerController {

    @Autowired private SessionManager sessionManager;
    @Autowired private PdfSummarizerService localSummarizer;
    @Autowired private GeminiSummarizerService geminiSummarizer;

    // Check which summarizers are available
    @GetMapping("/status")
    public ResponseEntity<?> getStatus() {

        return ResponseEntity.ok(Map.of("localAvailable", true,"geminiAvailable", geminiSummarizer.isConfigured()));
    }

    // Upload pdf and summarize

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> summarize(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "engine", defaultValue = "local") String engine) {

        // check login
        if (!sessionManager.isLoggedIn())
            return ResponseEntity.status(401)
                    .body(Map.of("error", "Not logged in"));


        if (file.isEmpty())
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "No file uploaded"));

        String name = (file.getOriginalFilename() != null? file.getOriginalFilename() : "").toLowerCase();

        if (!name.endsWith(".pdf"))
            return ResponseEntity.badRequest().body(Map.of("error", "Only PDF files allowed."));

        if (file.getSize() > 20L * 1024 * 1024)
            return ResponseEntity.badRequest().body(Map.of("error", "File too large (max 20MB)."));

        try {

            // if user selects gemini
            if ("gemini".equalsIgnoreCase(engine)) {

                // check if api key exists
                if (!geminiSummarizer.isConfigured()) {
                    return ResponseEntity.badRequest().body(Map.of(
                            "error", "Gemini not configured. Add API key in properties."
                    ));
                }

                // Extract text locally
                SummaryResult local = localSummarizer.summarize(file);

                // send text to gemini for better summary
                String aiSummary = geminiSummarizer.summarize(local.getExtractedText() + "...",
                        file.getOriginalFilename()
                );

                // return result with AI summary
                return ResponseEntity.ok(new SummaryResult(
                        local.getFileName(),
                        local.getPageCount(),
                        local.getExtractedText(),
                        aiSummary
                ));

            } else {
                // default: local summarizer
                return ResponseEntity.ok(localSummarizer.summarize(file));
            }

        } catch (Exception e) {
            System.out.println("Error in summarizern : " + e.getMessage());

            return ResponseEntity.status(500)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
