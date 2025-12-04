package g6.fashionFlex.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import g6.fashionFlex.entity.Order;
import g6.fashionFlex.entity.OrderItem;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Integer> {
    List<OrderItem> findByOrder(Order order);
    
    @Query("SELECT oi FROM OrderItem oi WHERE oi.variant.variantID = :variantId")
    List<OrderItem> findByVariantVariantID(@Param("variantId") Integer variantId);
    
    @Query("SELECT COUNT(oi) FROM OrderItem oi WHERE oi.variant.variantID = :variantId")
    long countByVariantVariantID(@Param("variantId") Integer variantId);
}