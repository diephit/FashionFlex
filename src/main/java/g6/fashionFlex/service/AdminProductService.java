package g6.fashionFlex.service;

import g6.fashionFlex.entity.Admin;
import g6.fashionFlex.entity.Product;
import g6.fashionFlex.entity.Product.ProductStatus;
import g6.fashionFlex.entity.ProductVariant;
import g6.fashionFlex.repository.AdminRepository;
import g6.fashionFlex.repository.CategoryRepository;
import g6.fashionFlex.repository.ProductRepository;
import g6.fashionFlex.repository.ProductVariantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class AdminProductService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductVariantRepository variantRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private AdminRepository adminRepository;

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
        product.setUpdatedByAdmin(admin);
        
        return productRepository.save(product);
    }

    @Transactional
    public void deleteProduct(Integer productID) {
        Product product = productRepository.findById(productID)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        productRepository.delete(product);
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

    // Variant Management
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
        
        // Check SKU uniqueness if changed
        if (!variant.getSku().equals(variantData.getSku()) && 
            variantRepository.existsBySku(variantData.getSku())) {
            throw new RuntimeException("SKU already exists");
        }
        
        variant.setSku(variantData.getSku());
        variant.setPrice(variantData.getPrice());
        variant.setSize(variantData.getSize());
        variant.setColor(variantData.getColor());
        
        return variantRepository.save(variant);
    }

    @Transactional
    public void deleteVariant(Integer variantID) {
        ProductVariant variant = variantRepository.findById(variantID)
                .orElseThrow(() -> new RuntimeException("Variant not found"));
        variantRepository.delete(variant);
    }
}

