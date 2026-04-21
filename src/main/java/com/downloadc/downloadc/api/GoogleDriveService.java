package com.downloadc.downloadc.api;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

// handles everything Google Drive related: uploading, listing, auth
// setup: get credentials.json from Google Cloud console and drop it in resources/
@Service
public class GoogleDriveService {

    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final List<String> SCOPES = Collections.singletonList(DriveScopes.DRIVE_FILE);
    private static final String APP_NAME = "LMS Download Automator";

    // all lms files go inside this folder in the user's drive
    private static final String ROOT_FOLDER_NAME = "LMS Downloads";

    @Value("${google.drive.credentials.path:src/main/resources/credentials.json}")
    private String credentialsPath;

    @Value("${google.drive.tokens.dir:tokens}")
    private String tokensDir;

    // cache course name: folder id, so we don't keep making duplicate folders
    private final Map<String, String> folderIdCache = new HashMap<>();

    // root "LMS Downloads" folder id, null until first use
    private String rootFolderId = null;

    // returns true if credentials.json exists, used to show/hide drive button on frontend
    public boolean isConfigured() {
        return new java.io.File(credentialsPath).exists();
    }

    // returns true if user has already authorized (token saved in tokens/ folder)
    public boolean isAuthorized() {
        if (!isConfigured()) return false;
        java.io.File tokenDir = new java.io.File(tokensDir);
        return tokenDir.exists() && tokenDir.listFiles() != null
                && Objects.requireNonNull(tokenDir.listFiles()).length > 0;
    }

    // triggers oauth flow, opens browser on first run, silent after that
    public String authorize() throws Exception {
        Drive drive = buildDriveService();
        drive.about().get().setFields("user").execute(); // quick test call to verify token works
        return "Google Drive connected successfully.";
    }

    // deletes saved tokens so user can re-auth with a different account
    public void disconnect() {
        java.io.File tokenDir = new java.io.File(tokensDir);
        if (tokenDir.exists()) {
            for (java.io.File f : Objects.requireNonNull(tokenDir.listFiles())) {
                f.delete();
            }
        }
        rootFolderId = null;
        folderIdCache.clear();
        System.out.println("[GoogleDriveService] Disconnected. Tokens cleared.");
    }

    // uploads a single file into LMS Downloads/{courseName}/ in drive
    public String uploadFile(String localFilePath, String courseName) throws Exception {

        Drive drive = buildDriveService();

        java.io.File localFile = new java.io.File(localFilePath);
        if (!localFile.exists()) {
            throw new IOException("Local file not found: " + localFilePath);
        }

        String rootId = getOrCreateRootFolder(drive);
        String courseFolderId = getOrCreateCourseFolder(drive, rootId, courseName);

        String mimeType = Files.probeContentType(localFile.toPath());
        if (mimeType == null) mimeType = "application/octet-stream";

        File fileMeta = new File();
        fileMeta.setName(localFile.getName());
        fileMeta.setParents(Collections.singletonList(courseFolderId));

        FileContent content = new FileContent(mimeType, localFile);
        File uploaded = drive.files().create(fileMeta, content)
                .setFields("id, name, webViewLink")
                .execute();

        System.out.println("[GoogleDriveService] Uploaded: " + uploaded.getName()
                + " → " + uploaded.getWebViewLink());

        return uploaded.getId();
    }

    // uploads all files from downloads/{courseName}/ to drive, skips duplicates
    public UploadResult uploadCourse(String courseName) throws Exception {

        Drive drive = buildDriveService();
        String rootId = getOrCreateRootFolder(drive);
        String courseFolderId = getOrCreateCourseFolder(drive, rootId, courseName);

        // grab existing file names in drive so we can skip dupes
        Set<String> existingNames = listFileNamesInFolder(drive, courseFolderId);

        Path courseDir = Paths.get("downloads", sanitize(courseName));
        if (!Files.exists(courseDir)) {
            throw new IOException("No local downloads found for course: " + courseName);
        }

        int uploaded = 0, skipped = 0, failed = 0;

        try (var stream = Files.list(courseDir)) {
            for (Path filePath : stream.toList()) {

                if (!Files.isRegularFile(filePath)) continue;

                String fileName = filePath.getFileName().toString();

                if (existingNames.contains(fileName)) {
                    System.out.println("[GoogleDriveService] Skipped (already in Drive): " + fileName);
                    skipped++;
                    continue;
                }

                try {
                    String mimeType = Files.probeContentType(filePath);
                    if (mimeType == null) mimeType = "application/octet-stream";

                    File fileMeta = new File();
                    fileMeta.setName(fileName);
                    fileMeta.setParents(Collections.singletonList(courseFolderId));

                    FileContent content = new FileContent(mimeType, filePath.toFile());
                    drive.files().create(fileMeta, content).setFields("id").execute();

                    System.out.println("[GoogleDriveService] Uploaded: " + fileName);
                    uploaded++;

                } catch (Exception e) {
                    System.err.println("[GoogleDriveService] Failed: " + fileName + " — " + e.getMessage());
                    failed++;
                }
            }
        }

        return new UploadResult(courseName, uploaded, skipped, failed);
    }

