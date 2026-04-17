package com.downloadc.downloadc.model;

// Stores the result after summarizing a PDF file
// It contains basic info of file name,pages, preview text and summary

public class SummaryResult {

    // File details
    private final String fileName;
    private final int pageCount;

    // Content data
    private final String extractedText;  // text preview
    private final String summary;

    // Constructor
    public SummaryResult(String fileName, int pageCount,
                         String extractedText, String summary) {

        this.fileName = fileName;
        this.pageCount = pageCount;
        this.extractedText = extractedText;
        this.summary = summary;
    }

    // GETTERS

    public String getFileName() {
        return fileName;
    }

    public int getPageCount() {
        return pageCount;
    }

    public String getExtractedText() {
        return extractedText;
    }

    public String getSummary() {
        return summary;
    }

}
