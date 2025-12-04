package g6.fashionFlex.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import g6.fashionFlex.entity.Cart;
import g6.fashionFlex.entity.CartItem;
import g6.fashionFlex.entity.ProductVariant;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Integer> {
    List<CartItem> findByCart(Cart cart);
    Optional<CartItem> findByCartAndVariant(Cart cart, ProductVariant variant);
    List<CartItem> findByVariant(ProductVariant variant);
    
    @Query("SELECT ci FROM CartItem ci WHERE ci.variant.variantID = :variantId")
    List<CartItem> findByVariantVariantID(@Param("variantId") Integer variantId);
    
    @Query("SELECT COUNT(ci) FROM CartItem ci WHERE ci.variant.variantID = :variantId")
    long countByVariantVariantID(@Param("variantId") Integer variantId);
}