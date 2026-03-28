package com.downloadc.downloadc.api;

import com.downloadc.downloadc.model.MoodleConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.SecureRandom;

//AuthService xchanges credentials for a Token.
//Send username + password to the token endpoint

public class AuthService {

     // HttpClient send the login request to Moodle's token endpoint
     // ObjectMapper converts JSon string into a Java object
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    // Constructor
    public AuthService() {
        try {
            // Trust all certificates to bypaas LMS SSL issues
            TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() { return null; }
                        public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) { }
                        public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) { }
                    }
            };
// Configure SSLContext to use the custom TrustManager
            SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new SecureRandom());

            // Attach SSL config so requests do not fail
            this.httpClient = HttpClient.newBuilder()
                    .sslContext(sslContext)
                    .build();

            this.objectMapper = new ObjectMapper();

        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize AuthService", e);
        }
    }

// Gets token so password is not sent in every request

    public String getToken(MoodleConfig config) throws Exception {

        // Construct the token attach username and password as URL parameters
        String url = config.getBaseUrl()
                + "/login/token.php"
                + "?username=" + config.getUsername()
                + "&password=" + config.getPassword()
                + "&service=moodle_mobile_app";

        // Create a  GET request
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

// Execute request and capture response body as a String
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode jsonNode = objectMapper.readTree(response.body());
//Moodle returns an "error" key if authentication fails
        if (jsonNode.has("error")) {
            throw new Exception("Login failed: " + jsonNode.get("error").asText());
        }

        return jsonNode.get("token").asText();
    }
}