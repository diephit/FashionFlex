package g6.fashionFlex.repository;

import g6.fashionFlex.entity.Admin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AdminRepository extends JpaRepository<Admin, Integer> {
    
    Optional<Admin> findByUserUserID(Integer userID);
    
    Optional<Admin> findByUserEmail(String email);
}

