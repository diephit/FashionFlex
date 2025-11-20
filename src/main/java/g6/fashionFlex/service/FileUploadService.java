package g6.fashionFlex.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface FileUploadService {

    /**
     * Upload a single file
     * @param file the file to upload
     * @param subfolder the subfolder (e.g., "products", "brands")
     * @return the relative path to the uploaded file
     */
    String uploadFile(MultipartFile file, String subfolder) throws IOException;

    /**
     * Upload multiple files
     * @param files the files to upload
     * @param subfolder the subfolder
     * @return list of relative paths to uploaded files
     */
    List<String> uploadFiles(List<MultipartFile> files, String subfolder) throws IOException;

    /**
     * Delete a file
     * @param filePath the relative path to the file
     * @return true if deleted successfully
     */
    boolean deleteFile(String filePath);

    /**
     * Check if file is an image
     * @param file the file to check
     * @return true if it's an image
     */
    boolean isImageFile(MultipartFile file);
}
