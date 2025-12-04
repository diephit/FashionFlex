package g6.fashionFlex.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import g6.fashionFlex.entity.Product;
import g6.fashionFlex.entity.Wishlist;
import g6.fashionFlex.entity.WishlistItem;

@Repository
public interface WishlistItemRepository extends JpaRepository<WishlistItem, Integer> {
    
    List<WishlistItem> findByWishlist(Wishlist wishlist);
    
    @Query("SELECT wi FROM WishlistItem wi WHERE wi.wishlist = :wishlist AND wi.product = :product")
    Optional<WishlistItem> findByWishlistAndProduct(@Param("wishlist") Wishlist wishlist, @Param("product") Product product);
    
    void deleteByWishlistAndProduct(Wishlist wishlist, Product product);
    
    int countByWishlist(Wishlist wishlist);
    
    @Query("SELECT wi FROM WishlistItem wi WHERE wi.variant.variantID = :variantId")
    List<WishlistItem> findByVariantVariantID(@Param("variantId") Integer variantId);
    
    @Query("SELECT COUNT(wi) FROM WishlistItem wi WHERE wi.variant.variantID = :variantId")
    long countByVariantVariantID(@Param("variantId") Integer variantId);
    
    @Query("SELECT wi FROM WishlistItem wi WHERE wi.product.productID = :productId")
    List<WishlistItem> findByProductProductID(@Param("productId") Integer productId);
    
    @Query("SELECT COUNT(wi) FROM WishlistItem wi WHERE wi.product.productID = :productId")
    long countByProductProductID(@Param("productId") Integer productId);
}