package g6.fashionFlex.controller;

import g6.fashionFlex.dto.CategoryDTO;
import g6.fashionFlex.service.AdminCategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/categories")
@PreAuthorize("hasRole('ADMIN')")
public class AdminCategoryController {

    @Autowired
    private AdminCategoryService categoryService;

    @GetMapping
    public String listCategories(Model model) {
        model.addAttribute("categories", categoryService.getAllCategories());
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
}
