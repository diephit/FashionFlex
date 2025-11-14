package g6.fashionFlex.controller;

import g6.fashionFlex.dto.ProductQuickViewDTO;
import g6.fashionFlex.service.ProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
public class ProductApiController {

    private final ProductService productService;

    public ProductApiController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/{productId}/quick-view")
    public ResponseEntity<ProductQuickViewDTO> getProductQuickView(@PathVariable Integer productId) {
        ProductQuickViewDTO response = productService.getQuickViewByProductId(productId);
        return ResponseEntity.ok(response);
    }
}

