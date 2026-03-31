package com.downloadc.downloadc.downloader;

import com.downloadc.downloadc.model.CourseFile;
import com.downloadc.downloadc.model.MoodleConfig;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;

//Saves a course file from Moodle to the local filesystem
public class FileDownloader {
    private final HttpClient httpClient;
    private final MoodleConfig config;
    private static final String DOWNLOAD_ROOT = "downloads";

    public FileDownloader(MoodleConfig config) {
        this.config = config;
        try {
            // Configure SSL context to bypass certificate validation for specific LMS environments
            TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() { return null; }
                        public void checkClientTrusted(java.security.cert.X509Certificate[] c, String a) {}
                        public void checkServerTrusted(java.security.cert.X509Certificate[] c, String a) {}
                    }
            };
            SSLContext ssl = SSLContext.getInstance("SSL");
            ssl.init(null, trustAllCerts, new SecureRandom());
            this.httpClient = HttpClient.newBuilder().sslContext(ssl).build();
        } catch (Exception e) {
            throw new RuntimeException("FileDownloader SSL init failed: " + e.getMessage(), e);
        }
    }

    // Downloads a file to a structured local directory.
    public boolean download(CourseFile courseFile) throws Exception {

        //senitizeName() removes characters that are illegal in file name
        String safeCourse = sanitizeName(courseFile.getCourseName());
        String safeFile   = sanitizeName(courseFile.getFileName());

        Path courseFolder = Paths.get(DOWNLOAD_ROOT, safeCourse);
        Path destination  = courseFolder.resolve(safeFile);

        // Skip download if the file already exists
        if (Files.exists(destination)) {
            System.out.println("Skipped: " + safeFile);
            return false;
        }

        //Create the course folder
        Files.createDirectories(courseFolder);

        // Append authentication token to the Moodle file URL
        String fileUrl = courseFile.getFileUrl();
        String authenticatedUrl = fileUrl.contains("?")
                ? fileUrl + "&token=" + config.getToken()
                : fileUrl + "?token="  + config.getToken();

        System.out.printf("Downloading: %-40s → %s%n",
                safeFile, courseFolder);

        // Build and send the download request.
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(authenticatedUrl))
                .GET()
                .build();

        HttpResponse<InputStream> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofInputStream()
        );

        // Checks for error
        if (response.statusCode() != 200) {
            throw new Exception("Download failed for " + safeFile
                    + "HTTP " + response.statusCode());
        }


        // 9.5 KB of memory chunks at a time is faster than moving it once
        try (InputStream in       = response.body();
             OutputStream out     = Files.newOutputStream(destination)) {

            byte[] buffer         = new byte[9500];
            int    bytesRead;

            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }

        System.out.println("[FileDownloader] Saved: " + destination);
        return true;
    }

    // Removes illegal characters from strings for safe filesystem usage
    private String sanitizeName(String name) {
        return name.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
    }
}