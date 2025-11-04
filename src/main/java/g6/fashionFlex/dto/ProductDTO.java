package g6.fashionFlex.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductDTO {
    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private BigDecimal discountPrice;
    private Integer stock;
    private String sku;
    private String imageUrl;
    private Set<String> additionalImages;
    private Long categoryId;
    private String categoryName;
    private Boolean active;
    private Boolean featured;
    private Set<String> availableSizes;
    private Set<String> availableColors;
    private Long viewCount;
    private Long soldCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