    // lists all files inside LMS Downloads/{courseName}/ in drive
    public List<DriveFileInfo> listCourseFiles(String courseName) throws Exception {

        Drive drive = buildDriveService();
        String rootId = getOrCreateRootFolder(drive);
        String courseFolderId = getOrCreateCourseFolder(drive, rootId, courseName);

        FileList result = drive.files().list()
                .setQ("'" + courseFolderId + "' in parents and trashed = false")
                .setFields("files(id, name, size, webViewLink, mimeType)")
                .execute();

        List<DriveFileInfo> files = new ArrayList<>();
        for (File f : result.getFiles()) {
            files.add(new DriveFileInfo(
                    f.getId(), f.getName(),
                    f.getSize() != null ? f.getSize() : 0L,
                    f.getWebViewLink(), f.getMimeType()
            ));
        }
        return files;
    }

    private Drive buildDriveService() throws Exception {
        final NetHttpTransport transport = GoogleNetHttpTransport.newTrustedTransport();
        Credential credential = getCredentials(transport);
        return new Drive.Builder(transport, JSON_FACTORY, credential)
                .setApplicationName(APP_NAME)
                .build();
    }

    private Credential getCredentials(NetHttpTransport transport) throws Exception {

        java.io.File credFile = new java.io.File(credentialsPath);
        if (!credFile.exists()) {
            throw new FileNotFoundException(
                    "credentials.json not found at: " + credentialsPath
                            + "\nDownload it from Google Cloud Console → APIs & Services → Credentials."
            );
        }

        GoogleClientSecrets secrets = GoogleClientSecrets.load(JSON_FACTORY, new FileReader(credFile));

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                transport, JSON_FACTORY, secrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(tokensDir)))
                .setAccessType("offline")
                .build();

        // localhost:8888 catches the oauth redirect
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();

        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }

    // finds or creates the "LMS Downloads" root folder in drive
    private String getOrCreateRootFolder(Drive drive) throws Exception {
        if (rootFolderId != null) return rootFolderId;

        FileList existing = drive.files().list()
                .setQ("name = '" + ROOT_FOLDER_NAME + "' "
                        + "and mimeType = 'application/vnd.google-apps.folder' "
                        + "and trashed = false")
                .setFields("files(id)")
                .execute();

        if (!existing.getFiles().isEmpty()) {
            rootFolderId = existing.getFiles().get(0).getId();
            return rootFolderId;
        }

        File meta = new File();
        meta.setName(ROOT_FOLDER_NAME);
        meta.setMimeType("application/vnd.google-apps.folder");

        rootFolderId = drive.files().create(meta).setFields("id").execute().getId();
        System.out.println("[GoogleDriveService] Created root folder: " + ROOT_FOLDER_NAME);
        return rootFolderId;
    }

    // finds or creates a course subfolder inside the root, caches the id
    private String getOrCreateCourseFolder(Drive drive, String rootId, String courseName) throws Exception {

        String safe = sanitize(courseName);
        if (folderIdCache.containsKey(safe)) return folderIdCache.get(safe);

        FileList existing = drive.files().list()
                .setQ("name = '" + safe + "' "
                        + "and '" + rootId + "' in parents "
                        + "and mimeType = 'application/vnd.google-apps.folder' "
                        + "and trashed = false")
                .setFields("files(id)")
                .execute();

        String folderId;
        if (!existing.getFiles().isEmpty()) {
            folderId = existing.getFiles().get(0).getId();
        } else {
            File meta = new File();
            meta.setName(safe);
            meta.setMimeType("application/vnd.google-apps.folder");
            meta.setParents(Collections.singletonList(rootId));
            folderId = drive.files().create(meta).setFields("id").execute().getId();
            System.out.println("[GoogleDriveService] Created course folder: " + safe);
        }

        folderIdCache.put(safe, folderId);
        return folderId;
    }

    // returns just file names in a folder, used for duplicate checking
    private Set<String> listFileNamesInFolder(Drive drive, String folderId) throws Exception {
        FileList result = drive.files().list()
                .setQ("'" + folderId + "' in parents and trashed = false")
                .setFields("files(name)")
                .execute();

        Set<String> names = new HashSet<>();
        for (File f : result.getFiles()) names.add(f.getName());
        return names;
    }

    // strips illegal chars from folder/file names
    private String sanitize(String name) {
        return name.replaceAll("[\\/:*?\"<>|\\\\]", "_").trim();
    }

    // upload result summary sent back to frontend
    public record UploadResult(String courseName, int uploaded, int skipped, int failed) {}

    // basic file info for listing drive contents
    public record DriveFileInfo(String id, String name, long size, String webViewLink, String mimeType) {}
}