package com.downloadc.downloadc.model;

// course model, enriched with instructor, last access, new file count, and favorite flag
// extra fields default to safe values so the 4-arg constructor still works fine
public class Course {

    private final int    id;
    private final String fullName;
    private final String shortName;
    private final String summary;

    // populated by CourseService after the base course is fetched.
    private String  instructorName; // first teacher found in Moodle response
    private long    lastAccess;     // Unix epoch, when student last opened this course
    private int     newFilesCount;  // files on lms not yet downloaded locally
    private boolean favorite;       // pinned by user, persisted by FavoritesService

    // old constructor, keeps existing call sites compiling
    public Course(int id, String fullName, String shortName, String summary) {
        this.id             = id;
        this.fullName       = fullName;
        this.shortName      = shortName;
        this.summary        = summary;
        this.instructorName = "";
        this.lastAccess     = 0L;
        this.newFilesCount  = 0;
        this.favorite       = false;
    }

    public Course(int id, String fullName, String shortName, String summary,
                  String instructorName, long lastAccess, int newFilesCount, boolean favorite) {
        this.id             = id;
        this.fullName       = fullName;
        this.shortName      = shortName;
        this.summary        = summary;
        this.instructorName = instructorName;
        this.lastAccess     = lastAccess;
        this.newFilesCount  = newFilesCount;
        this.favorite       = favorite;
    }

    public int     getId()             { return id; }
    public String  getFullName()       { return fullName; }
    public String  getShortName()      { return shortName; }
    public String  getSummary()        { return summary; }
    public String  getInstructorName() { return instructorName; }
    public long    getLastAccess()     { return lastAccess; }
    public int     getNewFilesCount()  { return newFilesCount; }
    public boolean isFavorite()        { return favorite; }

    public void setInstructorName(String instructorName) { this.instructorName = instructorName; }
    public void setLastAccess(long lastAccess)           { this.lastAccess     = lastAccess; }
    public void setNewFilesCount(int count)              { this.newFilesCount  = count; }
    public void setFavorite(boolean favorite)            { this.favorite       = favorite; }

    @Override
    public String toString() {
        return String.format("Course[id=%d, %s, instructor=%s, newFiles=%d, fav=%b]",
                id, shortName, instructorName, newFilesCount, favorite);
    }
}