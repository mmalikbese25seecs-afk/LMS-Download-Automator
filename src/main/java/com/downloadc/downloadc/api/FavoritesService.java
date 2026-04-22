package com.downloadc.downloadc.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

// saves pinned course ids to downloads/favorites.json as a simple int array e.g. [12,47,93]
// used by CourseService, FavoritesController, and DashboardService
@Service
public class FavoritesService {

    private static final Path FAVORITES_FILE = Paths.get("downloads", "favorites.json");
    private final ObjectMapper objectMapper = new ObjectMapper();

    public synchronized Set<Integer> getFavoriteIds() {
        if (!Files.exists(FAVORITES_FILE)) return new HashSet<>();
        try {
            return objectMapper.readValue(
                    FAVORITES_FILE.toFile(),
                    new TypeReference<Set<Integer>>() {}
            );
        } catch (IOException e) {
            System.out.println("[FavoritesService] Read error: " + e.getMessage());
            return new HashSet<>();
        }
    }

    public boolean isFavorite(int courseId) {
        return getFavoriteIds().contains(courseId);
    }

    // returns true if newly added, false if already in the list
    public synchronized boolean addFavorite(int courseId) {
        try {
            Set<Integer> ids = getFavoriteIds();
            if (ids.add(courseId)) {
                save(ids);
                System.out.println("[FavoritesService] Pinned course " + courseId);
                return true;
            }
            return false;
        } catch (IOException e) {
            System.out.println("[FavoritesService] Save error: " + e.getMessage());
            return false;
        }
    }

    // returns true if removed, false if wasnt there
    public synchronized boolean removeFavorite(int courseId) {
        try {
            Set<Integer> ids = getFavoriteIds();
            if (ids.remove(courseId)) {
                save(ids);
                System.out.println("[FavoritesService] Unpinned course " + courseId);
                return true;
            }
            return false;
        } catch (IOException e) {
            System.out.println("[FavoritesService] Save error: " + e.getMessage());
            return false;
        }
    }

    private void save(Set<Integer> ids) throws IOException {
        Files.createDirectories(FAVORITES_FILE.getParent());
        objectMapper.writeValue(FAVORITES_FILE.toFile(), ids);
    }
}