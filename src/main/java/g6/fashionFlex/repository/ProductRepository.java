package g6.fashionFlex.repository;

import g6.fashionFlex.entity.Product;
import g6.fashionFlex.entity.Product.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
    
    List<Product> findByCategoryCategoryID(Integer categoryID);
    
    List<Product> findByStatus(ProductStatus status);

    @Query("SELECT p FROM Product p " +
           "LEFT JOIN p.category c " +
           "LEFT JOIN c.parentCategory cp " +
           "LEFT JOIN cp.parentCategory cpp " +
           "WHERE c.categoryID = :topId OR cp.categoryID = :topId OR cpp.categoryID = :topId")
    List<Product> findByTopLevelCategory(@Param("topId") Integer topCategoryId);

    @Query("SELECT p FROM Product p " +
           "LEFT JOIN p.category c " +
           "LEFT JOIN c.parentCategory cp " +
           "LEFT JOIN cp.parentCategory cpp " +
           "WHERE c.categoryID IN :topIds OR cp.categoryID IN :topIds OR cpp.categoryID IN :topIds")
    List<Product> findByTopLevelCategories(@Param("topIds") List<Integer> topCategoryIds);
}

