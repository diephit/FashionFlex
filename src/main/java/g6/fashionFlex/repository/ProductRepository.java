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

    // Find by active status with JOIN FETCH to avoid N+1 problem
    @Query("SELECT DISTINCT p FROM Product p LEFT JOIN FETCH p.category LEFT JOIN FETCH p.brand WHERE p.active = true AND p.category.active = true")
    List<Product> findByActiveTrue();

    @Query(value = "SELECT DISTINCT p FROM Product p LEFT JOIN FETCH p.category LEFT JOIN FETCH p.brand WHERE p.active = true AND p.category.active = true",
           countQuery = "SELECT COUNT(p) FROM Product p WHERE p.active = true AND p.category.active = true")
    Page<Product> findByActiveTrue(Pageable pageable);

    // Find by category with JOIN FETCH
    @Query(value = "SELECT DISTINCT p FROM Product p LEFT JOIN FETCH p.category LEFT JOIN FETCH p.brand WHERE p.category.id = :categoryId AND p.active = true AND p.category.active = true",
           countQuery = "SELECT COUNT(p) FROM Product p WHERE p.category.id = :categoryId AND p.active = true AND p.category.active = true")
    Page<Product> findByCategoryIdAndActiveTrue(@Param("categoryId") Long categoryId, Pageable pageable);

    // Find by category name with JOIN FETCH
    @Query(value = "SELECT DISTINCT p FROM Product p LEFT JOIN FETCH p.category LEFT JOIN FETCH p.brand WHERE LOWER(p.category.name) = LOWER(:categoryName) AND p.active = true AND p.category.active = true",
           countQuery = "SELECT COUNT(p) FROM Product p WHERE LOWER(p.category.name) = LOWER(:categoryName) AND p.active = true AND p.category.active = true")
    Page<Product> findByCategoryNameAndActiveTrue(@Param("categoryName") String categoryName, Pageable pageable);

    // Find by brand with JOIN FETCH
    @Query(value = "SELECT DISTINCT p FROM Product p LEFT JOIN FETCH p.category LEFT JOIN FETCH p.brand WHERE p.brand.id = :brandId AND p.active = true AND p.category.active = true",
           countQuery = "SELECT COUNT(p) FROM Product p WHERE p.brand.id = :brandId AND p.active = true AND p.category.active = true")
    Page<Product> findByBrandIdAndActiveTrue(@Param("brandId") Long brandId, Pageable pageable);

    // Find featured products with JOIN FETCH
    @Query("SELECT DISTINCT p FROM Product p LEFT JOIN FETCH p.category LEFT JOIN FETCH p.brand WHERE p.featured = true AND p.active = true AND p.category.active = true ORDER BY p.createdAt DESC")
    List<Product> findByFeaturedTrueAndActiveTrueOrderByCreatedAtDesc();

    @Query(value = "SELECT DISTINCT p FROM Product p LEFT JOIN FETCH p.category LEFT JOIN FETCH p.brand WHERE p.featured = true AND p.active = true AND p.category.active = true",
           countQuery = "SELECT COUNT(p) FROM Product p WHERE p.featured = true AND p.active = true AND p.category.active = true")
    Page<Product> findByFeaturedTrueAndActiveTrue(Pageable pageable);

    // Search by name with JOIN FETCH
    @Query(value = "SELECT DISTINCT p FROM Product p LEFT JOIN FETCH p.category LEFT JOIN FETCH p.brand WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) AND p.active = true AND p.category.active = true",
           countQuery = "SELECT COUNT(p) FROM Product p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) AND p.active = true AND p.category.active = true")
    Page<Product> searchByName(@Param("keyword") String keyword, Pageable pageable);

    // Find by price range with JOIN FETCH
    @Query(value = "SELECT DISTINCT p FROM Product p LEFT JOIN FETCH p.category LEFT JOIN FETCH p.brand WHERE p.price BETWEEN :minPrice AND :maxPrice AND p.active = true AND p.category.active = true",
           countQuery = "SELECT COUNT(p) FROM Product p WHERE p.price BETWEEN :minPrice AND :maxPrice AND p.active = true AND p.category.active = true")
    Page<Product> findByPriceBetweenAndActiveTrue(@Param("minPrice") BigDecimal minPrice, @Param("maxPrice") BigDecimal maxPrice, Pageable pageable);

    // Find products with discount with JOIN FETCH
    @Query(value = "SELECT DISTINCT p FROM Product p LEFT JOIN FETCH p.category LEFT JOIN FETCH p.brand WHERE p.discountPrice IS NOT NULL AND p.discountPrice < p.price AND p.active = true AND p.category.active = true",
           countQuery = "SELECT COUNT(p) FROM Product p WHERE p.discountPrice IS NOT NULL AND p.discountPrice < p.price AND p.active = true AND p.category.active = true")
    Page<Product> findProductsWithDiscount(Pageable pageable);

    // Find best sellers (by sold count) with JOIN FETCH
    @Query("SELECT DISTINCT p FROM Product p LEFT JOIN FETCH p.category LEFT JOIN FETCH p.brand WHERE p.active = true AND p.category.active = true ORDER BY p.soldCount DESC")
    List<Product> findTop10ByActiveTrueOrderBySoldCountDesc();

    // Find most viewed with JOIN FETCH
    @Query("SELECT DISTINCT p FROM Product p LEFT JOIN FETCH p.category LEFT JOIN FETCH p.brand WHERE p.active = true AND p.category.active = true ORDER BY p.viewCount DESC")
    List<Product> findTop10ByActiveTrueOrderByViewCountDesc();

    // Check SKU exists
    boolean existsBySku(String sku);

    // Count by category
    @Query("SELECT COUNT(p) FROM Product p WHERE p.category.id = :categoryId AND p.active = true AND p.category.active = true")
    long countByCategoryId(@Param("categoryId") Long categoryId);

    // Count by brand
    @Query("SELECT COUNT(p) FROM Product p WHERE p.brand.id = :brandId")
    long countByBrandId(@Param("brandId") Long brandId);

    // Admin queries - include inactive products
    @Query("SELECT p FROM Product p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Product> adminSearchByName(@Param("keyword") String keyword, Pageable pageable);
}
