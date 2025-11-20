package g6.fashionFlex.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductDTO {

    private Long id;

    @NotBlank(message = "Product name is required")
    @Size(max = 200, message = "Product name must not exceed 200 characters")
    private String name;

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    private BigDecimal price;

    @DecimalMin(value = "0.0", inclusive = false, message = "Discount price must be greater than 0")
    private BigDecimal discountPrice;

    @NotNull(message = "Stock is required")
    @Min(value = 0, message = "Stock cannot be negative")
    private Integer stock;

    @Size(max = 100, message = "SKU must not exceed 100 characters")
    private String sku;

    private String imageUrl;

    // For file upload
    private MultipartFile mainImage;

    private List<MultipartFile> additionalImagesFiles = new ArrayList<>();

    private Set<String> additionalImages = new HashSet<>();

    @NotNull(message = "Category is required")
    private Long categoryId;

    private String categoryName;

    private Long brandId;

    private String brandName;

    private Boolean active = true;

    private Boolean featured = false;

    private Set<String> availableSizes = new HashSet<>();

    private Set<String> availableColors = new HashSet<>();

    // For form input - comma separated values
    private String sizesInput;

    private String colorsInput;

    private Long viewCount = 0L;

    private Long soldCount = 0L;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // Helper methods
    public void parseSizesInput() {
        if (sizesInput != null && !sizesInput.trim().isEmpty()) {
            String[] sizes = sizesInput.split(",");
            availableSizes = new HashSet<>();
            for (String size : sizes) {
                String trimmed = size.trim().toUpperCase();
                if (!trimmed.isEmpty()) {
                    availableSizes.add(trimmed);
                }
            }
        }
    }

    public void parseColorsInput() {
        if (colorsInput != null && !colorsInput.trim().isEmpty()) {
            String[] colors = colorsInput.split(",");
            availableColors = new HashSet<>();
            for (String color : colors) {
                String trimmed = color.trim();
                if (!trimmed.isEmpty()) {
                    availableColors.add(trimmed);
                }
            }
        }
    }

    public String getSizesAsString() {
        return availableSizes != null ? String.join(", ", availableSizes) : "";
    }

    public String getColorsAsString() {
        return availableColors != null ? String.join(", ", availableColors) : "";
    }
}
