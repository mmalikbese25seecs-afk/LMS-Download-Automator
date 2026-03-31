package com.downloadc.downloadc.model;

// Represents a single downloadable file from moodle
public class CourseFile {
    private final String fileName;
    private final String fileUrl;    // Token must be appended before downloading
    private final long fileSize;
    private final String fileType;
    private final String courseName; // Course Name for setting folder name

    // Constructor
    public CourseFile(String fileName, String fileUrl,
                      long fileSize, String fileType, String courseName) {
        this.fileName   = fileName;
        this.fileUrl    = fileUrl;
        this.fileSize   = fileSize;
        this.fileType   = fileType;
        this.courseName = courseName;
    }
    @Override
    public String toString() {
        return String.format("CourseFile[%s , %.1f KB , %s",
                fileName, fileSize / 1024.0, courseName);
    }
}
