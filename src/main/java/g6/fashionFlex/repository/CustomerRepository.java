package g6.fashionFlex.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import g6.fashionFlex.entity.Customer;
import g6.fashionFlex.entity.User;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Integer> {

    Optional<Customer> findByUser(User user);
}
