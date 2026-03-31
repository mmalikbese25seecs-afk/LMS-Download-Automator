package com.downloadc.downloadc.api;

import com.downloadc.downloadc.model.Course;
import com.downloadc.downloadc.model.CourseFile;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

// Extracts downloadable file metadata.
// Sets nested Json sections and modules into a flat list of CourseFile objects.
public class FileService {

    private final MoodleApiClient apiClient;

    public FileService(MoodleApiClient apiClient) {
        this.apiClient = apiClient;
    }

    // Retrieves all file resources for a specific course.
    // Handles the nesting of Json
    public List<CourseFile> getFilesForCourse(Course course) {
        List<CourseFile> files = new ArrayList<>();
        System.out.println("Course: " + course.getShortName());

        try {
            // API call toretrieve the full course content tree
            JsonNode sections = apiClient.callFunction(
                    "core_course_get_contents",
                    "courseid=" + course.getId()
            );

            //Check for valid array response
            if (sections == null || !sections.isArray()) {
                System.err.println("Error for " + course.getShortName());
                return files;
            }

            //Iterate through Sections
            for (JsonNode section : sections) {
                JsonNode modules = section.get("modules");
                if (modules == null || !modules.isArray()) continue;

                // iIterate through Modules
                for (JsonNode module : modules) {
                    String modName = module.path("modname").asText("");

                    // Filter for resource types only
                    if (!modName.equals("resource")) continue;

                    JsonNode contents = module.get("contents");
                    if (contents == null || !contents.isArray()) continue;

                    // Extract individual file assets from the module
                    for (JsonNode content : contents) {
                        String contentType = content.path("type").asText("");

                        // Ensure the content is an actual file and has a valid URL
                        if (!contentType.equals("file") || !content.has("fileurl")) continue;

                        String fileName = content.path("filename").asText("unknown_file");
                        String fileUrl  = content.path("fileurl").asText();
                        long fileSize   = content.path("filesize").asLong(0L);
                        String mimeType = content.path("mimetype").asText("application/octet-stream");

                        // Encapsulate metadata in a CourseFile model
                        files.add(new CourseFile(
                                fileName,
                                fileUrl,
                                fileSize,
                                mimeType,
                                course.getShortName()
                        ));
                    }
                }
            }

            System.out.println("Identified " + files.size() + " files in " + course.getShortName());

        } catch (Exception e) {
            System.err.println("Failed to process course " + course.getShortName() + ": " + e.getMessage());
        }

        return files;
    }
}
