package g6.fashionFlex.repository;

import g6.fashionFlex.entity.Order;
import g6.fashionFlex.entity.Order.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Integer> {
    
    Page<Order> findAll(Pageable pageable);
    
    Page<Order> findByStatus(OrderStatus status, Pageable pageable);
    
    @Query("SELECT o FROM Order o WHERE " +
           "CAST(o.orderID AS string) LIKE CONCAT('%', :keyword, '%') OR " +
           "LOWER(o.customer.user.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(o.customer.user.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(o.trackingNumber) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Order> searchOrders(@Param("keyword") String keyword, Pageable pageable);
    
    @Query("SELECT o FROM Order o WHERE " +
           "(CAST(o.orderID AS string) LIKE CONCAT('%', :keyword, '%') OR " +
           "LOWER(o.customer.user.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(o.customer.user.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(o.trackingNumber) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "o.status = :status")
    Page<Order> searchOrdersByStatus(@Param("keyword") String keyword, 
                                     @Param("status") OrderStatus status, 
                                     Pageable pageable);
    
    List<Order> findByOrderDateBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    List<Order> findByStatusAndOrderDateBetween(OrderStatus status, 
                                                 LocalDateTime startDate, 
                                                 LocalDateTime endDate);
}

