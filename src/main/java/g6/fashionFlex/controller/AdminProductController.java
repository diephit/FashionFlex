package g6.fashionFlex.controller;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import g6.fashionFlex.entity.Admin;
import g6.fashionFlex.entity.Product;
import g6.fashionFlex.entity.Product.ProductStatus;
import g6.fashionFlex.entity.ProductVariant;
import g6.fashionFlex.entity.User;
import g6.fashionFlex.repository.AdminRepository;
import g6.fashionFlex.repository.CategoryRepository;
import g6.fashionFlex.repository.UserRepository;
import g6.fashionFlex.service.AdminProductService;
import g6.fashionFlex.service.FileStorageService;

@Controller
@RequestMapping("/admin/products")
public class AdminProductController {

    private static final Logger logger = LoggerFactory.getLogger(AdminProductController.class);

    @Autowired
    private AdminProductService productService;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private FileStorageService fileStorageService;

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

    private String getAdminDisplayName() {
        Admin admin = getCurrentAdmin();
        if (admin != null && admin.getUser() != null && admin.getUser().getName() != null) {
            return admin.getUser().getName();
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            return auth.getName();
        }
        return "Admin";
    }

    @GetMapping
    public String listProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "productID") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
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
        model.addAttribute("adminDisplayName", getAdminDisplayName());
        
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
        model.addAttribute("adminDisplayName", getAdminDisplayName());
        return "admin/product-detail";
    }

    @PostMapping
    public String createProduct(@ModelAttribute Product product,
                               @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                               RedirectAttributes redirectAttributes) {
        Admin admin = getCurrentAdmin();
        if (admin == null) {
            redirectAttributes.addFlashAttribute("error", "Admin not found");
            return "redirect:/admin/products";
        }
        
        try {
            if (imageFile != null && !imageFile.isEmpty()) {
                String imagePath = fileStorageService.storeProductImage(imageFile);
                product.setMainImage(imagePath);
            }
            productService.createProduct(product, admin.getAdminID());
            redirectAttributes.addFlashAttribute("success", "Product created successfully");
        } catch (IOException e) {
            logger.error("Error uploading image: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Error uploading image: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Error creating product: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Error creating product: " + e.getMessage());
        }
        
        return "redirect:/admin/products";
    }

    @PostMapping("/{id}")
    public String updateProduct(@PathVariable Integer id,
                               @ModelAttribute Product product,
                               @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                               RedirectAttributes redirectAttributes) {
        Admin admin = getCurrentAdmin();
        if (admin == null) {
            redirectAttributes.addFlashAttribute("error", "Admin not found");
            return "redirect:/admin/products";
        }
        
        try {
            if (imageFile != null && !imageFile.isEmpty()) {
                String imagePath = fileStorageService.storeProductImage(imageFile);
                product.setMainImage(imagePath);
            }
            productService.updateProduct(id, product, admin.getAdminID());
            redirectAttributes.addFlashAttribute("success", "Product updated successfully");
        } catch (IOException e) {
            logger.error("Error uploading image: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Error uploading image: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Error updating product: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Error updating product: " + e.getMessage());
        }
        
        return "redirect:/admin/products/" + id;
    }

    @PostMapping("/{id}/delete")
    public String deleteProduct(@PathVariable Integer id,
                               RedirectAttributes redirectAttributes) {
        Admin admin = getCurrentAdmin();
        if (admin == null) {
            logger.warn("Unauthorized delete attempt - admin not found");
            redirectAttributes.addFlashAttribute("error", "Admin not found");
            return "redirect:/admin/products";
        }
        
        try {
            logger.info("Admin {} attempting to deactivate product {}", admin.getAdminID(), id);
            // Soft delete: chỉ toggle status sang inactive thay vì hard delete
            Product product = productService.toggleProductStatus(id);
            String status = product.getStatus() == Product.ProductStatus.inactive ? "deactivated" : "activated";
            logger.info("Product {} {} successfully by admin {}", id, status, admin.getAdminID());
            redirectAttributes.addFlashAttribute("success", "Product " + status + " successfully");
            return "redirect:/admin/products";
        } catch (RuntimeException e) {
            logger.error("Delete product error: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Cannot delete product: " + e.getMessage());
            return "redirect:/admin/products";
        } catch (Exception e) {
            logger.error("Unexpected error deleting product: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "System error: " + e.getMessage());
            return "redirect:/admin/products";
        }
    }

    @PostMapping("/{id}/toggle-status")
    public String toggleProductStatus(@PathVariable Integer id,
                                     RedirectAttributes redirectAttributes) {
        try {
            productService.toggleProductStatus(id);
            redirectAttributes.addFlashAttribute("success", "Product status updated");
        } catch (Exception e) {
            logger.error("Error updating status: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Error updating status: " + e.getMessage());
        }
        
        return "redirect:/admin/products";
    }

    @PostMapping("/{productId}/variants")
    public String createVariant(@PathVariable Integer productId,
                                @RequestParam String sku,
                                @RequestParam BigDecimal price,
                                @RequestParam(value = "variantImage", required = false) MultipartFile variantImage,
                                RedirectAttributes redirectAttributes) {
        try {
            ProductVariant variant = new ProductVariant();
            variant.setSku(sku);
            variant.setPrice(price);
            if (variantImage != null && !variantImage.isEmpty()) {
                String imagePath = fileStorageService.storeProductImage(variantImage);
                variant.setVariantImage(imagePath);
            }
            
            productService.createVariant(productId, variant);
            redirectAttributes.addFlashAttribute("success", "Variant created successfully");
        } catch (IOException e) {
            logger.error("Error uploading variant image: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Error uploading variant image: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Error creating variant: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Error creating variant: " + e.getMessage());
        }
        
        return "redirect:/admin/products/" + productId;
    }

    @PostMapping("/variants/{variantId}")
    public String updateVariant(@PathVariable Integer variantId,
                               @RequestParam String sku,
                               @RequestParam BigDecimal price,
                               @RequestParam Integer productId,
                               @RequestParam(value = "variantImage", required = false) MultipartFile variantImage,
                               RedirectAttributes redirectAttributes) {
        try {
            ProductVariant variant = new ProductVariant();
            variant.setSku(sku);
            variant.setPrice(price);
            if (variantImage != null && !variantImage.isEmpty()) {
                String imagePath = fileStorageService.storeProductImage(variantImage);
                variant.setVariantImage(imagePath);
            }
            
            productService.updateVariant(variantId, variant);
            redirectAttributes.addFlashAttribute("success", "Variant updated successfully");
        } catch (IOException e) {
            logger.error("Error uploading variant image: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Error uploading variant image: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Error updating variant: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Error updating variant: " + e.getMessage());
        }
        
        return "redirect:/admin/products/" + productId;
    }

    @PostMapping("/variants/{variantId}/delete")
    public String deleteVariant(@PathVariable Integer variantId,
                               @RequestParam Integer productId,
                               RedirectAttributes redirectAttributes) {
        Admin admin = getCurrentAdmin();
        if (admin == null) {
            logger.warn("Unauthorized delete attempt - admin not found");
            redirectAttributes.addFlashAttribute("error", "Admin not found");
            return "redirect:/admin/products/" + productId;
        }
        
        try {
            logger.info("Admin {} attempting to delete variant {}", admin.getAdminID(), variantId);
            productService.deleteVariant(variantId);
            logger.info("Variant {} deleted successfully by admin {}", variantId, admin.getAdminID());
            redirectAttributes.addFlashAttribute("success", "Variant deleted successfully");
            return "redirect:/admin/products/" + productId;
        } catch (RuntimeException e) {
            logger.error("Delete variant error: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Cannot delete variant: " + e.getMessage());
            return "redirect:/admin/products/" + productId;
        } catch (Exception e) {
            logger.error("Unexpected error deleting variant: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "System error: " + e.getMessage());
            return "redirect:/admin/products/" + productId;
        }
    }

    @PostMapping("/{id}/stock")
    public String increaseStock(@PathVariable Integer id,
                                @RequestParam Integer amount,
                                RedirectAttributes redirectAttributes) {
        if (amount == null || amount <= 0) {
            redirectAttributes.addFlashAttribute("error", "Please enter a quantity greater than 0.");
            return "redirect:/admin/products/" + id + "#add-stock";
        }
        try {
            productService.increaseStock(id, amount);
            redirectAttributes.addFlashAttribute("success", "Stock increased by " + amount + " units.");
        } catch (Exception e) {
            logger.error("Error updating stock: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Unable to update stock: " + e.getMessage());
        }
        return "redirect:/admin/products/" + id + "#add-stock";
    }
}