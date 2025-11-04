package g6.fashionFlex.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "products")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 2000)
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "discount_price", precision = 10, scale = 2)
    private BigDecimal discountPrice;

    @Column(nullable = false)
    private Integer stock = 0;

    @Column(length = 100)
    private String sku;

    @Column(length = 500)
    private String imageUrl;

    @ElementCollection
    @CollectionTable(name = "product_images", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "image_url")
    private Set<String> additionalImages = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(nullable = false)
    private Boolean featured = false;

    @ElementCollection
    @CollectionTable(name = "product_sizes", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "size")
    private Set<String> availableSizes = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "product_colors", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "color")
    private Set<String> availableColors = new HashSet<>();

    @Column(name = "view_count")
    private Long viewCount = 0L;

    @Column(name = "sold_count")
    private Long soldCount = 0L;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    private void validatePrices() {
        if (discountPrice != null && discountPrice.compareTo(price) > 0) {
            throw new IllegalArgumentException("Discount price cannot be greater than original price");
        }
    }

    public BigDecimal getEffectivePrice() {
        return discountPrice != null ? discountPrice : price;
    }

    public boolean hasDiscount() {
        return discountPrice != null && discountPrice.compareTo(price) < 0;
    }

    public int getDiscountPercentage() {
        if (!hasDiscount()) {
            return 0;
        }
        BigDecimal discount = price.subtract(discountPrice);
        return discount.multiply(BigDecimal.valueOf(100)).divide(price, 0, java.math.RoundingMode.HALF_UP).intValue();
    }
}
