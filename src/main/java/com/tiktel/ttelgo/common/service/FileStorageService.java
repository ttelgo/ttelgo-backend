package com.tiktel.ttelgo.common.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

/**
 * Stores uploaded files to local disk and returns a publicly accessible URL path.
 * Files are served from /uploads/** via Spring static-resource handler.
 */
@Service
@Slf4j
public class FileStorageService {

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif"
    );
    private static final long MAX_SIZE_BYTES = 5 * 1024 * 1024; // 5 MB

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    private Path uploadPath;

    @PostConstruct
    public void init() {
        uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(uploadPath);
            log.info("Upload directory ready: {}", uploadPath);
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory: " + uploadPath, e);
        }
    }

    /**
     * Validates and stores a profile picture.
     *
     * @param file      the uploaded multipart file
     * @param subFolder sub-folder inside uploads dir (e.g. "profile-pictures")
     * @return public URL path (e.g. "/uploads/profile-pictures/uuid.jpg")
     */
    public String storeProfilePicture(MultipartFile file, String subFolder) throws IOException {
        // ── Validate ────────────────────────────────────────────────────────────
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File must not be empty.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("Only JPEG, PNG, WebP, or GIF images are allowed.");
        }

        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new IllegalArgumentException("File size must not exceed 5 MB.");
        }

        // ── Build unique filename ────────────────────────────────────────────────
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename() != null
                ? file.getOriginalFilename() : "upload");
        String extension = "";
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex >= 0) {
            extension = originalFilename.substring(dotIndex).toLowerCase();
        } else {
            // Fall back to content-type derived extension
            extension = switch (contentType.toLowerCase()) {
                case "image/png" -> ".png";
                case "image/webp" -> ".webp";
                case "image/gif" -> ".gif";
                default -> ".jpg";
            };
        }

        String uniqueFilename = UUID.randomUUID() + extension;

        // ── Create sub-folder and copy file ─────────────────────────────────────
        Path targetDir = uploadPath.resolve(subFolder).normalize();
        Files.createDirectories(targetDir);

        Path targetPath = targetDir.resolve(uniqueFilename).normalize();

        // Prevent path traversal
        if (!targetPath.startsWith(uploadPath)) {
            throw new IllegalArgumentException("Invalid file path detected.");
        }

        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        log.info("Stored file: {}", targetPath);

        // Return URL path the frontend can use directly
        return "/uploads/" + subFolder + "/" + uniqueFilename;
    }

    /**
     * Deletes a previously stored file given its URL path (e.g. "/uploads/profile-pictures/uuid.jpg").
     * Silently ignores missing files.
     */
    public void deleteFile(String urlPath) {
        if (urlPath == null || urlPath.isBlank()) return;
        try {
            // Strip leading slash and resolve against upload root's parent
            String relative = urlPath.startsWith("/") ? urlPath.substring(1) : urlPath;
            // urlPath looks like "uploads/profile-pictures/uuid.jpg"
            Path filePath = Paths.get(relative).toAbsolutePath().normalize();
            Files.deleteIfExists(filePath);
            log.info("Deleted file: {}", filePath);
        } catch (IOException e) {
            log.warn("Could not delete file {}: {}", urlPath, e.getMessage());
        }
    }
}
