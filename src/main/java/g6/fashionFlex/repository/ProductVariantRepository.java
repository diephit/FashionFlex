package g6.fashionFlex.repository;

import g6.fashionFlex.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Integer> {
    
    List<ProductVariant> findByProductProductID(Integer productID);

    List<ProductVariant> findByProductProductIDAndStatus(Integer productID, ProductVariant.VariantStatus status);
    
    Optional<ProductVariant> findBySku(String sku);
    
    Boolean existsBySku(String sku);
}

