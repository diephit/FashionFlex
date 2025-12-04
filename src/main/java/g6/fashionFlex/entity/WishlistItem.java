package g6.fashionFlex.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "wishlist_items")
public class WishlistItem {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer wishlistItemID;
    
    @ManyToOne
    @JoinColumn(name = "wishlistID", nullable = false)
    private Wishlist wishlist;
    
    @ManyToOne
    @JoinColumn(name = "productID", nullable = false)
    private Product product;
    
    @ManyToOne
    @JoinColumn(name = "variantID", nullable = true)
    private ProductVariant variant;
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime addedAt;
    
    @PrePersist
    protected void onCreate() {
        addedAt = LocalDateTime.now();
    }

    // Getters & Setters
    public Integer getWishlistItemID() {
        return wishlistItemID;
    }

    public void setWishlistItemID(Integer wishlistItemID) {
        this.wishlistItemID = wishlistItemID;
    }

    public Wishlist getWishlist() {
        return wishlist;
    }

    public void setWishlist(Wishlist wishlist) {
        this.wishlist = wishlist;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public ProductVariant getVariant() {
        return variant;
    }

    public void setVariant(ProductVariant variant) {
        this.variant = variant;
    }

    public LocalDateTime getAddedAt() {
        return addedAt;
    }

    public void setAddedAt(LocalDateTime addedAt) {
        this.addedAt = addedAt;
    }
}