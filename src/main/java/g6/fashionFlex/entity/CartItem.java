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
@Table(name = "cart_items",
       uniqueConstraints = @UniqueConstraint(columnNames = {"cart_id", "product_id", "size", "color"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private Integer quantity;

    @Column(length = 10)
    private String size;

    @Column(length = 30)
    private String color;

    @Column(name = "price", precision = 10, scale = 2, nullable = false)
    private BigDecimal price;

    @Column(name = "discount_price", precision = 10, scale = 2)
    private BigDecimal discountPrice;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Helper methods

    /**
     * Get effective price (discount price if available, otherwise regular price)
     *
     * Product-level discount takes priority. This is the actual price
     * used for cart calculations.
     *
     * Example:
     *   price = $50, discountPrice = $40 → returns $40
     *   price = $50, discountPrice = null → returns $50
     *
     * @return The price to use for calculations
     */
    public BigDecimal getEffectivePrice() {
        return discountPrice != null && discountPrice.compareTo(BigDecimal.ZERO) > 0
                ? discountPrice
                : price;
    }

    /**
     * Calculate total for this cart item
     *
     * Formula: effectivePrice × quantity
     *
     * Example:
     *   effectivePrice = $40, quantity = 2 → $80
     *
     * @return Line item total before cart-level coupon discount
     */
    public BigDecimal calculateItemTotal() {
        return getEffectivePrice().multiply(new BigDecimal(quantity));
    }

    /**
     * Check if this cart item has a product-level discount
     *
     * @return true if discountPrice is set and lower than regular price
     */
    public boolean hasDiscount() {
        return discountPrice != null && discountPrice.compareTo(BigDecimal.ZERO) > 0
                && discountPrice.compareTo(price) < 0;
    }

    /**
     * Get discount amount per single item
     *
     * Example:
     *   price = $50, discountPrice = $40 → $10 per item
     *
     * @return Discount per item (not multiplied by quantity)
     */
    public BigDecimal getDiscountAmount() {
        if (hasDiscount()) {
            return price.subtract(discountPrice);
        }
        return BigDecimal.ZERO;
    }

    /**
     * Get total savings for this cart item (all quantities)
     *
     * Formula: (price - discountPrice) × quantity
     *
     * Example:
     *   price = $50, discountPrice = $40, quantity = 2
     *   savings = ($50 - $40) × 2 = $20
     *
     * @return Total savings from product discount
     */
    public BigDecimal getTotalSavings() {
        return getDiscountAmount().multiply(new BigDecimal(quantity));
    }

    /**
     * Check if the selected size and color match
     */
    public boolean matches(String size, String color) {
        boolean sizeMatch = (this.size == null && size == null) ||
                           (this.size != null && this.size.equals(size));
        boolean colorMatch = (this.color == null && color == null) ||
                            (this.color != null && this.color.equals(color));
        return sizeMatch && colorMatch;
    }
}
