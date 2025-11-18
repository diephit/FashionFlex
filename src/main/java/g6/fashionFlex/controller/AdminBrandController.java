package g6.fashionFlex.controller;

import g6.fashionFlex.dto.BrandDTO;
import g6.fashionFlex.service.AdminBrandService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/brands")
@PreAuthorize("hasRole('ADMIN')")
public class AdminBrandController {

    @Autowired
    private AdminBrandService brandService;

    /**
     * Display list of all brands
     */
    @GetMapping
    public String listBrands(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean active,
            Model model) {

        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<BrandDTO> brandPage;

        // Use search if filters are provided
        if (keyword != null || active != null) {
            brandPage = brandService.searchBrands(keyword, active, pageable);
        } else {
            brandPage = brandService.getAllBrands(pageable);
        }

        // Add statistics
        AdminBrandService.BrandStatistics statistics = brandService.getStatistics();
        model.addAttribute("statistics", statistics);

        // Add current filters to model for persistence
        model.addAttribute("keyword", keyword);
        model.addAttribute("active", active);

        model.addAttribute("brands", brandPage.getContent());
        model.addAttribute("currentPage", brandPage.getNumber());
        model.addAttribute("totalItems", brandPage.getTotalElements());
        model.addAttribute("totalPages", brandPage.getTotalPages());
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDir", sortDir);

        return "admin/brands/list";
    }

    /**
     * Show form to create new brand
     */
    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("brand", new BrandDTO());
        model.addAttribute("isEdit", false);
        return "admin/brands/form";
    }

    /**
     * Show form to edit existing brand
     */
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            BrandDTO brand = brandService.getBrandById(id);
            model.addAttribute("brand", brand);
            model.addAttribute("isEdit", true);
            return "admin/brands/form";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error loading brand: " + e.getMessage());
            return "redirect:/admin/brands";
        }
    }

    /**
     * Save brand (create or update)
     */
    @PostMapping("/save")
    public String saveBrand(
            @ModelAttribute("brand") BrandDTO brandDTO,
            @RequestParam(value = "logoFile", required = false) MultipartFile logoFile,
            RedirectAttributes redirectAttributes,
            Model model) {

        try {
            // Basic validation
            if (brandDTO.getName() == null || brandDTO.getName().trim().isEmpty()) {
                model.addAttribute("error", "Brand name is required");
                model.addAttribute("isEdit", brandDTO.getId() != null);
                return "admin/brands/form";
            }

            if (brandDTO.getId() == null) {
                // Create new brand
                brandService.createBrand(brandDTO, logoFile);
                redirectAttributes.addFlashAttribute("success", "Brand created successfully!");
            } else {
                // Update existing brand
                brandService.updateBrand(brandDTO.getId(), brandDTO, logoFile);
                redirectAttributes.addFlashAttribute("success", "Brand updated successfully!");
            }
            return "redirect:/admin/brands";
        } catch (IOException e) {
            model.addAttribute("error", "Error uploading logo: " + e.getMessage());
            model.addAttribute("isEdit", brandDTO.getId() != null);
            return "admin/brands/form";
        } catch (Exception e) {
            model.addAttribute("error", "Error saving brand: " + e.getMessage());
            model.addAttribute("isEdit", brandDTO.getId() != null);
            return "admin/brands/form";
        }
    }

    /**
     * Delete brand
     */
    @GetMapping("/delete/{id}")
    public String deleteBrand(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            brandService.deleteBrand(id);
            redirectAttributes.addFlashAttribute("success", "Brand deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error deleting brand: " + e.getMessage());
        }
        return "redirect:/admin/brands";
    }

    /**
     * Toggle brand active status
     */
    @GetMapping("/toggle/{id}")
    public String toggleBrandStatus(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            BrandDTO brand = brandService.toggleBrandStatus(id);
            String status = brand.getActive() ? "activated" : "deactivated";
            redirectAttributes.addFlashAttribute("success", "Brand " + status + " successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error toggling brand status: " + e.getMessage());
        }
        return "redirect:/admin/brands";
    }

    // Bulk Actions
    @PostMapping("/bulk/activate")
    @ResponseBody
    public ResponseEntity<?> bulkActivate(@RequestBody Map<String, List<Long>> payload) {
        try {
            List<Long> ids = payload.get("ids");
            int count = brandService.bulkActivate(ids);
            return ResponseEntity.ok(Map.of("success", true, "message", count + " brands activated"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/bulk/deactivate")
    @ResponseBody
    public ResponseEntity<?> bulkDeactivate(@RequestBody Map<String, List<Long>> payload) {
        try {
            List<Long> ids = payload.get("ids");
            int count = brandService.bulkDeactivate(ids);
            return ResponseEntity.ok(Map.of("success", true, "message", count + " brands deactivated"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/bulk/delete")
    @ResponseBody
    public ResponseEntity<?> bulkDelete(@RequestBody Map<String, List<Long>> payload) {
        try {
            List<Long> ids = payload.get("ids");
            int count = brandService.bulkDelete(ids);
            return ResponseEntity.ok(Map.of("success", true, "message", count + " brands deleted (empty only)"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
