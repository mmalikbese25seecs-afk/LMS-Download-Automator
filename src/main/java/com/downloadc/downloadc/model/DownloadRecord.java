package com.downloadc.downloadc.model;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;


// Stores basic info about the downloaded file
public class DownloadRecord {

    // File details
    private final String fileName;
    private final String courseName;
    private final String courseShortName;
    private final long   fileSizeBytes;

    // Download info
    private final String downloadedAt;   // time when file was downloaded
    private final String localPath;      // where file is saved

    // Formatter to store time in readable format
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
                    .withZone(ZoneId.of("Asia/Karachi"));

    // Constructor called when creating a new download record
    // Automatically sets current time
    
    public DownloadRecord(String fileName, String courseName,
                          String courseShortName, long fileSizeBytes,
                          String localPath) {

        this.fileName = fileName;
        this.courseName = courseName;
        this.courseShortName = courseShortName;
        this.fileSizeBytes = fileSizeBytes;
        this.localPath = localPath;

        // Save current time
        this.downloadedAt = FORMATTER.format(Instant.now());
    }

    // Constructor used when reading data from JSON file
    // All values are passed manually
    
    public DownloadRecord(String fileName, String courseName,
                          String courseShortName, long fileSizeBytes,
                          String localPath, String downloadedAt) {

        this.fileName = fileName;
        this.courseName = courseName;
        this.courseShortName = courseShortName;
        this.fileSizeBytes = fileSizeBytes;
        this.localPath = localPath;
        this.downloadedAt = downloadedAt;
    }

    // GETTERs

    public String getFileName() {
        return fileName;
    }

    public String getCourseName() {
        return courseName;
    }

    public String getCourseShortName() {
        return courseShortName;
    }

    public long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public String getDownloadedAt() {
        return downloadedAt;
    }

    public String getLocalPath() {
        return localPath;
    }

    // Converts file size into readable format
    public String getFileSizeFormatted() {

        if (fileSizeBytes < 1024)
            return fileSizeBytes + " B";

        if (fileSizeBytes < 1024 * 1024)
            return String.format("%.1f KB", fileSizeBytes/ 1024.0);

        if (fileSizeBytes < 1024 * 1024 * 1024)
            return String.format("%.1f MB", fileSizeBytes /(1024.0 *1024));

        return String.format("%.1f GB", fileSizeBytes / (1024.0* 1024* 1024));
    }
}
