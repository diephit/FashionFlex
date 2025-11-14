package g6.fashionFlex.repository;

import g6.fashionFlex.entity.MembershipHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MembershipHistoryRepository extends JpaRepository<MembershipHistory, Integer> {
    
    List<MembershipHistory> findByCustomerCustomerID(Integer customerID);
    
    @Query("SELECT mh FROM MembershipHistory mh WHERE mh.customer.customerID = :customerID " +
           "ORDER BY mh.changedAt DESC")
    List<MembershipHistory> findByCustomerOrderByChangedAtDesc(@Param("customerID") Integer customerID);
    
    @Query("SELECT mh FROM MembershipHistory mh WHERE mh.changedAt BETWEEN :startDate AND :endDate")
    List<MembershipHistory> findByChangedAtBetween(@Param("startDate") LocalDateTime startDate,
                                                    @Param("endDate") LocalDateTime endDate);
    
    Page<MembershipHistory> findAll(Pageable pageable);
}

