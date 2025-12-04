package g6.fashionFlex.controller;

import g6.fashionFlex.entity.Category;
import g6.fashionFlex.entity.Category.CategoryStatus;
import g6.fashionFlex.service.AdminCategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/admin/categories")
public class AdminCategoryController {

    @Autowired
    private AdminCategoryService categoryService;

    @GetMapping
    public String listCategories(@RequestParam(required = false) String keyword,
                                @RequestParam(required = false) Integer editId,
                                Model model) {
        List<Category> categories;
        if (keyword != null && !keyword.trim().isEmpty()) {
            categories = categoryService.searchCategories(keyword);
        } else {
            categories = categoryService.getAllCategories();
        }
        
        model.addAttribute("categories", categories);
        model.addAttribute("rootCategories", categoryService.getRootCategories());
        model.addAttribute("keyword", keyword);
        model.addAttribute("totalCategories", categoryService.countAllCategories());
        model.addAttribute("activeCategories", categoryService.countByStatus(CategoryStatus.active));
        model.addAttribute("inactiveCategories", categoryService.countByStatus(CategoryStatus.inactive));
        model.addAttribute("totalProducts", categoryService.countTotalProducts());
        model.addAttribute("categoryStatusValues", CategoryStatus.values());
        model.addAttribute("newCategory", new Category());
        if (editId != null) {
            Optional<Category> editCategory = categoryService.getCategoryById(editId);
            editCategory.ifPresent(category -> model.addAttribute("editCategory", category));
        }
        return "admin/categories";
    }

    @PostMapping
    public String createCategory(@ModelAttribute Category category,
                                @RequestParam(required = false) Integer parentCategoryID,
                                RedirectAttributes redirectAttributes) {
        try {
            if (parentCategoryID != null) {
                categoryService.getCategoryById(parentCategoryID)
                    .ifPresent(category::setParentCategory);
            }
            categoryService.createCategory(category);
            redirectAttributes.addFlashAttribute("success", "Category created successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error creating category: " + e.getMessage());
        }
        
        return "redirect:/admin/categories";
    }

    @PostMapping("/{id}")
    public String updateCategory(@PathVariable Integer id,
                                @ModelAttribute Category category,
                                @RequestParam(required = false) Integer parentCategoryID,
                                RedirectAttributes redirectAttributes) {
        try {
            if (parentCategoryID != null) {
                categoryService.getCategoryById(parentCategoryID)
                    .ifPresent(category::setParentCategory);
            } else {
                category.setParentCategory(null);
            }
            categoryService.updateCategory(id, category);
            redirectAttributes.addFlashAttribute("success", "Category updated successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error updating category: " + e.getMessage());
        }
        
        return "redirect:/admin/categories";
    }

    @PostMapping("/{id}/delete")
    public String deleteCategory(@PathVariable Integer id,
                                RedirectAttributes redirectAttributes) {
        try {
            categoryService.deleteCategory(id);
            redirectAttributes.addFlashAttribute("success", "Category deleted successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error deleting category: " + e.getMessage());
        }
        
        return "redirect:/admin/categories";
    }

    @PostMapping("/{id}/toggle-status")
    public String toggleCategoryStatus(@PathVariable Integer id,
                                     RedirectAttributes redirectAttributes) {
        try {
            categoryService.toggleCategoryStatus(id);
            redirectAttributes.addFlashAttribute("success", "Category status updated");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error updating status: " + e.getMessage());
        }
        
        return "redirect:/admin/categories";
    }
}

