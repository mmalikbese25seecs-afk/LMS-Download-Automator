package com.downloadc.downloadc.model;

// This class serves as a data container for course metadata.
public class Course {

    // Fieldz
    private final int id;
    private final String fullName;
    private final String shortName;
    private final String summary;

    // Constructor
    public Course(int id, String fullName, String shortName, String summary) {
        this.id = id;
        this.fullName = fullName;
        this.shortName = shortName;
        this.summary = summary;
    }

    // Getters
    public int getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public String getShortName() {
        return shortName;
    }

    // Returns the course summary
  public String getSummary() {
        return summary;
    }
    @Override
    public String toString() {
        return String.format("Course id=%d , %s , %s", id, shortName, fullName);
    }
}
