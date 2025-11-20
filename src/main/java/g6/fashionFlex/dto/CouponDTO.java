package g6.fashionFlex.dto;

import g6.fashionFlex.entity.Coupon;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CouponDTO {

    private Long id;

    @NotBlank(message = "Coupon code is required")
    @Size(min = 3, max = 50, message = "Coupon code must be between 3 and 50 characters")
    @Pattern(regexp = "^[A-Z0-9_-]+$", message = "Coupon code must contain only uppercase letters, numbers, hyphens, and underscores")
    private String code;

    @NotBlank(message = "Description is required")
    @Size(max = 200, message = "Description must not exceed 200 characters")
    private String description;

    @NotNull(message = "Discount type is required")
    private Coupon.DiscountType discountType;

    @NotNull(message = "Discount value is required")
    @DecimalMin(value = "0.01", message = "Discount value must be greater than 0")
    @DecimalMax(value = "100.00", message = "Percentage discount cannot exceed 100")
    private BigDecimal discountValue;

    @DecimalMin(value = "0.01", message = "Minimum order amount must be greater than 0")
    private BigDecimal minOrderAmount;

    @DecimalMin(value = "0.01", message = "Maximum discount amount must be greater than 0")
    private BigDecimal maxDiscountAmount;

    @Min(value = 1, message = "Usage limit must be at least 1")
    private Integer usageLimit;

    private Integer usedCount = 0;

    @NotNull(message = "Start date is required")
    private LocalDateTime startDate;

    @NotNull(message = "End date is required")
    private LocalDateTime endDate;

    private Boolean active = true;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Helper methods for display
    public String getDiscountTypeDisplay() {
        return discountType == Coupon.DiscountType.PERCENTAGE ? "Percentage" : "Fixed Amount";
    }

    public String getDiscountValueDisplay() {
        if (discountType == Coupon.DiscountType.PERCENTAGE) {
            return discountValue + "%";
        } else {
            return "$" + discountValue;
        }
    }

    public String getStatusDisplay() {
        if (!active) return "Inactive";

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(startDate)) return "Scheduled";
        if (now.isAfter(endDate)) return "Expired";
        if (usageLimit != null && usedCount >= usageLimit) return "Limit Reached";

        return "Active";
    }

    public Integer getRemainingUses() {
        if (usageLimit == null) return null;
        return Math.max(0, usageLimit - usedCount);
    }

    public boolean isValid() {
        LocalDateTime now = LocalDateTime.now();
        return active
                && now.isAfter(startDate)
                && now.isBefore(endDate)
                && (usageLimit == null || usedCount < usageLimit);
    }

    // Validation method
    @AssertTrue(message = "End date must be after start date")
    public boolean isValidDateRange() {
        if (startDate == null || endDate == null) return true;
        return endDate.isAfter(startDate);
    }

    @AssertTrue(message = "Percentage discount value cannot exceed 100")
    public boolean isValidPercentage() {
        if (discountType == null || discountValue == null) return true;
        if (discountType == Coupon.DiscountType.PERCENTAGE) {
            return discountValue.compareTo(new BigDecimal("100")) <= 0;
        }
        return true;
    }

    @AssertTrue(message = "Maximum discount amount must be less than discount value for fixed amount coupons")
    public boolean isValidMaxDiscount() {
        if (discountType == null || maxDiscountAmount == null || discountValue == null) return true;
        if (discountType == Coupon.DiscountType.FIXED_AMOUNT) {
            return maxDiscountAmount.compareTo(discountValue) <= 0;
        }
        return true;
    }
}
