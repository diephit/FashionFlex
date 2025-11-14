package g6.fashionFlex.repository;

import g6.fashionFlex.entity.Cart;
import g6.fashionFlex.entity.CartItem;
import g6.fashionFlex.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Integer> {
    List<CartItem> findByCart(Cart cart);
    Optional<CartItem> findByCartAndVariant(Cart cart, ProductVariant variant);
}


