package g6.fashionFlex.repository;

import g6.fashionFlex.entity.Customer;
import g6.fashionFlex.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Integer> {
    Optional<Customer> findByUser(User user);
    Optional<Customer> findByPhone(String phone);
    boolean existsByPhone(String phone);
    boolean existsByPhoneAndCustomerIDNot(String phone, Integer customerID);
}