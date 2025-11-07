package g6.fashionFlex.controller;

import g6.fashionFlex.dto.UserDTO;
import g6.fashionFlex.entity.Product;
import g6.fashionFlex.service.ProductService;
import g6.fashionFlex.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;

@Controller
public class ProductController {

    @Autowired
    private ProductService productService;

    @Autowired
    private UserService userService;

    @GetMapping("/product")
    public String listProducts(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String priceRange,
            @RequestParam(required = false) String tag,
            @RequestParam(defaultValue = "0") int page,
            Model model) {

        // Add authentication info
        addAuthenticationInfo(model);

        // Set page size
        int pageSize = 12;

        // Determine sort order
        Sort sortOrder = getSortOrder(sort);
        Pageable pageable = PageRequest.of(page, pageSize, sortOrder);

        // Fetch products based on filters
        Page<Product> productPage;

        if (search != null && !search.trim().isEmpty()) {
            // Search by keyword
            productPage = productService.searchProducts(search.trim(), pageable);
            model.addAttribute("search", search);
        } else if (category != null && !category.trim().isEmpty()) {
            // Filter by category (need to implement this method)
            productPage = productService.getProductsByCategoryName(category.trim(), pageable);
            model.addAttribute("category", category);
        } else if (priceRange != null && !priceRange.equals("all")) {
            // Filter by price range
            productPage = getProductsByPriceRange(priceRange, pageable);
            model.addAttribute("priceRange", priceRange);
        } else {
            // Get all active products
            productPage = productService.findAllActive(pageable);
        }

        // Add model attributes
        model.addAttribute("products", productPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", productPage.getTotalPages());
        model.addAttribute("totalItems", productPage.getTotalElements());
        model.addAttribute("sort", sort);

        return "product";
    }

    @GetMapping("/products")
    public String listProductsAlias(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String priceRange,
            @RequestParam(required = false) String tag,
            @RequestParam(defaultValue = "0") int page,
            Model model) {
        return listProducts(category, search, sort, priceRange, tag, page, model);
    }

    @GetMapping("/product/{id}")
    public String productDetail(@PathVariable Long id, Model model) {
        // Add authentication info
        addAuthenticationInfo(model);

        Product product = productService.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));

        model.addAttribute("product", product);

        // Get related products (same category)
        if (product.getCategory() != null) {
            Pageable pageable = PageRequest.of(0, 4);
            Page<Product> relatedProducts = productService.getProductsByCategory(
                    product.getCategory().getId(), pageable);
            model.addAttribute("relatedProducts", relatedProducts.getContent());
        }

        return "product-detail";
    }

    @GetMapping("/product/quick-view/{id}")
    public String quickViewProduct(@PathVariable Long id, Model model) {
        Product product = productService.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));

        model.addAttribute("product", product);
        return "fragments/product-quick-view :: modal-content";
    }

    // Helper methods
    private void addAuthenticationInfo(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated()
                && !authentication.getName().equals("anonymousUser")) {
            try {
                String email = authentication.getName();
                UserDTO user = userService.getUserByEmail(email);
                model.addAttribute("user", user);
                model.addAttribute("isAuthenticated", true);
            } catch (Exception e) {
                model.addAttribute("isAuthenticated", false);
            }
        } else {
            model.addAttribute("isAuthenticated", false);
        }
    }

    private Sort getSortOrder(String sort) {
        if (sort == null || sort.equals("default")) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }

        switch (sort) {
            case "newest":
                return Sort.by(Sort.Direction.DESC, "createdAt");
            case "price-asc":
                return Sort.by(Sort.Direction.ASC, "price");
            case "price-desc":
                return Sort.by(Sort.Direction.DESC, "price");
            case "popularity":
                return Sort.by(Sort.Direction.DESC, "soldCount");
            default:
                return Sort.by(Sort.Direction.DESC, "createdAt");
        }
    }

    private Page<Product> getProductsByPriceRange(String priceRange, Pageable pageable) {
        BigDecimal minPrice;
        BigDecimal maxPrice;

        switch (priceRange) {
            case "0-50":
                minPrice = BigDecimal.ZERO;
                maxPrice = new BigDecimal("50.00");
                break;
            case "50-100":
                minPrice = new BigDecimal("50.00");
                maxPrice = new BigDecimal("100.00");
                break;
            case "100-150":
                minPrice = new BigDecimal("100.00");
                maxPrice = new BigDecimal("150.00");
                break;
            case "150-200":
                minPrice = new BigDecimal("150.00");
                maxPrice = new BigDecimal("200.00");
                break;
            case "200+":
                minPrice = new BigDecimal("200.00");
                maxPrice = new BigDecimal("999999.00");
                break;
            default:
                return productService.findAllActive(pageable);
        }

        return productService.getProductsByPriceRange(minPrice, maxPrice, pageable);
    }
}
