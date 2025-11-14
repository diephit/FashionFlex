package g6.fashionFlex.repository;

import g6.fashionFlex.entity.Category;
import g6.fashionFlex.entity.Category.CategoryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Integer> {
    
    List<Category> findByParentCategoryIsNull();
    
    List<Category> findByParentCategoryCategoryID(Integer parentCategoryID);
    
    List<Category> findByStatus(CategoryStatus status);
    
    Optional<Category> findByName(String name);
    
    @Query("SELECT c FROM Category c WHERE " +
           "LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Category> searchCategories(@Param("keyword") String keyword);
}

