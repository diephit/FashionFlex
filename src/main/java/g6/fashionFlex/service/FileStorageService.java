package g6.fashionFlex.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path productsUploadDir;
    private final Path customersUploadDir;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    public FileStorageService(@Value("${app.upload.products-dir:uploads/products}") String productsDir,
                              @Value("${app.upload.customers-dir:uploads/customers}") String customersDir) {
        try {
            this.productsUploadDir = Paths.get(productsDir).toAbsolutePath().normalize();
            Files.createDirectories(this.productsUploadDir);
            this.customersUploadDir = Paths.get(customersDir).toAbsolutePath().normalize();
            Files.createDirectories(this.customersUploadDir);
        } catch (IOException ex) {
            throw new RuntimeException("Could not initialize upload directory", ex);
        }
    }

    public String storeProductImage(MultipartFile file) throws IOException {
        return storeFile(file, productsUploadDir, "/uploads/products/");
    }

    public String storeCustomerImage(MultipartFile file) throws IOException {
        return storeFile(file, customersUploadDir, "/uploads/customers/");
    }

    private String storeFile(MultipartFile file, Path targetDir, String webPrefix) throws IOException {
        if (file == null || file.isEmpty()) {
            return null;
        }

        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename() != null ? file.getOriginalFilename() : "image");

        if (originalFilename.contains("..")) {
            throw new IOException("Invalid file name: " + originalFilename);
        }

        String extension = "";
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex > 0) {
            extension = originalFilename.substring(dotIndex);
        }

        String uniqueName = LocalDateTime.now().format(FORMATTER) + "-" + UUID.randomUUID() + extension;
        Path targetLocation = targetDir.resolve(uniqueName);
        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

        return webPrefix + uniqueName.replace("\\", "/");
    }
}

