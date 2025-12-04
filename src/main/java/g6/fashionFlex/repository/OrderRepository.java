package g6.fashionFlex.repository;

import g6.fashionFlex.entity.Order;
import g6.fashionFlex.entity.Order.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import g6.fashionFlex.entity.User;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Integer> {
    
    Page<Order> findAll(Pageable pageable);
    
    Page<Order> findByStatus(OrderStatus status, Pageable pageable);
    
    @Query("""
            SELECT o FROM Order o
            LEFT JOIN o.customer cust
            LEFT JOIN cust.user u
            WHERE CAST(o.orderID AS string) LIKE CONCAT('%', :keyword, '%')
               OR LOWER(COALESCE(u.name, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(COALESCE(u.email, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
            """)
    Page<Order> searchOrders(@Param("keyword") String keyword, Pageable pageable);
    
    @Query("""
            SELECT o FROM Order o
            LEFT JOIN o.customer cust
            LEFT JOIN cust.user u
            WHERE (CAST(o.orderID AS string) LIKE CONCAT('%', :keyword, '%')
               OR LOWER(COALESCE(u.name, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(COALESCE(u.email, '')) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND o.status = :status
            """)
    Page<Order> searchOrdersByStatus(@Param("keyword") String keyword, 
                                     @Param("status") OrderStatus status, 
                                     Pageable pageable);
    
    List<Order> findByOrderDateBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    List<Order> findByStatusAndOrderDateBetween(OrderStatus status, 
                                                 LocalDateTime startDate, 
                                                 LocalDateTime endDate);
    
    @Query("SELECT o FROM Order o ORDER BY o.orderDate DESC")
    List<Order> findRecentOrders(Pageable pageable);

    long countByStatus(OrderStatus status);

    @Query("""
            SELECT DISTINCT o FROM Order o
            LEFT JOIN FETCH o.orderItems items
            LEFT JOIN FETCH items.variant variant
            LEFT JOIN FETCH variant.product product
            WHERE o.customer.customerID = :customerId
            ORDER BY o.orderDate DESC
            """)
    List<Order> findOrdersWithItemsByCustomer(@Param("customerId") Integer customerId);
    
    @Query("""
            SELECT DISTINCT o FROM Order o
            LEFT JOIN FETCH o.orderItems items
            LEFT JOIN FETCH items.variant variant
            LEFT JOIN FETCH variant.product product
            WHERE (o.user.userID = :userId OR (o.customer IS NOT NULL AND o.customer.user.userID = :userId))
            ORDER BY o.orderDate DESC
            """)
    List<Order> findByUserOrderByOrderDateDesc(@Param("userId") Integer userId);
    
    @Query("""
            SELECT DISTINCT o FROM Order o
            LEFT JOIN FETCH o.orderItems items
            LEFT JOIN FETCH items.variant variant
            LEFT JOIN FETCH variant.product product
            WHERE LOWER(o.contactEmail) = LOWER(:email)
            ORDER BY o.orderDate DESC
            """)
    List<Order> findByContactEmailWithItems(@Param("email") String email);
    
    List<Order> findByUser(User user);
}