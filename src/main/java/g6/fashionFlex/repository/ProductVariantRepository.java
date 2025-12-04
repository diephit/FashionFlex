package g6.fashionFlex.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import g6.fashionFlex.entity.ProductVariant;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Integer> {
    
    List<ProductVariant> findByProductProductID(Integer productID);

    List<ProductVariant> findByProductProductIDAndStatus(Integer productID, ProductVariant.VariantStatus status);
    
    Optional<ProductVariant> findBySku(String sku);
    
    Boolean existsBySku(String sku);
}

