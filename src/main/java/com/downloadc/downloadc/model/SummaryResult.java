package com.downloadc.downloadc.model;

// Stores the result after summarizing a PDF file
// It contains basic info offile name,pages, preview text and summary

public class SummaryResult {

    // File details
    private final String fileName;
    private final int pageCount;

    // Content data
    trivate final String extractedText;  // text preview
    private final String summary; 

}
