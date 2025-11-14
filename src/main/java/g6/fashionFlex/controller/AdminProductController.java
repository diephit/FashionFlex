package g6.fashionFlex.controller;

import g6.fashionFlex.entity.Admin;
import g6.fashionFlex.entity.Product;
import g6.fashionFlex.entity.Product.ProductStatus;
import g6.fashionFlex.entity.ProductVariant;
import g6.fashionFlex.entity.User;
import g6.fashionFlex.repository.AdminRepository;
import g6.fashionFlex.repository.CategoryRepository;
import g6.fashionFlex.repository.UserRepository;
import g6.fashionFlex.service.AdminProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/admin/products")
public class AdminProductController {

    @Autowired
    private AdminProductService productService;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AdminRepository adminRepository;

    private Admin getCurrentAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        String email = auth.getName();
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return null;
        }
        return adminRepository.findByUserUserID(user.getUserID()).orElse(null);
    }

    @GetMapping
    public String listProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "productID") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            Model model) {
        
        Sort sort = sortDir.equalsIgnoreCase("asc") ? 
            Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<Product> products;
        if (keyword != null && !keyword.trim().isEmpty()) {
            products = productService.searchProducts(keyword, pageable);
            if (status != null && !status.isEmpty()) {
                ProductStatus productStatus = ProductStatus.valueOf(status);
                List<Product> filtered = products.getContent().stream()
                    .filter(p -> p.getStatus() == productStatus)
                    .toList();
                products = new PageImpl<>(filtered, pageable, filtered.size());
            }
        } else if (status != null && !status.isEmpty()) {
            ProductStatus productStatus = ProductStatus.valueOf(status);
            products = productService.getProductsByStatus(productStatus, pageable);
        } else {
            products = productService.getAllProducts(pageable);
        }
        
        model.addAttribute("products", products);
        model.addAttribute("categories", categoryRepository.findAll());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", products.getTotalPages());
        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDir", sortDir);
        
        return "admin/products";
    }

    @GetMapping("/{id}")
    public String viewProduct(@PathVariable Integer id, Model model) {
        Optional<Product> product = productService.getProductById(id);
        if (product.isEmpty()) {
            return "redirect:/admin/products";
        }
        model.addAttribute("product", product.get());
        model.addAttribute("variants", productService.getProductVariants(id));
        model.addAttribute("categories", categoryRepository.findAll());
        return "admin/product-detail";
    }

    @PostMapping
    public String createProduct(@ModelAttribute Product product,
                               RedirectAttributes redirectAttributes) {
        Admin admin = getCurrentAdmin();
        if (admin == null) {
            redirectAttributes.addFlashAttribute("error", "Admin not found");
            return "redirect:/admin/products";
        }
        
        try {
            productService.createProduct(product, admin.getAdminID());
            redirectAttributes.addFlashAttribute("success", "Product created successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error creating product: " + e.getMessage());
        }
        
        return "redirect:/admin/products";
    }

    @PostMapping("/{id}")
    public String updateProduct(@PathVariable Integer id,
                               @ModelAttribute Product product,
                               RedirectAttributes redirectAttributes) {
        Admin admin = getCurrentAdmin();
        if (admin == null) {
            redirectAttributes.addFlashAttribute("error", "Admin not found");
            return "redirect:/admin/products";
        }
        
        try {
            productService.updateProduct(id, product, admin.getAdminID());
            redirectAttributes.addFlashAttribute("success", "Product updated successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error updating product: " + e.getMessage());
        }
        
        return "redirect:/admin/products/" + id;
    }

    @PostMapping("/{id}/delete")
    public String deleteProduct(@PathVariable Integer id,
                               RedirectAttributes redirectAttributes) {
        try {
            productService.deleteProduct(id);
            redirectAttributes.addFlashAttribute("success", "Product deleted successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error deleting product: " + e.getMessage());
        }
        
        return "redirect:/admin/products";
    }

    @PostMapping("/{id}/toggle-status")
    public String toggleProductStatus(@PathVariable Integer id,
                                     RedirectAttributes redirectAttributes) {
        try {
            productService.toggleProductStatus(id);
            redirectAttributes.addFlashAttribute("success", "Product status updated");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error updating status: " + e.getMessage());
        }
        
        return "redirect:/admin/products";
    }

    // Variant Management
    @PostMapping("/{productId}/variants")
    public String createVariant(@PathVariable Integer productId,
                                @RequestParam String sku,
                                @RequestParam BigDecimal price,
                                @RequestParam(required = false) String size,
                                @RequestParam(required = false) String color,
                                RedirectAttributes redirectAttributes) {
        try {
            ProductVariant variant = new ProductVariant();
            variant.setSku(sku);
            variant.setPrice(price);
            variant.setSize(size);
            variant.setColor(color);
            
            productService.createVariant(productId, variant);
            redirectAttributes.addFlashAttribute("success", "Variant created successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error creating variant: " + e.getMessage());
        }
        
        return "redirect:/admin/products/" + productId;
    }

    @PostMapping("/variants/{variantId}")
    public String updateVariant(@PathVariable Integer variantId,
                               @RequestParam String sku,
                               @RequestParam BigDecimal price,
                               @RequestParam(required = false) String size,
                               @RequestParam(required = false) String color,
                               @RequestParam Integer productId,
                               RedirectAttributes redirectAttributes) {
        try {
            ProductVariant variant = new ProductVariant();
            variant.setSku(sku);
            variant.setPrice(price);
            variant.setSize(size);
            variant.setColor(color);
            
            productService.updateVariant(variantId, variant);
            redirectAttributes.addFlashAttribute("success", "Variant updated successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error updating variant: " + e.getMessage());
        }
        
        return "redirect:/admin/products/" + productId;
    }

    @PostMapping("/variants/{variantId}/delete")
    public String deleteVariant(@PathVariable Integer variantId,
                               @RequestParam Integer productId,
                               RedirectAttributes redirectAttributes) {
        try {
            productService.deleteVariant(variantId);
            redirectAttributes.addFlashAttribute("success", "Variant deleted successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error deleting variant: " + e.getMessage());
        }
        
        return "redirect:/admin/products/" + productId;
    }
}

