package g6.fashionFlex.repository;

import g6.fashionFlex.entity.Role.RoleName;
import g6.fashionFlex.entity.User;
import g6.fashionFlex.entity.User.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    Optional<User> findByEmail(String email);

    Boolean existsByEmail(String email);

    long countByStatus(UserStatus status);

    long countByCreatedAtAfter(LocalDateTime createdAt);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.customer c LEFT JOIN FETCH c.level " +
           "WHERE u.role.roleName = :roleName AND c IS NOT NULL AND " +
           "(LOWER(u.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<User> searchCustomersList(@Param("keyword") String keyword, @Param("roleName") RoleName roleName);
    
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.customer c LEFT JOIN FETCH c.level " +
           "WHERE u.role.roleName = :roleName AND c IS NOT NULL")
    List<User> findAllCustomers(@Param("roleName") RoleName roleName);
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.role.roleName = :roleName AND u.customer IS NOT NULL")
    long countCustomers(@Param("roleName") RoleName roleName);
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.role.roleName = :roleName AND u.customer IS NOT NULL AND u.status = :status")
    long countCustomersByStatus(@Param("roleName") RoleName roleName, @Param("status") UserStatus status);
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.role.roleName = :roleName AND u.customer IS NOT NULL AND u.createdAt > :createdAt")
    long countNewCustomersAfter(@Param("roleName") RoleName roleName, @Param("createdAt") LocalDateTime createdAt);
}
