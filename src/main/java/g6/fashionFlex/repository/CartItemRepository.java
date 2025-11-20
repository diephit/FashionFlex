package g6.fashionFlex.repository;

import g6.fashionFlex.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    /**
     * Find all cart items by cart ID
     */
    List<CartItem> findByCartId(Long cartId);

    /**
     * Find cart item by cart, product, size, and color
     */
    @Query("SELECT ci FROM CartItem ci WHERE ci.cart.id = :cartId " +
           "AND ci.product.id = :productId " +
           "AND (:size IS NULL AND ci.size IS NULL OR ci.size = :size) " +
           "AND (:color IS NULL AND ci.color IS NULL OR ci.color = :color)")
    Optional<CartItem> findByCartAndProductAndSizeAndColor(
            @Param("cartId") Long cartId,
            @Param("productId") Long productId,
            @Param("size") String size,
            @Param("color") String color
    );

    /**
     * Find cart items by product ID
     */
    List<CartItem> findByProductId(Long productId);

    /**
     * Delete all cart items by cart ID
     */
    void deleteByCartId(Long cartId);

    /**
     * Count cart items by cart ID
     */
    long countByCartId(Long cartId);
}
