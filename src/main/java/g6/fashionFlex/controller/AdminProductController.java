package g6.fashionFlex.controller;

import g6.fashionFlex.dto.ProductDTO;
import g6.fashionFlex.entity.Brand;
import g6.fashionFlex.entity.Category;
import g6.fashionFlex.repository.BrandRepository;
import g6.fashionFlex.repository.CategoryRepository;
import g6.fashionFlex.service.AdminProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/products")
@PreAuthorize("hasRole('ADMIN')")
@Slf4j
public class AdminProductController {

    @Autowired
    private AdminProductService productService;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private BrandRepository brandRepository;

    // Show product list page
    @GetMapping
    public String listProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String keyword,
            Model model) {

        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<ProductDTO> productPage;
        if (keyword != null && !keyword.isEmpty()) {
            productPage = productService.searchProducts(keyword, pageable);
            model.addAttribute("keyword", keyword);
        } else {
            productPage = productService.getAllProducts(pageable);
        }

        model.addAttribute("products", productPage.getContent());
        model.addAttribute("currentPage", productPage.getNumber());
        model.addAttribute("totalItems", productPage.getTotalElements());
        model.addAttribute("totalPages", productPage.getTotalPages());
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDir", sortDir);

        return "admin/products/list";
    }

    // Show create product form
    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("product", new ProductDTO());
        loadFormData(model);
        return "admin/products/form";
    }

    // Show edit product form
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            ProductDTO product = productService.getProductById(id);
            model.addAttribute("product", product);
            loadFormData(model);
            return "admin/products/form";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", "Product not found: " + e.getMessage());
            return "redirect:/admin/products";
        }
    }

    // Create or update product
    @PostMapping("/save")
    public String saveProduct(
            @ModelAttribute("product") ProductDTO productDTO,
            @RequestParam(value = "mainImage", required = false) MultipartFile mainImage,
            @RequestParam(value = "additionalImagesUpload", required = false) List<MultipartFile> additionalImagesUpload,
            RedirectAttributes redirectAttributes,
            Model model) {

        // Manual validation for required fields
        if (productDTO.getName() == null || productDTO.getName().trim().isEmpty()) {
            model.addAttribute("error", "Product name is required");
            loadFormData(model);
            return "admin/products/form";
        }

        if (productDTO.getPrice() == null || productDTO.getPrice().doubleValue() <= 0) {
            model.addAttribute("error", "Valid price is required");
            loadFormData(model);
            return "admin/products/form";
        }

        if (productDTO.getStock() == null || productDTO.getStock() < 0) {
            model.addAttribute("error", "Valid stock quantity is required");
            loadFormData(model);
            return "admin/products/form";
        }

        if (productDTO.getCategoryId() == null) {
            model.addAttribute("error", "Category is required");
            loadFormData(model);
            return "admin/products/form";
        }

        // Parse sizes and colors input
        if (productDTO.getSizesInput() != null && !productDTO.getSizesInput().trim().isEmpty()) {
            productDTO.parseSizesInput();
        }
        if (productDTO.getColorsInput() != null && !productDTO.getColorsInput().trim().isEmpty()) {
            productDTO.parseColorsInput();
        }

        try {
            // Set uploaded files
            productDTO.setMainImage(mainImage);
            productDTO.setAdditionalImagesFiles(additionalImagesUpload);

            if (productDTO.getId() == null) {
                productService.createProduct(productDTO);
                redirectAttributes.addFlashAttribute("success", "Product created successfully");
            } else {
                productService.updateProduct(productDTO.getId(), productDTO);
                redirectAttributes.addFlashAttribute("success", "Product updated successfully");
            }
            return "redirect:/admin/products";
        } catch (Exception e) {
            log.error("Error saving product", e);
            model.addAttribute("error", "Error saving product: " + e.getMessage());
            loadFormData(model);
            return "admin/products/form";
        }
    }

    // Delete product
    @GetMapping("/delete/{id}")
    public String deleteProduct(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            productService.deleteProduct(id);
            redirectAttributes.addFlashAttribute("success", "Product deleted successfully");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", "Error deleting product: " + e.getMessage());
        }
        return "redirect:/admin/products";
    }

    // Toggle product status
    @GetMapping("/toggle/{id}")
    public String toggleProductStatus(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            productService.toggleProductStatus(id);
            redirectAttributes.addFlashAttribute("success", "Product status updated successfully");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", "Error updating product status: " + e.getMessage());
        }
        return "redirect:/admin/products";
    }

    // Helper method to load form data
    private void loadFormData(Model model) {
        // Load categories
        List<Category> categories = categoryRepository.findAll();
        model.addAttribute("categories", categories);

        // Load brands
        List<Brand> brands = brandRepository.findByActiveTrueOrderByNameAsc();
        model.addAttribute("brands", brands);
    }
}
