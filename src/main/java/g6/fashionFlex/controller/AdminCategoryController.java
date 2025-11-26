package g6.fashionFlex.controller;

import g6.fashionFlex.dto.CategoryDTO;
import g6.fashionFlex.service.AdminCategoryService;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;


@Controller
@RequestMapping("/admin/categories")
@PreAuthorize("hasRole('ADMIN')")
public class AdminCategoryController {

    @Autowired
    private AdminCategoryService categoryService;

    @GetMapping
    public String listCategories(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean active,
            Model model) {

        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<CategoryDTO> categoryPage;

        // Use search if filters are provided
        if (keyword != null || active != null) {
            categoryPage = categoryService.searchCategories(keyword, active, pageable);
        } else {
            categoryPage = categoryService.getAllCategories(pageable);
        }

        // Add statistics
        AdminCategoryService.CategoryStatistics statistics = categoryService.getStatistics();
        model.addAttribute("statistics", statistics);

        // Add current filters to model for persistence
        model.addAttribute("keyword", keyword);
        model.addAttribute("active", active);

        model.addAttribute("categories", categoryPage.getContent());
        model.addAttribute("currentPage", categoryPage.getNumber());
        model.addAttribute("totalItems", categoryPage.getTotalElements());
        model.addAttribute("totalPages", categoryPage.getTotalPages());
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDir", sortDir);

        return "admin/categories/list";
    }


        @GetMapping("/new")
        public String showCreateForm(Model model) {
            model.addAttribute("category", new CategoryDTO());
            return "admin/categories/form";
        }

        @GetMapping("/edit/{id}")
        public String showEditForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
            try {
                CategoryDTO category = categoryService.getCategoryById(id);
                model.addAttribute("category", category);
                return "admin/categories/form";
            } catch (RuntimeException e) {
                redirectAttributes.addFlashAttribute("error", "Category not found: " + e.getMessage());
                return "redirect:/admin/categories";
            }
        }

        @PostMapping("/save")
        public String saveCategory(@ModelAttribute CategoryDTO categoryDTO, RedirectAttributes redirectAttributes) {
            try {
                if (categoryDTO.getId() == null) {
                    categoryService.createCategory(categoryDTO);
                    redirectAttributes.addFlashAttribute("success", "Category created successfully");
                } else {
                    categoryService.updateCategory(categoryDTO.getId(), categoryDTO);
                    redirectAttributes.addFlashAttribute("success", "Category updated successfully");
                }
                return "redirect:/admin/categories";
            } catch (RuntimeException e) {
                redirectAttributes.addFlashAttribute("error", "Error saving category: " + e.getMessage());
                return "redirect:/admin/categories/new";
            }
        }

        @GetMapping("/delete/{id}")
        public String deleteCategory(@PathVariable Long id, RedirectAttributes redirectAttributes) {
            try {
                categoryService.deleteCategory(id);
                redirectAttributes.addFlashAttribute("success", "Category deleted successfully");
            } catch (RuntimeException e) {
                redirectAttributes.addFlashAttribute("error", "Error deleting category: " + e.getMessage());
            }
            return "redirect:/admin/categories";
        }

    @GetMapping("/toggle/{id}")
    public String toggleCategoryStatus(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            categoryService.toggleCategoryStatus(id);
            redirectAttributes.addFlashAttribute("success", "Category status updated successfully");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", "Error updating category status: " + e.getMessage());
        }
        return "redirect:/admin/categories";
    }

    // Bulk Actions
    @PostMapping("/bulk/activate")
    @ResponseBody
    public ResponseEntity<?> bulkActivate(@RequestBody Map<String, List<Long>> payload) {
        try {
            List<Long> ids = payload.get("ids");
            int count = categoryService.bulkActivate(ids);
            return ResponseEntity.ok(Map.of("success", true, "message", count + " categories activated"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/bulk/deactivate")
    @ResponseBody
    public ResponseEntity<?> bulkDeactivate(@RequestBody Map<String, List<Long>> payload) {
        try {
            List<Long> ids = payload.get("ids");
            int count = categoryService.bulkDeactivate(ids);
            return ResponseEntity.ok(Map.of("success", true, "message", count + " categories deactivated"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/bulk/delete")
    @ResponseBody
    public ResponseEntity<?> bulkDelete(@RequestBody Map<String, List<Long>> payload) {
        try {
            List<Long> ids = payload.get("ids");
            int count = categoryService.bulkDelete(ids);
            return ResponseEntity.ok(Map.of("success", true, "message", count + " categories deleted (empty only)"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
