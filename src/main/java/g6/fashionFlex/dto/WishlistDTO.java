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
public class WishlistDTO {
    private Long id;
    private Long userId;
    private Long productId;
    private String productName;
    private String productImageUrl;
    private String productSku;
    private BigDecimal price;
    private BigDecimal discountPrice;
    private BigDecimal effectivePrice;
    private Integer stock;
    private Boolean inStock;
    private Boolean hasDiscount;
    private Integer discountPercentage;
    private Set<String> availableSizes;
    private Set<String> availableColors;
    private Boolean hasVariants;
    private LocalDateTime addedAt;
    private String categoryName;
    private String brandName;
}
