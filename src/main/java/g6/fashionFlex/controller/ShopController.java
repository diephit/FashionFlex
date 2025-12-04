package g6.fashionFlex.controller;

import g6.fashionFlex.dto.ProductQuickViewDTO;
import g6.fashionFlex.entity.Product;
import g6.fashionFlex.repository.ProductRepository;
import g6.fashionFlex.service.ProductService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class ShopController extends BaseController {  

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductService productService;

    @GetMapping("/product")
    public String showProductPage(
            @RequestParam(value = "categoryId", required = false) Integer categoryId,
            Model model, 
            HttpSession session) {  
        addAuthenticationToModel(model, session);  
        
        List<Product> products;
        if (categoryId != null && categoryId > 0) {
            // Filter by category (includes subcategories)
            products = productRepository.findByTopLevelCategory(categoryId);
        } else {
            // Show all active products
            products = productRepository.findAllActive();
        }
        
        model.addAttribute("products", products);
        model.addAttribute("activeCategoryId", categoryId);
        return "product";
    }

    @GetMapping("/product-detail/{productId}")
    public String showProductDetail(@PathVariable Integer productId, Model model, HttpSession session) {  // CHỈ THÊM HttpSession session
        addAuthenticationToModel(model, session);  // CHỈ THÊM DÒNG NÀY
        
        // GIỮ NGUYÊN LOGIC CŨ
        ProductQuickViewDTO productDetail = productService.getQuickViewByProductId(productId);
        model.addAttribute("productDetail", productDetail);
        return "product-detail";
    }
}