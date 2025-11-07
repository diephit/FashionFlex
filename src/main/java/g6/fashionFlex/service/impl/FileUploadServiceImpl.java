package g6.fashionFlex.service.impl;

import g6.fashionFlex.service.FileUploadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class FileUploadServiceImpl implements FileUploadService {

    @Value("${file.upload.base-dir:src/main/resources/static/images}")
    private String baseUploadDir;

    private static final List<String> ALLOWED_IMAGE_TYPES = Arrays.asList(
            "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp"
    );

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

    @Override
    public String uploadFile(MultipartFile file, String subfolder) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be null or empty");
        }

        if (!isImageFile(file)) {
            throw new IllegalArgumentException("Only image files are allowed");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size exceeds maximum allowed size of 5MB");
        }

        // Create upload directory if it doesn't exist
        Path uploadPath = Paths.get(baseUploadDir, subfolder);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String fileExtension = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : ".jpg";
        String uniqueFilename = UUID.randomUUID().toString() + fileExtension;

        // Save file
        Path filePath = uploadPath.resolve(uniqueFilename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        log.info("File uploaded successfully: {}", filePath);

        // Return relative path for web access
        return "/images/" + subfolder + "/" + uniqueFilename;
    }

    @Override
    public List<String> uploadFiles(List<MultipartFile> files, String subfolder) throws IOException {
        List<String> uploadedPaths = new ArrayList<>();

        if (files == null || files.isEmpty()) {
            return uploadedPaths;
        }

        for (MultipartFile file : files) {
            if (file != null && !file.isEmpty()) {
                try {
                    String path = uploadFile(file, subfolder);
                    uploadedPaths.add(path);
                } catch (Exception e) {
                    log.error("Failed to upload file: {}", file.getOriginalFilename(), e);
                    // Continue with other files
                }
            }
        }

        return uploadedPaths;
    }

    @Override
    public boolean deleteFile(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return false;
        }

        try {
            // Remove leading slash if present
            String cleanPath = filePath.startsWith("/") ? filePath.substring(1) : filePath;

            // Construct full path
            Path fullPath = Paths.get("src/main/resources/static", cleanPath);

            if (Files.exists(fullPath)) {
                Files.delete(fullPath);
                log.info("File deleted successfully: {}", fullPath);
                return true;
            } else {
                log.warn("File not found: {}", fullPath);
                return false;
            }
        } catch (Exception e) {
            log.error("Failed to delete file: {}", filePath, e);
            return false;
        }
    }

    @Override
    public boolean isImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return false;
        }

        String contentType = file.getContentType();
        return contentType != null && ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase());
    }
}
