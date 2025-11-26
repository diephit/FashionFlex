package g6.fashionFlex.service;

import g6.fashionFlex.entity.Cart;
import g6.fashionFlex.entity.Coupon;
import g6.fashionFlex.repository.CartRepository;
import g6.fashionFlex.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CouponService {

    private final CouponRepository couponRepository;
    private final CartRepository cartRepository;

    /**
     * Apply coupon to cart
     */
    public BigDecimal applyCouponToCart(Long cartId, String couponCode) {
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new IllegalArgumentException("Cart not found"));

        Coupon coupon = couponRepository.findValidCouponByCode(couponCode, LocalDateTime.now())
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired coupon code"));

        BigDecimal orderAmount = cart.calculateSubtotal();

        // Validate coupon
        if (!coupon.isValid()) {
            throw new IllegalArgumentException("Coupon is not valid");
        }

        if (!coupon.meetsMinimumOrder(orderAmount)) {
            throw new IllegalArgumentException("Order amount does not meet minimum requirement of $" +
                    coupon.getMinOrderAmount());
        }

        // Calculate discount
        BigDecimal discount = coupon.calculateDiscount(orderAmount);

        // Apply discount to cart
        cart.applyCoupon(couponCode, discount);
        cartRepository.save(cart);

        return discount;
    }

    /**
     * Validate coupon code
     */
    @Transactional(readOnly = true)
    public Coupon validateCoupon(String couponCode, BigDecimal orderAmount) {
        Coupon coupon = couponRepository.findValidCouponByCode(couponCode, LocalDateTime.now())
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired coupon code"));

        if (!coupon.isValid()) {
            throw new IllegalArgumentException("Coupon is not valid");
        }

        if (!coupon.meetsMinimumOrder(orderAmount)) {
            throw new IllegalArgumentException("Order amount does not meet minimum requirement of $" +
                    coupon.getMinOrderAmount());
        }

        return coupon;
    }

    /**
     * Calculate discount for coupon code
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateDiscount(String couponCode, BigDecimal orderAmount) {
        Coupon coupon = validateCoupon(couponCode, orderAmount);
        return coupon.calculateDiscount(orderAmount);
    }

    /**
     * Mark coupon as used
     */
    public void markCouponAsUsed(String couponCode) {
        Coupon coupon = couponRepository.findByCodeIgnoreCase(couponCode)
                .orElseThrow(() -> new IllegalArgumentException("Coupon not found"));

        coupon.incrementUsage();
        couponRepository.save(coupon);
    }

    /**
     * Get all active coupons
     */
    @Transactional(readOnly = true)
    public List<Coupon> getAllActiveCoupons() {
        return couponRepository.findByActiveTrue();
    }

    /**
     * Get all valid coupons
     */
    @Transactional(readOnly = true)
    public List<Coupon> getAllValidCoupons() {
        return couponRepository.findValidCoupons(LocalDateTime.now());
    }

    /**
     * Create new coupon
     */
    public Coupon createCoupon(Coupon coupon) {
        if (couponRepository.existsByCode(coupon.getCode())) {
            throw new IllegalArgumentException("Coupon code already exists");
        }
        return couponRepository.save(coupon);
    }

    /**
     * Update coupon
     */
    public Coupon updateCoupon(Long id, Coupon couponDetails) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Coupon not found"));

        // Check if code is being changed and if new code already exists
        if (!coupon.getCode().equals(couponDetails.getCode()) &&
            couponRepository.existsByCode(couponDetails.getCode())) {
            throw new IllegalArgumentException("Coupon code already exists");
        }

        coupon.setCode(couponDetails.getCode());
        coupon.setDescription(couponDetails.getDescription());
        coupon.setDiscountType(couponDetails.getDiscountType());
        coupon.setDiscountValue(couponDetails.getDiscountValue());
        coupon.setMinOrderAmount(couponDetails.getMinOrderAmount());
        coupon.setMaxDiscountAmount(couponDetails.getMaxDiscountAmount());
        coupon.setUsageLimit(couponDetails.getUsageLimit());
        coupon.setStartDate(couponDetails.getStartDate());
        coupon.setEndDate(couponDetails.getEndDate());
        coupon.setActive(couponDetails.getActive());

        return couponRepository.save(coupon);
    }

    /**
     * Delete coupon
     */
    public void deleteCoupon(Long id) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Coupon not found"));
        couponRepository.delete(coupon);
    }

    /**
     * Deactivate expired coupons
     */
    public void deactivateExpiredCoupons() {
        List<Coupon> expiredCoupons = couponRepository.findExpiredCoupons(LocalDateTime.now());
        expiredCoupons.forEach(coupon -> {
            coupon.setActive(false);
            couponRepository.save(coupon);
        });
    }

    /**
     * Deactivate coupons that reached usage limit
     */
    public void deactivateCouponsReachedLimit() {
        List<Coupon> coupons = couponRepository.findCouponsReachedLimit();
        coupons.forEach(coupon -> {
            coupon.setActive(false);
            couponRepository.save(coupon);
        });
    }
}
