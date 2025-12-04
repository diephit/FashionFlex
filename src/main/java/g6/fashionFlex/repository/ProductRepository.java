package g6.fashionFlex.repository;

import g6.fashionFlex.entity.Product;
import g6.fashionFlex.entity.Product.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Integer> {
    
    Page<Product> findAll(Pageable pageable);
    
    Page<Product> findByStatus(ProductStatus status, Pageable pageable);
    
    @Query("SELECT p FROM Product p WHERE " +
           "LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Product> searchProducts(@Param("keyword") String keyword, Pageable pageable);
    
    @Query("SELECT p FROM Product p WHERE " +
           "(LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "p.status = :status")
    Page<Product> searchProductsByStatus(@Param("keyword") String keyword, 
                                         @Param("status") ProductStatus status, 
                                         Pageable pageable);
    
    @Query("SELECT p FROM Product p " +
           "WHERE p.status = g6.fashionFlex.entity.Product.ProductStatus.active " +
           "AND (LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Product> searchActiveProducts(@Param("keyword") String keyword);
    
    List<Product> findByCategoryCategoryID(Integer categoryID);
    
    List<Product> findByStatus(ProductStatus status);

    List<Product> findByCategoryCategoryIDIn(Collection<Integer> categoryIds);

    long countByStatus(ProductStatus status);

    @Query("SELECT p FROM Product p " +
           "LEFT JOIN p.category c " +
           "LEFT JOIN c.parentCategory cp " +
           "LEFT JOIN cp.parentCategory cpp " +
           "WHERE (c.categoryID = :topId OR cp.categoryID = :topId OR cpp.categoryID = :topId) " +
           "AND p.status = g6.fashionFlex.entity.Product.ProductStatus.active " +
           "AND (c.status = g6.fashionFlex.entity.Category.CategoryStatus.active OR c.status IS NULL) " +
           "AND (cp.status = g6.fashionFlex.entity.Category.CategoryStatus.active OR cp.status IS NULL) " +
           "AND (cpp.status = g6.fashionFlex.entity.Category.CategoryStatus.active OR cpp.status IS NULL)")
    List<Product> findByTopLevelCategory(@Param("topId") Integer topCategoryId);

    @Query("SELECT p FROM Product p " +
           "LEFT JOIN p.category c " +
           "LEFT JOIN c.parentCategory cp " +
           "LEFT JOIN cp.parentCategory cpp " +
           "WHERE (c.categoryID IN :topIds OR cp.categoryID IN :topIds OR cpp.categoryID IN :topIds) " +
           "AND p.status = g6.fashionFlex.entity.Product.ProductStatus.active " +
           "AND (c.status = g6.fashionFlex.entity.Category.CategoryStatus.active OR c.status IS NULL) " +
           "AND (cp.status = g6.fashionFlex.entity.Category.CategoryStatus.active OR cp.status IS NULL) " +
           "AND (cpp.status = g6.fashionFlex.entity.Category.CategoryStatus.active OR cpp.status IS NULL)")
    List<Product> findByTopLevelCategories(@Param("topIds") List<Integer> topCategoryIds);

    @Query("SELECT p FROM Product p WHERE p.status = g6.fashionFlex.entity.Product.ProductStatus.active")
    List<Product> findAllActive();
}

