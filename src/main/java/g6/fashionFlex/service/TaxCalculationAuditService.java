package g6.fashionFlex.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Audit logging service for tax calculations
 * Logs all tax calculations for compliance and debugging purposes
 */
@Service
@Slf4j
public class TaxCalculationAuditService {

    /**
     * Log tax calculation for audit trail
     *
     * @param userId User ID
     * @param cartId Cart ID
     * @param subtotal Cart subtotal (before discount)
     * @param discountAmount Discount amount from coupon
     * @param totalBeforeTax Total after discount (tax base)
     * @param taxRate Tax rate applied
     * @param taxAmount Calculated tax amount
     * @param country Shipping country
     * @param state Shipping state/region
     */
    public void logTaxCalculation(
            Long userId,
            Long cartId,
            BigDecimal subtotal,
            BigDecimal discountAmount,
            BigDecimal totalBeforeTax,
            BigDecimal taxRate,
            BigDecimal taxAmount,
            String country,
            String state
    ) {
        log.info("TAX_CALCULATION | userId={} | cartId={} | subtotal={} | discount={} | taxBase={} | taxRate={} | taxAmount={} | location={},{} | timestamp={}",
                userId,
                cartId,
                subtotal,
                discountAmount != null ? discountAmount : BigDecimal.ZERO,
                totalBeforeTax,
                taxRate,
                taxAmount,
                country,
                state != null ? state : "N/A",
                LocalDateTime.now()
        );
    }

    /**
     * Log shipping selection with tax
     *
     * @param userId User ID
     * @param cartId Cart ID
     * @param shippingMethod Selected shipping method
     * @param shippingCost Shipping cost
     * @param taxAmount Tax amount
     * @param finalTotal Final total amount
     */
    public void logShippingSelection(
            Long userId,
            Long cartId,
            String shippingMethod,
            BigDecimal shippingCost,
            BigDecimal taxAmount,
            BigDecimal finalTotal
    ) {
        log.info("SHIPPING_SELECTED | userId={} | cartId={} | method='{}' | shippingCost={} | tax={} | finalTotal={} | timestamp={}",
                userId,
                cartId,
                shippingMethod,
                shippingCost,
                taxAmount,
                finalTotal,
                LocalDateTime.now()
        );
    }

    /**
     * Log coupon application with tax impact
     *
     * @param userId User ID
     * @param cartId Cart ID
     * @param couponCode Coupon code applied
     * @param discountAmount Discount amount
     * @param oldTax Previous tax amount
     * @param newTax New tax amount after coupon
     */
    public void logCouponTaxImpact(
            Long userId,
            Long cartId,
            String couponCode,
            BigDecimal discountAmount,
            BigDecimal oldTax,
            BigDecimal newTax
    ) {
        BigDecimal taxSavings = oldTax != null && newTax != null
                ? oldTax.subtract(newTax)
                : BigDecimal.ZERO;

        log.info("COUPON_TAX_IMPACT | userId={} | cartId={} | coupon='{}' | discount={} | oldTax={} | newTax={} | taxSavings={} | timestamp={}",
                userId,
                cartId,
                couponCode,
                discountAmount,
                oldTax != null ? oldTax : BigDecimal.ZERO,
                newTax != null ? newTax : BigDecimal.ZERO,
                taxSavings,
                LocalDateTime.now()
        );
    }

    /**
     * Log checkout validation failure
     *
     * @param userId User ID
     * @param reason Validation failure reason
     */
    public void logCheckoutValidationFailure(Long userId, String reason) {
        log.warn("CHECKOUT_VALIDATION_FAILED | userId={} | reason='{}' | timestamp={}",
                userId,
                reason,
                LocalDateTime.now()
        );
    }

    /**
     * Log successful checkout validation
     *
     * @param userId User ID
     * @param cartId Cart ID
     * @param finalTotal Final total amount
     */
    public void logCheckoutValidationSuccess(Long userId, Long cartId, BigDecimal finalTotal) {
        log.info("CHECKOUT_VALIDATION_SUCCESS | userId={} | cartId={} | finalTotal={} | timestamp={}",
                userId,
                cartId,
                finalTotal,
                LocalDateTime.now()
        );
    }
}
