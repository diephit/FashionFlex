package g6.fashionFlex.repository;

import g6.fashionFlex.entity.PaymentHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentHistoryRepository extends JpaRepository<PaymentHistory, Integer> {
    
    List<PaymentHistory> findByPaymentPaymentID(Integer paymentID);
}

