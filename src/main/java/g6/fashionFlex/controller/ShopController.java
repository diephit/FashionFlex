package g6.fashionFlex.controller;

import g6.fashionFlex.dto.ProductQuickViewDTO;
import g6.fashionFlex.entity.Product;
import g6.fashionFlex.repository.ProductRepository;
import g6.fashionFlex.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@Controller
public class ShopController {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductService productService;

    @GetMapping("/product")
    public String showProductPage(Model model) {
        List<Product> products = productRepository.findAll();
        model.addAttribute("products", products);
        return "product";
    }

    @GetMapping("/product-detail/{productId}")
    public String showProductDetail(@PathVariable Integer productId, Model model) {
        ProductQuickViewDTO productDetail = productService.getQuickViewByProductId(productId);
        model.addAttribute("productDetail", productDetail);
        return "product-detail";
    }
}
