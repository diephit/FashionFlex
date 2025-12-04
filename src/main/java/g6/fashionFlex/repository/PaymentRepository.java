package g6.fashionFlex.repository;

import g6.fashionFlex.entity.Payment;
import g6.fashionFlex.entity.Payment.PaymentStatus;
import g6.fashionFlex.entity.Payment.PaymentMethod;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Integer> {
    
    Page<Payment> findAll(Pageable pageable);
    
    List<Payment> findByOrderOrderID(Integer orderID);
    
    List<Payment> findByStatus(PaymentStatus status);
    
    List<Payment> findByMethod(PaymentMethod method);
    
    @Query("SELECT p FROM Payment p WHERE " +
           "CAST(p.paymentID AS string) LIKE CONCAT('%', :keyword, '%') OR " +
           "LOWER(p.transactionCode) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "CAST(p.order.orderID AS string) LIKE CONCAT('%', :keyword, '%')")
    Page<Payment> searchPayments(@Param("keyword") String keyword, Pageable pageable);
    
    @Query("SELECT p FROM Payment p WHERE p.paymentDate BETWEEN :startDate AND :endDate")
    List<Payment> findByPaymentDateBetween(@Param("startDate") LocalDateTime startDate, 
                                          @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT p FROM Payment p WHERE p.status = :status AND " +
           "p.paymentDate BETWEEN :startDate AND :endDate")
    List<Payment> findByStatusAndPaymentDateBetween(@Param("status") PaymentStatus status,
                                                    @Param("startDate") LocalDateTime startDate,
                                                    @Param("endDate") LocalDateTime endDate);

    Optional<Payment> findTopByOrderOrderIDOrderByPaymentDateDesc(Integer orderID);
}

