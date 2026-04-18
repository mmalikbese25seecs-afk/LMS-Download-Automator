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

// Service to call Gemini api and summarize PDF text
@Service

public class GeminiSummarizerService {

    @Value("${gemini.api.key:}")
    private String apiKey;

    // Gemini API URL
    private static final String GEMINI_URL ="https://generativelanguage.googleapis.com/v1beta/models/" +
        "gemini-2.5-flash-lite:generateContent?key=";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    // Constructor
    public GeminiSummarizerService() {
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15))  // timeout set
                .build();

        this.objectMapper = new ObjectMapper();
    }

    // check if api key exists or not
    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    // Send text to Gemini and get summary
    public String summarize(String extractedText, String fileName) throws Exception {

        // if API key missing
        if (!isConfigured()) {
            throw new Exception(
                "Gemini API key not configured. Add gemini api key"
            );
        }

        // limit text size so API doesn't break
        String text= extractedText.length() > 12_000? extractedText.substring(0, 12_000) + "\n[shortened]"
                : extractedText;

        // JSON body for request

        ObjectNode body = objectMapper.createObjectNode();
        ArrayNode contents = body.putArray("contents");
        ObjectNode content = contents.addObject();
        content.putArray("parts").addObject().put("text", buildPrompt(text, fileName));

        // config settings for Gemini
        ObjectNode genCfg = body.putObject("generationConfig");

        // low temperature to make summary more factual

        genCfg.put("temperature", 0.3);
        genCfg.put("maxOutputTokens", 1024);

        // Building http Request

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GEMINI_URL + apiKey))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .timeout(Duration.ofSeconds(30))
                .build();

        System.out.println("Calling gemini for : " + fileName);

        // sending request
        HttpResponse<String> response = httpClient.send(
                request, HttpResponse.BodyHandlers.ofString()
        );

        System.out.println("Status : " + response.statusCode());

        // Error handling

        if (response.statusCode() == 429)

            throw new Exception("Rate limit hit, try later");
        if (response.statusCode() != 200)
            throw new Exception("Rrror from gemini : " + response.statusCode());

        return parseResponse(response.body());
    }

    // Prompt for Gemini

    private String buildPrompt(String text, String fileName) {
        return "Summarize the document '" + fileName + "' like this:\n\n" +
               "Key Topics\n" +
               "Core Concepts\n" +
               "Key Takeaways\n" +
               "Overview\n\n" +
               "Be simple and clear.\n\n" +
               "DOCUMENT:\n" + text;
    }

    // Reads response JSON and extract text

    private String parseResponse(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);

        // if API gives error
        if (root.has("error"))
            throw new Exception("gemini error: " + root.get("error").get("message").asText());

        try {
            return root.get("candidates").get(0)
                       .get("content").get("parts").get(0)
                       .get("text").asText();
        } catch (Exception e) {
            throw new Exception("failed to parse response");
        }
    }
}
