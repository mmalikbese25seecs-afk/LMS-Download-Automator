package com.downloadc.downloadc.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
public class GeminiSummarizerService implements Summarizable {

    @Value("${gemini.api.key:}")
    private String apiKey;

    private static final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/" +
                    "gemini-2.0-flash:generateContent?key=";

    // Retry settings
    private static final int    MAX_RETRIES   = 3;
    private static final long[] RETRY_DELAYS  = { 5_000, 15_000, 30_000 }; // ms between retries

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public GeminiSummarizerService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    // Original method for backward compatibility
    public String summarize(String extractedText, String fileName) throws Exception {

        if (!isConfigured()) {
            throw new Exception("Gemini API key not configured. Add gemini.api.key to application.properties");
        }

        // Limit text size
        String text = extractedText.length() > 12_000
                ? extractedText.substring(0, 12_000) + "\n[text shortened]"
                : extractedText;

        String promptText = buildDefaultPrompt(text, fileName);
        return callGeminiAPI(promptText);
    }

    // New method that accepts a full prompt
    public String summarizeWithPrompt(String fullPrompt) throws Exception {

        if (!isConfigured()) {
            throw new Exception("Gemini API key not configured. Add gemini.api.key to application.properties");
        }

        // Limit prompt size
        String prompt = fullPrompt.length() > 20_000
                ? fullPrompt.substring(0, 20_000) + "\n[text shortened]"
                : fullPrompt;

        return callGeminiAPI(prompt);
    }

    // Internal method to call Gemini API
    private String callGeminiAPI(String promptText) throws Exception {

        ObjectNode body = objectMapper.createObjectNode();
        ArrayNode contents = body.putArray("contents");
        ObjectNode content = contents.addObject();
        ObjectNode part = content.putArray("parts").addObject();
        part.put("text", promptText);

        ObjectNode genCfg = body.putObject("generationConfig");
        genCfg.put("temperature", 0.3);
        genCfg.put("maxOutputTokens", 1024);

        String requestBody = objectMapper.writeValueAsString(body);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GEMINI_URL + apiKey))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .timeout(Duration.ofSeconds(60))
                .build();

        System.out.println("[GeminiSummarizerService] Calling Gemini API");

        // Retry loop — handles 429 rate limit automatically
        HttpResponse<String> response = null;
        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {

            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("[GeminiSummarizerService] Response status (attempt " + (attempt + 1) + "): " + response.statusCode());

            if (response.statusCode() == 429) {
                if (attempt < MAX_RETRIES) {
                    long delay = RETRY_DELAYS[attempt];
                    System.out.println("[GeminiSummarizerService] Rate limit hit. Retrying in " + (delay / 1000) + "s...");
                    Thread.sleep(delay);
                    continue; // retry
                } else {
                    throw new Exception("Rate limit hit after " + MAX_RETRIES + " retries. Please wait a minute and try again.");
                }
            }

            break; // success or non-429 error — stop retrying
        }

        if (response.statusCode() == 400) {
            JsonNode errRoot = objectMapper.readTree(response.body());
            String errMsg = errRoot.path("error").path("message").asText("Bad request");
            throw new Exception("Gemini rejected the request: " + errMsg);
        }

        if (response.statusCode() != 200) {
            System.err.println("[GeminiSummarizerService] Error response: " + response.body());
            throw new Exception("Error from Gemini: " + response.statusCode());
        }

        return parseResponse(response.body());
    }

    private String buildDefaultPrompt(String text, String fileName) {
        String safeName = fileName != null
                ? fileName.replaceAll("[\"'\\\\]", "")
                : "document";

        return "You are a helpful study assistant. Summarize the document '" + safeName + "' using these sections:\n\n" +
                "**Key Topics** — What main subjects does this cover?\n" +
                "**Core Concepts** — What are the key ideas explained?\n" +
                "**Key Takeaways** — What should the reader remember?\n" +
                "**Overview** — A brief 2-3 sentence summary.\n\n" +
                "Be concise, clear, and student-friendly.\n\n" +
                "DOCUMENT TEXT:\n" + text;
    }

    private String parseResponse(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);

        if (root.has("error")) {
            throw new Exception("Gemini error: " + root.get("error").get("message").asText());
        }

        try {
            String summary = root.get("candidates").get(0)
                    .get("content").get("parts").get(0)
                    .get("text").asText();
            System.out.println("[GeminiSummarizerService] Successfully parsed response");
            return summary;
        } catch (Exception e) {
            System.err.println("[GeminiSummarizerService] Failed to parse response: " + e.getMessage());
            throw new Exception("Failed to parse Gemini response. Raw: " +
                    responseBody.substring(0, Math.min(200, responseBody.length())));
        }
    }
}
