package g6.fashionFlex.repository;

import g6.fashionFlex.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    // Find by active status
    Page<Product> findByActiveTrue(Pageable pageable);

    List<Product> findByActiveTrue();

    // Find by category
    Page<Product> findByCategoryIdAndActiveTrue(Long categoryId, Pageable pageable);

    // Find featured products
    List<Product> findByFeaturedTrueAndActiveTrueOrderByCreatedAtDesc();

    Page<Product> findByFeaturedTrueAndActiveTrue(Pageable pageable);

    // Search by name
    @Query("SELECT p FROM Product p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) AND p.active = true")
    Page<Product> searchByName(@Param("keyword") String keyword, Pageable pageable);

    // Find by price range
    Page<Product> findByPriceBetweenAndActiveTrue(BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);

    // Find products with discount
    @Query("SELECT p FROM Product p WHERE p.discountPrice IS NOT NULL AND p.discountPrice < p.price AND p.active = true")
    Page<Product> findProductsWithDiscount(Pageable pageable);

    // Find best sellers (by sold count)
    List<Product> findTop10ByActiveTrueOrderBySoldCountDesc();

    // Find most viewed
    List<Product> findTop10ByActiveTrueOrderByViewCountDesc();

    // Check SKU exists
    boolean existsBySku(String sku);

    // Count by category
    @Query("SELECT COUNT(p) FROM Product p WHERE p.category.id = :categoryId AND p.active = true")
    long countByCategoryId(@Param("categoryId") Long categoryId);

    // Admin queries - include inactive products
    @Query("SELECT p FROM Product p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Product> adminSearchByName(@Param("keyword") String keyword, Pageable pageable);
}
