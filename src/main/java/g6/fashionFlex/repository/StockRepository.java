package g6.fashionFlex.repository;

import g6.fashionFlex.entity.Stock;
import g6.fashionFlex.entity.Stock.ChangeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StockRepository extends JpaRepository<Stock, Integer> {
    
    List<Stock> findByVariantVariantID(Integer variantID);
    
    List<Stock> findByChangeType(ChangeType changeType);
    
    @Query("SELECT s FROM Stock s WHERE s.variant.variantID = :variantID " +
           "ORDER BY s.updatedAt DESC")
    List<Stock> findByVariantOrderByUpdatedAtDesc(@Param("variantID") Integer variantID);
    
    @Query("SELECT s FROM Stock s WHERE s.updatedAt BETWEEN :startDate AND :endDate")
    List<Stock> findByUpdatedAtBetween(@Param("startDate") LocalDateTime startDate, 
                                       @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT s FROM Stock s WHERE s.variant.variantID = :variantID " +
           "ORDER BY s.updatedAt DESC")
    Stock findLatestByVariant(@Param("variantID") Integer variantID);
    
    Page<Stock> findAll(Pageable pageable);
    
    @Query("SELECT s FROM Stock s WHERE s.currentQuantity <= :threshold " +
           "ORDER BY s.currentQuantity ASC")
    List<Stock> findLowStockVariants(@Param("threshold") Integer threshold);
}

