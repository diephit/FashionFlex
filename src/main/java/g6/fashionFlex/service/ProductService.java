package g6.fashionFlex.service;

import g6.fashionFlex.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ProductService {
    // Get all products
    List<Product> findAll();

    // Get all active products (list)
    List<Product> findAllActive();

    // Get all active products (paginated)
    Page<Product> findAllActive(Pageable pageable);

    // Get product by ID
    Optional<Product> findById(Long id);

    // Get featured products (for homepage)
    List<Product> getFeaturedProducts();

    // Get latest products (for homepage)
    List<Product> getLatestProducts(int limit);

    // Get best sellers (for homepage)
    List<Product> getBestSellers();

    // Get products by category ID
    Page<Product> getProductsByCategory(Long categoryId, Pageable pageable);

    // Get products by category name
    Page<Product> getProductsByCategoryName(String categoryName, Pageable pageable);

    // Get products by price range
    Page<Product> getProductsByPriceRange(BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);

    // Search products
    Page<Product> searchProducts(String keyword, Pageable pageable);

    // Get products with discount
    Page<Product> getProductsWithDiscount(Pageable pageable);

    // Save product
    Product save(Product product);

    // Delete product
    void delete(Long id);

    // Increment view count
    void incrementViewCount(Long id);
}
