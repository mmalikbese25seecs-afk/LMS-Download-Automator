package com.downloadc.downloadc.downloader;

import com.downloadc.downloadc.api.DownloadHistoryService;
import com.downloadc.downloadc.model.CourseFile;
import com.downloadc.downloadc.model.DownloadRecord;
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

// class to download files and save them locally
public class FileDownloader {

    private final HttpClient httpClient;
    private final MoodleConfig config;
    private final DownloadHistoryService historyService;

    private static final String DOWNLOAD_ROOT = "downloads";

    // constructor
    public FileDownloader(MoodleConfig config, DownloadHistoryService historyService) {
        this.config = config;
        this.historyService = historyService;

        try {
            // trust all SSL 
            TrustManager[] trustAll = new TrustManager[]{
                new X509TrustManager() {
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() { return null; }
                    public void checkClientTrusted(java.security.cert.X509Certificate[] c, String a) {}
                    public void checkServerTrusted(java.security.cert.X509Certificate[] c, String a) {}
                }
            };

            SSLContext ssl = SSLContext.getInstance("SSL");
            ssl.init(null, trustAll, new SecureRandom());

            this.httpClient = HttpClient.newBuilder().sslContext(ssl)
                    .build();

        } catch (Exception e) {
            throw new RuntimeException("SSL setup failed : " + e.getMessage(), e);
        }
    }

    // download a file
    // return true if downloaded, false if already exists
    public boolean download(CourseFile courseFile) throws Exception {

        String safeCourse = sanitize(courseFile.getCourseName());
        String safeFile = sanitize(courseFile.getFileName());

        Path courseFolder = Paths.get(DOWNLOAD_ROOT, safeCourse);
        Path destination = courseFolder.resolve(safeFile);

        // skip if already exists
        if (Files.exists(destination)) {
            System.out.println("Skipped: " + safeFile);
            return false;
        }

        Files.createDirectories(courseFolder);

        // Add token to url
        String url = courseFile.getFileUrl();

        String authenticatedUrl = url.contains("?")? url + "&token=" + config.getToken()
                : url + "?token=" + config.getToken();

        System.out.println("Downloading : " + safeFile);

        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(authenticatedUrl))
                .GET()
                .build();

        HttpResponse<InputStream> response = httpClient.send(
                request, HttpResponse.BodyHandlers.ofInputStream()
        );

        if (response.statusCode() != 200) {
            throw new Exception("HTTP " + response.statusCode());
        }

        long bytesWritten = 0;

        // write file to disk
        try (InputStream in = response.body();
             OutputStream out = Files.newOutputStream(destination)) {

            byte[] buffer = new byte[8192];
            int read;

            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
                bytesWritten += read;
            }
        }

        System.out.println("Saved: " + destination + " (" + bytesWritten + " bytes)");

        // Save record in history
        DownloadRecord record = new DownloadRecord(
                safeFile,
                courseFile.getCourseName(),
                safeCourse,
                bytesWritten,
                destination.toString()
        );

        historyService.addRecord(record);

        return true;
    }

    // Remove invalid characters from file/folder name
    private String sanitize(String name) {
        return name.replaceAll("[\/:*?\"<>|]\\\", "_").trim();
    }
}
