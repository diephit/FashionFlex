package g6.fashionFlex.service;

import g6.fashionFlex.dto.AddToCartRequest;
import g6.fashionFlex.dto.CartDTO;
import g6.fashionFlex.dto.MoveToCartRequest;
import g6.fashionFlex.dto.WishlistDTO;
import g6.fashionFlex.entity.Product;
import g6.fashionFlex.entity.User;
import g6.fashionFlex.entity.Wishlist;
import g6.fashionFlex.exception.ResourceNotFoundException;
import g6.fashionFlex.repository.ProductRepository;
import g6.fashionFlex.repository.UserRepository;
import g6.fashionFlex.repository.WishlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final CartService cartService;

    private static final int MAX_WISHLIST_ITEMS = 20;

    /**
     * Get all wishlist items for user
     */
    @Transactional(readOnly = true)
    public List<WishlistDTO> getWishlistByUserId(Long userId) {
        List<Wishlist> wishlists = wishlistRepository.findByUserIdWithProduct(userId);
        return wishlists.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Add product to wishlist
     */
    public WishlistDTO addToWishlist(Long userId, Long productId) {
        // Check if already in wishlist
        if (wishlistRepository.existsByUserIdAndProductId(userId, productId)) {
            throw new IllegalArgumentException("Product is already in your wishlist");
        }

        // Check wishlist size limit
        long currentCount = wishlistRepository.countByUserId(userId);
        if (currentCount >= MAX_WISHLIST_ITEMS) {
            throw new IllegalArgumentException("Wishlist is full. Maximum " + MAX_WISHLIST_ITEMS + " items allowed");
        }

        // Get user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        // Get product
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        // Validate product is active
        if (!product.getActive()) {
            throw new IllegalArgumentException("Product is no longer available");
        }

        // Create wishlist item
        Wishlist wishlist = new Wishlist();
        wishlist.setUser(user);
        wishlist.setProduct(product);

        Wishlist savedWishlist = wishlistRepository.save(wishlist);
        return convertToDTO(savedWishlist);
    }

    /**
     * Remove product from wishlist
     */
    public void removeFromWishlist(Long userId, Long wishlistId) {
        Wishlist wishlist = wishlistRepository.findById(wishlistId)
                .orElseThrow(() -> new ResourceNotFoundException("Wishlist item not found with id: " + wishlistId));

        // Verify wishlist item belongs to user
        if (!wishlist.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Wishlist item does not belong to this user");
        }

        wishlistRepository.delete(wishlist);
    }

    /**
     * Remove product from wishlist by product ID
     */
    public void removeFromWishlistByProductId(Long userId, Long productId) {
        wishlistRepository.deleteByUserIdAndProductId(userId, productId);
    }

    /**
     * Check if product is in wishlist
     */
    @Transactional(readOnly = true)
    public boolean isInWishlist(Long userId, Long productId) {
        return wishlistRepository.existsByUserIdAndProductId(userId, productId);
    }

    /**
     * Get wishlist count for user
     */
    @Transactional(readOnly = true)
    public long getWishlistCount(Long userId) {
        return wishlistRepository.countByUserId(userId);
    }

    /**
     * Clear all wishlist items for user
     */
    public void clearWishlist(Long userId) {
        List<Wishlist> wishlists = wishlistRepository.findByUserIdOrderByAddedAtDesc(userId);
        wishlistRepository.deleteAll(wishlists);
    }

    /**
     * Move wishlist item to cart
     */
    public CartDTO moveToCart(Long userId, MoveToCartRequest request) {
        // Get wishlist item
        Wishlist wishlist = wishlistRepository.findById(request.getWishlistId())
                .orElseThrow(() -> new ResourceNotFoundException("Wishlist item not found with id: " + request.getWishlistId()));

        // Verify wishlist item belongs to user
        if (!wishlist.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Wishlist item does not belong to this user");
        }

        Product product = wishlist.getProduct();

        // Validate product is still active and in stock
        if (!product.getActive()) {
            throw new IllegalArgumentException("Product is no longer available");
        }

        if (product.getStock() < request.getQuantity()) {
            throw new IllegalArgumentException("Insufficient stock. Only " + product.getStock() + " items available");
        }

        // Validate variants if product has them
        if (!product.getAvailableSizes().isEmpty() && (request.getSize() == null || request.getSize().trim().isEmpty())) {
            throw new IllegalArgumentException("Please select a size");
        }

        if (!product.getAvailableColors().isEmpty() && (request.getColor() == null || request.getColor().trim().isEmpty())) {
            throw new IllegalArgumentException("Please select a color");
        }

        // Validate selected size and color are valid
        if (request.getSize() != null && !request.getSize().trim().isEmpty()
                && !product.getAvailableSizes().isEmpty()
                && !product.getAvailableSizes().contains(request.getSize())) {
            throw new IllegalArgumentException("Selected size is not available for this product");
        }

        if (request.getColor() != null && !request.getColor().trim().isEmpty()
                && !product.getAvailableColors().isEmpty()
                && !product.getAvailableColors().contains(request.getColor())) {
            throw new IllegalArgumentException("Selected color is not available for this product");
        }

        // Create add to cart request
        AddToCartRequest addToCartRequest = new AddToCartRequest();
        addToCartRequest.setProductId(product.getId());
        addToCartRequest.setQuantity(request.getQuantity());
        addToCartRequest.setSize(request.getSize());
        addToCartRequest.setColor(request.getColor());

        // Add to cart
        CartDTO cartDTO = cartService.addToCart(userId, addToCartRequest);

        // Remove from wishlist after successful add to cart
        wishlistRepository.delete(wishlist);

        return cartDTO;
    }

    /**
     * Convert Wishlist entity to DTO
     */
    private WishlistDTO convertToDTO(Wishlist wishlist) {
        WishlistDTO dto = new WishlistDTO();
        dto.setId(wishlist.getId());
        dto.setUserId(wishlist.getUser().getId());
        dto.setAddedAt(wishlist.getAddedAt());

        Product product = wishlist.getProduct();
        dto.setProductId(product.getId());
        dto.setProductName(product.getName());
        dto.setProductImageUrl(product.getImageUrl());
        dto.setProductSku(product.getSku());
        dto.setPrice(product.getPrice());
        dto.setDiscountPrice(product.getDiscountPrice());
        dto.setEffectivePrice(product.getEffectivePrice());
        dto.setStock(product.getStock());
        dto.setInStock(product.getStock() > 0);
        dto.setHasDiscount(product.hasDiscount());
        dto.setDiscountPercentage(product.getDiscountPercentage());
        dto.setAvailableSizes(product.getAvailableSizes());
        dto.setAvailableColors(product.getAvailableColors());
        dto.setHasVariants(!product.getAvailableSizes().isEmpty() || !product.getAvailableColors().isEmpty());

        // Get category and brand names if available
        if (product.getCategory() != null) {
            dto.setCategoryName(product.getCategory().getName());
        }
        if (product.getBrand() != null) {
            dto.setBrandName(product.getBrand().getName());
        }

        return dto;
    }
}
