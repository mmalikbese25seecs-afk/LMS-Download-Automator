package com.downloadc.downloadc.api;

import com.downloadc.downloadc.model.DownloadRecord;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

// Service class to store and manage download history in a JSON file

@Service
public class DownloadHistoryService {

    // path where history is saved
    private static final Path HISTORY_FILE = Paths.get("downloads", "history.json");

    private final ObjectMapper objectMapper;

    // constructor setup object mapper to make json readable 
    public DownloadHistoryService() {
        this.objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT); 
    }

    // add a new record to history
    public synchronized void addRecord(DownloadRecord record) {
        try {
            List<DownloadRecord> existing = readAll();
            existing.add(record);
            writeAll(existing);
        } catch (IOException e) {
            // don't crash app if history fails
            System.out.println("Error saving history: " + e.getMessage());
        }
    }

    // get all records from file
    
    public synchronized List<DownloadRecord> readAll() {
   // Returns empty list if file not found
        if (!Files.exists(HISTORY_FILE)) {
            return new ArrayList<>();
        }

        try {
            return objectMapper.readValue(
                    HISTORY_FILE.toFile(),
                    new TypeReference<List<DownloadRecord>>() {}
            );
        } catch (IOException e) {
            System.out.println("Error " + e.getMessage());
            return new ArrayList<>();
        }
    }

    // delete all history (reset)
    public synchronized void clearAll() throws IOException {
        writeAll(new ArrayList<>());
        System.out.println("History cleared");
    }

    // Calculates stats 
    public DownloadStats getStats() {

        List<DownloadRecord> records = readAll();

        long totalBytes = records.stream()
                .mapToLong(DownloadRecord::getFileSizeBytes)
                .sum();

        long totalFiles = records.size();

        // Count unique courses
        long uniqueCourses = records.stream()
                .map(DownloadRecord::getCourseShortName)
                .distinct()
                .count();

        return new DownloadStats(totalFiles, totalBytes, uniqueCourses);
    }

    // write full list to file
    private void writeAll(List<DownloadRecord> records) throws IOException {

        // create folder if missing
        Files.createDirectories(HISTORY_FILE.getParent());

        objectMapper.writeValue(HISTORY_FILE.toFile(), records);
    }

    // simple record to send stats to frontend
    public record DownloadStats(
            long totalFiles,
            long totalBytes,
            long uniqueCourses
    ) {

        // convert size into KB/MB/GB
        public String totalSizeFormatted() {

            if (totalBytes < 1024)
                return totalBytes + " B";

            if (totalBytes < 1024 * 1024)
                return String.format("%.1f KB", totalBytes/ 1024.0);

            if (totalBytes < 1024L * 1024 * 1024)
                return String.format("%.1f MB", totalBytes / (1024.0 * 1024));

            return String.format("%.1f GB", totalBytes /(1024.0 * 1024 * 1024));
        }
    }
}
