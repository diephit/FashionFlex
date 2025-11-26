package g6.fashionFlex.repository;

import g6.fashionFlex.entity.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, Long> {

    /**
     * Find coupon by code
     */
    Optional<Coupon> findByCode(String code);

    /**
     * Find coupon by code (case insensitive)
     */
    Optional<Coupon> findByCodeIgnoreCase(String code);

    /**
     * Check if coupon code exists
     */
    boolean existsByCode(String code);

    /**
     * Find all active coupons
     */
    List<Coupon> findByActiveTrue();

    /**
     * Find valid coupons (active and within date range)
     */
    @Query("SELECT c FROM Coupon c WHERE c.active = true " +
           "AND c.startDate <= :now " +
           "AND c.endDate >= :now " +
           "AND (c.usageLimit IS NULL OR c.usedCount < c.usageLimit)")
    List<Coupon> findValidCoupons(@Param("now") LocalDateTime now);

    /**
     * Find valid coupon by code
     */
    @Query("SELECT c FROM Coupon c WHERE UPPER(c.code) = UPPER(:code) " +
           "AND c.active = true " +
           "AND c.startDate <= :now " +
           "AND c.endDate >= :now " +
           "AND (c.usageLimit IS NULL OR c.usedCount < c.usageLimit)")
    Optional<Coupon> findValidCouponByCode(@Param("code") String code, @Param("now") LocalDateTime now);

    /**
     * Find expired coupons
     */
    @Query("SELECT c FROM Coupon c WHERE c.endDate < :now")
    List<Coupon> findExpiredCoupons(@Param("now") LocalDateTime now);

    /**
     * Find coupons that have reached usage limit
     */
    @Query("SELECT c FROM Coupon c WHERE c.usageLimit IS NOT NULL AND c.usedCount >= c.usageLimit")
    List<Coupon> findCouponsReachedLimit();
}
