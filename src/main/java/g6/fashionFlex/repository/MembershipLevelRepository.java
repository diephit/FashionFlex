package g6.fashionFlex.repository;

import g6.fashionFlex.entity.MembershipLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface MembershipLevelRepository extends JpaRepository<MembershipLevel, Integer> {
    
    Optional<MembershipLevel> findByLevelName(String levelName);
    
    @Query("SELECT ml FROM MembershipLevel ml WHERE ml.minSpent <= :totalSpent " +
           "ORDER BY ml.minSpent DESC")
    List<MembershipLevel> findEligibleLevels(@Param("totalSpent") BigDecimal totalSpent);
    
    @Query("SELECT ml FROM MembershipLevel ml ORDER BY ml.minSpent ASC")
    List<MembershipLevel> findAllOrderByMinSpentAsc();
}

