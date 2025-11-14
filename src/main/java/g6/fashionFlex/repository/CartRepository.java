package g6.fashionFlex.repository;

import g6.fashionFlex.entity.Cart;
import g6.fashionFlex.entity.Customer;
import g6.fashionFlex.entity.Cart.CartStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Integer> {
    Optional<Cart> findByCustomer(Customer customer);
    Optional<Cart> findBySessionIDAndStatus(String sessionID, CartStatus status);
}


