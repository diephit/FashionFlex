package g6.fashionFlex.controller;

import g6.fashionFlex.dto.CouponDTO;
import g6.fashionFlex.entity.Coupon;
import g6.fashionFlex.service.AdminCouponService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;

@Controller
@RequestMapping("/admin/coupons")
@PreAuthorize("hasRole('ADMIN')")
@Slf4j
public class AdminCouponController {

    @Autowired
    private AdminCouponService couponService;

    /**
     * Show coupon list page
     */
    @GetMapping
    public String listCoupons(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String keyword,
            Model model) {

        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<CouponDTO> couponPage;
        if (keyword != null && !keyword.isEmpty()) {
            couponPage = couponService.searchCoupons(keyword, pageable);
            model.addAttribute("keyword", keyword);
        } else {
            couponPage = couponService.getAllCoupons(pageable);
        }

        model.addAttribute("coupons", couponPage.getContent());
        model.addAttribute("currentPage", couponPage.getNumber());
        model.addAttribute("totalItems", couponPage.getTotalElements());
        model.addAttribute("totalPages", couponPage.getTotalPages());
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDir", sortDir);

        // Add statistics
        AdminCouponService.CouponStatistics stats = couponService.getStatistics();
        model.addAttribute("statistics", stats);

        return "admin/coupons/list";
    }

    /**
     * Show create coupon form
     */
    @GetMapping("/new")
    public String showCreateForm(Model model) {
        CouponDTO couponDTO = new CouponDTO();
        // Set default values
        couponDTO.setStartDate(LocalDateTime.now());
        couponDTO.setEndDate(LocalDateTime.now().plusMonths(1));
        couponDTO.setActive(true);
        couponDTO.setDiscountType(Coupon.DiscountType.PERCENTAGE);

        model.addAttribute("coupon", couponDTO);
        model.addAttribute("isEdit", false);
        return "admin/coupons/form";
    }

    /**
     * Show edit coupon form
     */
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            CouponDTO coupon = couponService.getCouponById(id);
            model.addAttribute("coupon", coupon);
            model.addAttribute("isEdit", true);
            return "admin/coupons/form";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", "Coupon not found: " + e.getMessage());
            return "redirect:/admin/coupons";
        }
    }

    /**
     * Create or update coupon
     */
    @PostMapping("/save")
    public String saveCoupon(
            @Valid @ModelAttribute("coupon") CouponDTO couponDTO,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            Model model) {

        // Check for validation errors
        if (bindingResult.hasErrors()) {
            model.addAttribute("isEdit", couponDTO.getId() != null);
            return "admin/coupons/form";
        }

        try {
            if (couponDTO.getId() == null) {
                // Create new coupon
                couponService.createCoupon(couponDTO);
                redirectAttributes.addFlashAttribute("success", "Coupon created successfully!");
            } else {
                // Update existing coupon
                couponService.updateCoupon(couponDTO.getId(), couponDTO);
                redirectAttributes.addFlashAttribute("success", "Coupon updated successfully!");
            }
            return "redirect:/admin/coupons";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("isEdit", couponDTO.getId() != null);
            return "admin/coupons/form";
        } catch (Exception e) {
            log.error("Error saving coupon", e);
            model.addAttribute("error", "An error occurred while saving coupon: " + e.getMessage());
            model.addAttribute("isEdit", couponDTO.getId() != null);
            return "admin/coupons/form";
        }
    }

    /**
     * Delete coupon
     */
    @PostMapping("/delete/{id}")
    public String deleteCoupon(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            couponService.deleteCoupon(id);
            redirectAttributes.addFlashAttribute("success", "Coupon deleted successfully!");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", "Error deleting coupon: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error deleting coupon", e);
            redirectAttributes.addFlashAttribute("error", "An error occurred while deleting coupon");
        }
        return "redirect:/admin/coupons";
    }

    /**
     * Toggle active status
     */
    @PostMapping("/toggle/{id}")
    public String toggleActiveStatus(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            CouponDTO coupon = couponService.toggleActiveStatus(id);
            String status = coupon.getActive() ? "activated" : "deactivated";
            redirectAttributes.addFlashAttribute("success", "Coupon " + status + " successfully!");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", "Error toggling coupon status: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error toggling coupon status", e);
            redirectAttributes.addFlashAttribute("error", "An error occurred while toggling coupon status");
        }
        return "redirect:/admin/coupons";
    }

    /**
     * Deactivate expired coupons
     */
    @PostMapping("/deactivate-expired")
    public String deactivateExpiredCoupons(RedirectAttributes redirectAttributes) {
        try {
            int count = couponService.deactivateExpiredCoupons();
            redirectAttributes.addFlashAttribute("success", "Deactivated " + count + " expired coupon(s)");
        } catch (Exception e) {
            log.error("Error deactivating expired coupons", e);
            redirectAttributes.addFlashAttribute("error", "An error occurred while deactivating expired coupons");
        }
        return "redirect:/admin/coupons";
    }

    /**
     * Deactivate coupons that reached limit
     */
    @PostMapping("/deactivate-limit-reached")
    public String deactivateLimitReachedCoupons(RedirectAttributes redirectAttributes) {
        try {
            int count = couponService.deactivateLimitReachedCoupons();
            redirectAttributes.addFlashAttribute("success", "Deactivated " + count + " coupon(s) that reached usage limit");
        } catch (Exception e) {
            log.error("Error deactivating limit-reached coupons", e);
            redirectAttributes.addFlashAttribute("error", "An error occurred while deactivating coupons");
        }
        return "redirect:/admin/coupons";
    }
}
