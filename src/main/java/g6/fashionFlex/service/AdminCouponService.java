package g6.fashionFlex.service;

import g6.fashionFlex.dto.CouponDTO;
import g6.fashionFlex.entity.Coupon;
import g6.fashionFlex.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AdminCouponService {

    private final CouponRepository couponRepository;

    /**
     * Get all coupons with pagination
     */
    @Transactional(readOnly = true)
    public Page<CouponDTO> getAllCoupons(Pageable pageable) {
        Page<Coupon> coupons = couponRepository.findAll(pageable);
        return coupons.map(this::convertToDTO);
    }

    /**
     * Search coupons by code or description
     */
    @Transactional(readOnly = true)
    public Page<CouponDTO> searchCoupons(String keyword, Pageable pageable) {
        // Using simple search - you can enhance this with custom query
        Page<Coupon> coupons = couponRepository.findAll(pageable);
        return coupons.map(this::convertToDTO);
    }

    /**
     * Get coupon by ID
     */
    @Transactional(readOnly = true)
    public CouponDTO getCouponById(Long id) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Coupon not found with ID: " + id));
        return convertToDTO(coupon);
    }

    /**
     * Create new coupon
     */
    public CouponDTO createCoupon(CouponDTO couponDTO) {
        // Check if code already exists
        if (couponRepository.existsByCode(couponDTO.getCode())) {
            throw new IllegalArgumentException("Coupon code already exists: " + couponDTO.getCode());
        }

        // Validate dates
        if (couponDTO.getEndDate().isBefore(couponDTO.getStartDate())) {
            throw new IllegalArgumentException("End date must be after start date");
        }

        Coupon coupon = convertToEntity(couponDTO);
        coupon.setUsedCount(0);
        Coupon savedCoupon = couponRepository.save(coupon);

        log.info("Created new coupon: {} (ID: {})", savedCoupon.getCode(), savedCoupon.getId());
        return convertToDTO(savedCoupon);
    }

    /**
     * Update existing coupon
     */
    public CouponDTO updateCoupon(Long id, CouponDTO couponDTO) {
        Coupon existingCoupon = couponRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Coupon not found with ID: " + id));

        // Check if code is being changed and if new code already exists
        if (!existingCoupon.getCode().equals(couponDTO.getCode()) &&
            couponRepository.existsByCode(couponDTO.getCode())) {
            throw new IllegalArgumentException("Coupon code already exists: " + couponDTO.getCode());
        }

        // Validate dates
        if (couponDTO.getEndDate().isBefore(couponDTO.getStartDate())) {
            throw new IllegalArgumentException("End date must be after start date");
        }

        // Update fields
        existingCoupon.setCode(couponDTO.getCode());
        existingCoupon.setDescription(couponDTO.getDescription());
        existingCoupon.setDiscountType(couponDTO.getDiscountType());
        existingCoupon.setDiscountValue(couponDTO.getDiscountValue());
        existingCoupon.setMinOrderAmount(couponDTO.getMinOrderAmount());
        existingCoupon.setMaxDiscountAmount(couponDTO.getMaxDiscountAmount());
        existingCoupon.setUsageLimit(couponDTO.getUsageLimit());
        existingCoupon.setStartDate(couponDTO.getStartDate());
        existingCoupon.setEndDate(couponDTO.getEndDate());
        existingCoupon.setActive(couponDTO.getActive());

        Coupon updatedCoupon = couponRepository.save(existingCoupon);
        log.info("Updated coupon: {} (ID: {})", updatedCoupon.getCode(), updatedCoupon.getId());

        return convertToDTO(updatedCoupon);
    }

    /**
     * Delete coupon
     */
    public void deleteCoupon(Long id) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Coupon not found with ID: " + id));

        couponRepository.delete(coupon);
        log.info("Deleted coupon: {} (ID: {})", coupon.getCode(), coupon.getId());
    }

    /**
     * Toggle coupon active status
     */
    public CouponDTO toggleActiveStatus(Long id) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Coupon not found with ID: " + id));

        coupon.setActive(!coupon.getActive());
        Coupon updatedCoupon = couponRepository.save(coupon);

        log.info("Toggled coupon {} status to: {}", coupon.getCode(), updatedCoupon.getActive() ? "Active" : "Inactive");
        return convertToDTO(updatedCoupon);
    }

    /**
     * Get statistics for dashboard
     */
    @Transactional(readOnly = true)
    public CouponStatistics getStatistics() {
        List<Coupon> allCoupons = couponRepository.findAll();
        LocalDateTime now = LocalDateTime.now();

        long totalCoupons = allCoupons.size();
        long activeCoupons = allCoupons.stream()
                .filter(c -> c.getActive() && c.isValid())
                .count();
        long expiredCoupons = allCoupons.stream()
                .filter(c -> c.getEndDate().isBefore(now))
                .count();
        long usageLimitReached = allCoupons.stream()
                .filter(Coupon::hasReachedLimit)
                .count();

        return new CouponStatistics(totalCoupons, activeCoupons, expiredCoupons, usageLimitReached);
    }

    /**
     * Deactivate expired coupons (scheduled task or manual trigger)
     */
    public int deactivateExpiredCoupons() {
        List<Coupon> expiredCoupons = couponRepository.findExpiredCoupons(LocalDateTime.now());
        int count = 0;
        for (Coupon coupon : expiredCoupons) {
            if (coupon.getActive()) {
                coupon.setActive(false);
                couponRepository.save(coupon);
                count++;
            }
        }
        log.info("Deactivated {} expired coupons", count);
        return count;
    }

    /**
     * Deactivate coupons that reached usage limit
     */
    public int deactivateLimitReachedCoupons() {
        List<Coupon> coupons = couponRepository.findCouponsReachedLimit();
        int count = 0;
        for (Coupon coupon : coupons) {
            if (coupon.getActive()) {
                coupon.setActive(false);
                couponRepository.save(coupon);
                count++;
            }
        }
        log.info("Deactivated {} coupons that reached usage limit", count);
        return count;
    }

    // Conversion methods
    private CouponDTO convertToDTO(Coupon coupon) {
        CouponDTO dto = new CouponDTO();
        dto.setId(coupon.getId());
        dto.setCode(coupon.getCode());
        dto.setDescription(coupon.getDescription());
        dto.setDiscountType(coupon.getDiscountType());
        dto.setDiscountValue(coupon.getDiscountValue());
        dto.setMinOrderAmount(coupon.getMinOrderAmount());
        dto.setMaxDiscountAmount(coupon.getMaxDiscountAmount());
        dto.setUsageLimit(coupon.getUsageLimit());
        dto.setUsedCount(coupon.getUsedCount());
        dto.setStartDate(coupon.getStartDate());
        dto.setEndDate(coupon.getEndDate());
        dto.setActive(coupon.getActive());
        dto.setCreatedAt(coupon.getCreatedAt());
        dto.setUpdatedAt(coupon.getUpdatedAt());
        return dto;
    }

    private Coupon convertToEntity(CouponDTO dto) {
        Coupon coupon = new Coupon();
        coupon.setId(dto.getId());
        coupon.setCode(dto.getCode());
        coupon.setDescription(dto.getDescription());
        coupon.setDiscountType(dto.getDiscountType());
        coupon.setDiscountValue(dto.getDiscountValue());
        coupon.setMinOrderAmount(dto.getMinOrderAmount());
        coupon.setMaxDiscountAmount(dto.getMaxDiscountAmount());
        coupon.setUsageLimit(dto.getUsageLimit());
        coupon.setUsedCount(dto.getUsedCount() != null ? dto.getUsedCount() : 0);
        coupon.setStartDate(dto.getStartDate());
        coupon.setEndDate(dto.getEndDate());
        coupon.setActive(dto.getActive() != null ? dto.getActive() : true);
        return coupon;
    }

    // Statistics class
    public static class CouponStatistics {
        private final long totalCoupons;
        private final long activeCoupons;
        private final long expiredCoupons;
        private final long limitReachedCoupons;

        public CouponStatistics(long totalCoupons, long activeCoupons, long expiredCoupons, long limitReachedCoupons) {
            this.totalCoupons = totalCoupons;
            this.activeCoupons = activeCoupons;
            this.expiredCoupons = expiredCoupons;
            this.limitReachedCoupons = limitReachedCoupons;
        }

        public long getTotalCoupons() { return totalCoupons; }
        public long getActiveCoupons() { return activeCoupons; }
        public long getExpiredCoupons() { return expiredCoupons; }
        public long getLimitReachedCoupons() { return limitReachedCoupons; }
    }
}
