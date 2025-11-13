package g6.fashionFlex.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "coupons")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 200)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DiscountType discountType;

    @Column(name = "discount_value", precision = 10, scale = 2, nullable = false)
    private BigDecimal discountValue;

    @Column(name = "min_order_amount", precision = 10, scale = 2)
    private BigDecimal minOrderAmount;

    @Column(name = "max_discount_amount", precision = 10, scale = 2)
    private BigDecimal maxDiscountAmount;

    @Column(name = "usage_limit")
    private Integer usageLimit;

    @Column(name = "used_count", nullable = false)
    private Integer usedCount = 0;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    @Column(nullable = false)
    private Boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Enums
    public enum DiscountType {
        PERCENTAGE,  // Discount by percentage (e.g., 10% off)
        FIXED_AMOUNT // Discount by fixed amount (e.g., $5 off)
    }

    // Helper methods

    /**
     * Check if coupon is currently valid
     */
    public boolean isValid() {
        LocalDateTime now = LocalDateTime.now();
        return active
                && now.isAfter(startDate)
                && now.isBefore(endDate)
                && (usageLimit == null || usedCount < usageLimit);
    }

    /**
     * Check if order amount meets minimum requirement
     */
    public boolean meetsMinimumOrder(BigDecimal orderAmount) {
        return minOrderAmount == null || orderAmount.compareTo(minOrderAmount) >= 0;
    }

    /**
     * Calculate discount amount for given order total
     */
    public BigDecimal calculateDiscount(BigDecimal orderAmount) {
        if (!isValid() || !meetsMinimumOrder(orderAmount)) {
            return BigDecimal.ZERO;
        }

        BigDecimal discount;
        if (discountType == DiscountType.PERCENTAGE) {
            // Calculate percentage discount
            discount = orderAmount.multiply(discountValue).divide(new BigDecimal(100));
        } else {
            // Fixed amount discount
            discount = discountValue;
        }

        // Apply maximum discount cap if set
        if (maxDiscountAmount != null && discount.compareTo(maxDiscountAmount) > 0) {
            discount = maxDiscountAmount;
        }

        // Discount cannot exceed order amount
        if (discount.compareTo(orderAmount) > 0) {
            discount = orderAmount;
        }

        return discount;
    }

    /**
     * Increment usage count
     */
    public void incrementUsage() {
        this.usedCount++;
    }

    /**
     * Check if coupon has reached usage limit
     */
    public boolean hasReachedLimit() {
        return usageLimit != null && usedCount >= usageLimit;
    }

    /**
     * Get remaining uses
     */
    public Integer getRemainingUses() {
        if (usageLimit == null) {
            return null; // Unlimited
        }
        return Math.max(0, usageLimit - usedCount);
    }
}
