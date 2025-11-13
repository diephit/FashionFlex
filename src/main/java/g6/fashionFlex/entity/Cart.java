package g6.fashionFlex.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "carts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CartItem> items = new ArrayList<>();

    @Column(name = "coupon_code", length = 50)
    private String couponCode;

    @Column(name = "discount_amount", precision = 10, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "selected_shipping_method", length = 100)
    private String selectedShippingMethod;

    @Column(name = "shipping_cost", precision = 10, scale = 2)
    private BigDecimal shippingCost = BigDecimal.ZERO;

    @Column(name = "tax_amount", precision = 10, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "shipping_country", length = 50)
    private String shippingCountry;

    @Column(name = "shipping_state", length = 50)
    private String shippingState;

    @Column(name = "shipping_postcode", length = 20)
    private String shippingPostcode;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Helper methods
    public void addItem(CartItem item) {
        items.add(item);
        item.setCart(this);
    }

    public void removeItem(CartItem item) {
        items.remove(item);
        item.setCart(null);
    }

    public void clearItems() {
        items.forEach(item -> item.setCart(null));
        items.clear();
    }

    /**
     * Calculate subtotal (sum of all cart items before any discounts)
     *
     * Formula: Σ(item.effectivePrice × item.quantity)
     *
     * Example:
     *   Product A: $50 × 2 = $100
     *   Product B: $30 × 1 = $30
     *   Subtotal = $130
     *
     * @return Subtotal amount before coupon discount
     */
    public BigDecimal calculateSubtotal() {
        return items.stream()
                .map(CartItem::calculateItemTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Calculate total after coupon discount
     *
     * Formula: Subtotal - Discount
     *
     * Example:
     *   Subtotal: $130
     *   Coupon "SAVE10" (10%): -$13
     *   Total = $117
     *
     * Note: Tax is calculated on this amount (after discount)
     *
     * @return Total amount after discount, before shipping and tax
     */
    public BigDecimal calculateTotal() {
        BigDecimal subtotal = calculateSubtotal();
        return subtotal.subtract(discountAmount != null ? discountAmount : BigDecimal.ZERO);
    }

    /**
     * Calculate final total including shipping and tax
     *
     * Formula: Total + Shipping + Tax
     * Where:
     *   - Total = Subtotal - Discount
     *   - Tax = Total × TaxRate (calculated on amount after discount)
     *
     * Complete Example:
     *   Items:
     *     Product A: $50 × 2 = $100
     *     Product B: $30 × 1 = $30
     *
     *   Subtotal: $130
     *   Coupon "SAVE10" (10%): -$13
     *   Total: $117
     *   Shipping (Standard US): $5
     *   Tax (CA 7.25%): $8.48 (= $117 × 0.0725)
     *   ━━━━━━━━━━━━━━━━━━━━━
     *   Final Total: $130.48
     *
     * @return Final total amount customer needs to pay
     */
    public BigDecimal calculateFinalTotal() {
        BigDecimal total = calculateTotal();
        BigDecimal shipping = shippingCost != null ? shippingCost : BigDecimal.ZERO;
        BigDecimal tax = taxAmount != null ? taxAmount : BigDecimal.ZERO;
        return total.add(shipping).add(tax);
    }

    /**
     * Get total number of items in cart
     */
    public int getTotalItems() {
        return items.stream()
                .mapToInt(CartItem::getQuantity)
                .sum();
    }

    /**
     * Apply coupon discount
     */
    public void applyCoupon(String couponCode, BigDecimal discountAmount) {
        this.couponCode = couponCode;
        this.discountAmount = discountAmount;
    }

    /**
     * Remove coupon
     */
    public void removeCoupon() {
        this.couponCode = null;
        this.discountAmount = BigDecimal.ZERO;
    }

    /**
     * Select shipping method
     */
    public void selectShippingMethod(String method, BigDecimal cost, BigDecimal tax,
                                     String country, String state, String postcode) {
        this.selectedShippingMethod = method;
        this.shippingCost = cost;
        this.taxAmount = tax;
        this.shippingCountry = country;
        this.shippingState = state;
        this.shippingPostcode = postcode;
    }

    /**
     * Clear shipping selection
     */
    public void clearShippingSelection() {
        this.selectedShippingMethod = null;
        this.shippingCost = BigDecimal.ZERO;
        this.taxAmount = BigDecimal.ZERO;
        this.shippingCountry = null;
        this.shippingState = null;
        this.shippingPostcode = null;
    }

    /**
     * Check if shipping method is selected
     */
    public boolean hasShippingMethod() {
        return selectedShippingMethod != null && shippingCost != null;
    }
}
