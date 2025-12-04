package g6.fashionFlex.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import g6.fashionFlex.entity.Customer;
import g6.fashionFlex.entity.Wishlist;

public interface WishlistRepository extends JpaRepository<Wishlist, Integer> {
    Optional<Wishlist> findByCustomer(Customer customer);
}