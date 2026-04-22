package com.downloadc.downloadc.model;

// a single downloadable file from Moodle
// moodleTimestamp = "timemodified" from lms, used by smart sync to skip unchanged files.
public class CourseFile {

    private final String fileName;
    private final String fileUrl;         // needs auth token appended before downloading
    private final long   fileSize;
    private final String fileType;
    private final String courseName;
    private final long   moodleTimestamp; // Unix epoch seconds, 0 if unknown

    public CourseFile(String fileName, String fileUrl,
                      long fileSize, String fileType,
                      String courseName, long moodleTimestamp) {
        this.fileName        = fileName;
        this.fileUrl         = fileUrl;
        this.fileSize        = fileSize;
        this.fileType        = fileType;
        this.courseName      = courseName;
        this.moodleTimestamp = moodleTimestamp;
    }

    // old constructor without timestamp, defaults to 0
    public CourseFile(String fileName, String fileUrl,
                      long fileSize, String fileType, String courseName) {
        this(fileName, fileUrl, fileSize, fileType, courseName, 0L);
    }

    public String getFileName()        { return fileName; }
    public String getFileUrl()         { return fileUrl; }
    public long   getFileSize()        { return fileSize; }
    public String getFileType()        { return fileType; }
    public String getCourseName()      { return courseName; }
    public long   getMoodleTimestamp() { return moodleTimestamp; }

    @Override
    public String toString() {
        return String.format("CourseFile[%s, %.1f KB, %s, ts=%d]",
                fileName, fileSize / 1024.0, courseName, moodleTimestamp);
    }
}