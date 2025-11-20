package g6.fashionFlex.repository;

import g6.fashionFlex.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AddressRepository extends JpaRepository<Address, Long> {
    List<Address> findByUserId(Long userId);
    long countByUserId(Long userId);
}