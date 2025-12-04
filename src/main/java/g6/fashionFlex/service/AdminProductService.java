package g6.fashionFlex.service;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import g6.fashionFlex.entity.Admin;
import g6.fashionFlex.entity.Product;
import g6.fashionFlex.entity.Product.ProductStatus;
import g6.fashionFlex.entity.ProductVariant;
import g6.fashionFlex.entity.Stock;
import g6.fashionFlex.repository.AdminRepository;
import g6.fashionFlex.repository.CartItemRepository;
import g6.fashionFlex.repository.OrderItemRepository;
import g6.fashionFlex.repository.ProductRepository;
import g6.fashionFlex.repository.ProductVariantRepository;
import g6.fashionFlex.repository.StockRepository;
import g6.fashionFlex.repository.WishlistItemRepository;

@Service
public class AdminProductService {

    private static final Logger logger = LoggerFactory.getLogger(AdminProductService.class);

    @Autowired
    private ProductVariantRepository variantRepository;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private WishlistItemRepository wishlistItemRepository;

    public Page<Product> getAllProducts(Pageable pageable) {
        return productRepository.findAll(pageable);
    }

    public Page<Product> searchProducts(String keyword, Pageable pageable) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return productRepository.findAll(pageable);
        }
        return productRepository.searchProducts(keyword, pageable);
    }

    public Page<Product> getProductsByStatus(ProductStatus status, Pageable pageable) {
        return productRepository.findByStatus(status, pageable);
    }

    public Optional<Product> getProductById(Integer productID) {
        return productRepository.findById(productID);
    }

    @Transactional
    public Product createProduct(Product product, Integer adminID) {
        Admin admin = adminRepository.findById(adminID)
                .orElseThrow(() -> new RuntimeException("Admin not found"));
        product.setCreatedByAdmin(admin);
        product.setUpdatedByAdmin(admin);
        if (product.getStatus() == null) {
            product.setStatus(ProductStatus.active);
        }
        return productRepository.save(product);
    }

    @Transactional
    public Product updateProduct(Integer productID, Product productData, Integer adminID) {
        Product product = productRepository.findById(productID)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        
        Admin admin = adminRepository.findById(adminID)
                .orElseThrow(() -> new RuntimeException("Admin not found"));
        
        product.setName(productData.getName());
        product.setDescription(productData.getDescription());
        product.setCategory(productData.getCategory());
        product.setStockQuantity(productData.getStockQuantity());
        if (productData.getStatus() != null) {
            product.setStatus(productData.getStatus());
        }
        if (productData.getMainImage() != null && !productData.getMainImage().isBlank()) {
            product.setMainImage(productData.getMainImage());
        }
        product.setUpdatedByAdmin(admin);
        
        return productRepository.save(product);
    }

    // DEPRECATED: Không dùng hard delete nữa, chỉ dùng toggleProductStatus để soft delete
    // Giữ lại method này để tránh breaking changes, nhưng sẽ throw exception
    @Deprecated
    @Transactional
    public void deleteProduct(Integer productID) {
        throw new UnsupportedOperationException(
            "Hard delete is not allowed. Use toggleProductStatus() for soft delete instead."
        );
    }

    @Transactional
    public Product toggleProductStatus(Integer productID) {
        Product product = productRepository.findById(productID)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        
        if (product.getStatus() == ProductStatus.active) {
            product.setStatus(ProductStatus.inactive);
        } else {
            product.setStatus(ProductStatus.active);
        }
        
        return productRepository.save(product);
    }

    public List<ProductVariant> getProductVariants(Integer productID) {
        return variantRepository.findByProductProductID(productID);
    }

    @Transactional
    public ProductVariant createVariant(Integer productID, ProductVariant variant) {
        Product product = productRepository.findById(productID)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        
        if (variantRepository.existsBySku(variant.getSku())) {
            throw new RuntimeException("SKU already exists");
        }
        
        variant.setProduct(product);
        return variantRepository.save(variant);
    }

    @Transactional
    public ProductVariant updateVariant(Integer variantID, ProductVariant variantData) {
        ProductVariant variant = variantRepository.findById(variantID)
                .orElseThrow(() -> new RuntimeException("Variant not found"));
        
        if (!variant.getSku().equals(variantData.getSku()) && 
            variantRepository.existsBySku(variantData.getSku())) {
            throw new RuntimeException("SKU already exists");
        }
        
        variant.setSku(variantData.getSku());
        variant.setPrice(variantData.getPrice());
        if (variantData.getVariantImage() != null && !variantData.getVariantImage().isBlank()) {
            variant.setVariantImage(variantData.getVariantImage());
        }
        
        return variantRepository.save(variant);
    }

    @Transactional
    public void deleteVariant(Integer variantID) {
        ProductVariant variant = variantRepository.findById(variantID)
                .orElseThrow(() -> new RuntimeException("Variant not found"));
        
        logger.info("Starting deletion of variant ID: {}", variantID);
        
        long cartCount = cartItemRepository.countByVariantVariantID(variantID);
        if (cartCount > 0) {
            cartItemRepository.findByVariantVariantID(variantID)
                    .forEach(cartItemRepository::delete);
            logger.debug("Deleted {} cart items for variant {}", cartCount, variantID);
        }
        
        long orderCount = orderItemRepository.countByVariantVariantID(variantID);
        if (orderCount > 0) {
            orderItemRepository.findByVariantVariantID(variantID)
                    .forEach(orderItemRepository::delete);
            logger.debug("Deleted {} order items for variant {}", orderCount, variantID);
        }
        
        long wishCount = wishlistItemRepository.countByVariantVariantID(variantID);
        if (wishCount > 0) {
            wishlistItemRepository.findByVariantVariantID(variantID)
                    .forEach(wishlistItemRepository::delete);
            logger.debug("Deleted {} wishlist items for variant {}", wishCount, variantID);
        }
        
        List<Stock> stocks = stockRepository.findByVariantVariantID(variantID);
        if (!stocks.isEmpty()) {
            stockRepository.deleteAll(stocks);
            logger.debug("Deleted {} stock records for variant {}", stocks.size(), variantID);
        }
        
        variantRepository.delete(variant);
        logger.info("Successfully deleted variant ID: {}", variantID);
    }

    @Transactional
    public Product increaseStock(Integer productID, int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be greater than 0");
        }
        Product product = productRepository.findById(productID)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        int current = product.getStockQuantity() != null ? product.getStockQuantity() : 0;
        product.setStockQuantity(current + amount);
        return productRepository.save(product);
    }
}
