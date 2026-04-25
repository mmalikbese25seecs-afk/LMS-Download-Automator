
package com.downloadc.downloadc.api;


import java.io.InputStream;
import com.downloadc.downloadc.model.SummaryResult;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;


// Service to extract text from pdf and generate summary
@Service
public class PdfSummarizerService {

    // Limited text size to avoid issues with large pdfs
    private static final int MAX_CHARS = 20000;

    //Sentences we want in summary
    private static final int SUMMARY_SENTENCES = 15;

    // Main function to process pdf file
    public SummaryResult summarize(MultipartFile file) throws Exception {

        String fileName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "document.pdf";

        System.out.println("Processing file : " + fileName);

        String fullText ;
        int pageCount;

        // xtract text using pdfbox
        try (InputStream is = file.getInputStream();
             PDDocument doc = PDDocument.load(is)) {

            pageCount = doc.getNumberOfPages();

            PDFTextStripper stripper = new PDFTextStripper();

// Keep correct reading order
            stripper.setSortByPosition(true);

            fullText = stripper.getText(doc);
        }

        // limit text length if too big
        String processText = fullText.length() > MAX_CHARS
                ? fullText.substring(0, MAX_CHARS)
                : fullText;

        // Generate summary
        String summary = extractiveSummary(processText, SUMMARY_SENTENCES);

        // preview
        String preview = processText.length() > 500
                ? processText.substring(0, 500) + "..."
                : processText;

        return new SummaryResult(fileName, pageCount, preview, summary);
    }


    // simple summarization
    private String extractiveSummary(String text, int maxSentences) {

        String[] sentences = text.split("(?<=[.!?])\\s+");

        if (sentences.length== 0)
            return "Could not generate summary";

        if (sentences.length <= maxSentences)
            return String.join(" ", sentences);

        // some keywords
        String[] keywords = {"therefore", "however", "conclusion", "result", "important",
                "define", "concept", "method", "approach", "algorithm",
                "example", "objective", "purpose", "summary", "analysis",
                "function", "class", "object", "interface", "input", "output"
        };

        double[] scores = new double[sentences.length];

        for (int i = 0; i < sentences.length; i++) {

            String s = sentences[i];
            String lower = s.toLowerCase();

            // bonus for first/last sentence
            if (i == 0 || i == sentences.length - 1)
                scores[i] += 2;

            // keyword scoring
            for (String kw : keywords) {
                if (lower.contains(kw))
                    scores[i] += 3;
            }

            // check sentence length
            String[] words = s.trim().split("\\s+");

            if (words.length < 6)
                scores[i] -= 5;

            if (words.length > 10)
                scores[i] += Math.min(words.length * 0.1, 2);
        }

        // sort sentences by score
        Integer[] indices = new Integer[sentences.length];
        for (int i = 0; i < indices.length; i++)
            indices[i] = i;

        java.util.Arrays.sort(indices,
                (a, b) -> Double.compare(scores[b], scores[a]));

        // pick top sentences
        java.util.List<Integer> top = new java.util.ArrayList<>();

        for (int i = 0; i < Math.min(maxSentences, indices.length); i++) {
            if (scores[indices[i]] > -3)
                top.add(indices[i]);
        }

        // Keep original order
        java.util.Collections.sort(top);

        StringBuilder sb = new StringBuilder();

        for (int idx : top) {
            sb.append(sentences[idx].trim()).append(" ");
        }

        return sb.toString().trim();
    }

    // Clean extracted text
    private String cleanText(String raw) {

        return raw
                .replaceAll("\\r\\n|\\r", "\n")
                .replaceAll("\\n{3,}", "\n\n")
                .replaceAll("[ \\t]{2,}", " ")
                .replaceAll("(?m)^\\s*\\d+\\s*$", "")
                .trim();
    }
}
