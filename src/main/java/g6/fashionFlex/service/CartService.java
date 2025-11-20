package g6.fashionFlex.service;

import g6.fashionFlex.dto.*;
import g6.fashionFlex.entity.*;
import g6.fashionFlex.exception.ResourceNotFoundException;
import g6.fashionFlex.repository.CartItemRepository;
import g6.fashionFlex.repository.CartRepository;
import g6.fashionFlex.repository.ProductRepository;
import g6.fashionFlex.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final CouponService couponService; // Injected CouponService

    /**
     * Get or create cart for user
     */
    public Cart getOrCreateCart(Long userId) {
        return cartRepository.findByUserId(userId)
                .orElseGet(() -> createNewCart(userId));
    }

    /**
     * Create new cart for user
     */
    private Cart createNewCart(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Cart cart = new Cart();
        cart.setUser(user);
        return cartRepository.save(cart);
    }

    /**
     * Get cart with items for user
     */
    @Transactional
    public CartDTO getCartByUserId(Long userId) {
        Cart cart = getOrCreateCart(userId);
        return convertToDTO(cart);
    }

    /**
     * Add item to cart
     */
    public CartDTO addToCart(Long userId, AddToCartRequest request) {
        Cart cart = getOrCreateCart(userId);
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + request.getProductId()));

        if (request.getSize() == null || request.getSize().isBlank()) {
            throw new IllegalArgumentException("Size is required");
        }
        if (request.getColor() == null || request.getColor().isBlank()) {
            throw new IllegalArgumentException("Color is required");
        }

        // Validate product is active
        if (!product.getActive()) {
            throw new IllegalArgumentException("Product is no longer available");
        }

        // Validate stock availability
        if (product.getStock() < request.getQuantity()) {
            throw new IllegalArgumentException("Insufficient stock. Only " + product.getStock() + " items available");
        }

        // Check if item with same product, size, and color already exists
        CartItem existingItem = cartItemRepository.findByCartAndProductAndSizeAndColor(
                cart.getId(),
                product.getId(),
                request.getSize(),
                request.getColor()
        ).orElse(null);

        if (existingItem != null) {
            // Update existing item quantity
            int newQuantity = existingItem.getQuantity() + request.getQuantity();

            // Validate total quantity against stock
            if (product.getStock() < newQuantity) {
                throw new IllegalArgumentException("Insufficient stock. Only " + product.getStock() + " items available");
            }

            existingItem.setQuantity(newQuantity);
            cartItemRepository.save(existingItem);
            cart = cartRepository.save(cart);
        } else {
            // Create new cart item
            CartItem cartItem = new CartItem();
            cartItem.setCart(cart);
            cartItem.setProduct(product);
            cartItem.setQuantity(request.getQuantity());
            cartItem.setSize(request.getSize());
            cartItem.setColor(request.getColor());
            cartItem.setPrice(product.getPrice());
            cartItem.setDiscountPrice(product.getDiscountPrice());

            // First save the cart to ensure it has an ID
            cart = cartRepository.save(cart);

            // Then add item and save it
            cart.addItem(cartItem);
            cartItemRepository.save(cartItem);

            // Save cart again to update the relationship
            cart = cartRepository.save(cart);
        }

        return convertToDTO(cart);
    }

    /**
     * Update cart item quantity
     */
    public CartDTO updateCartItem(Long userId, UpdateCartItemRequest request) {
        Cart cart = getOrCreateCart(userId);

        CartItem cartItem = cartItemRepository.findById(request.getCartItemId())
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found with id: " + request.getCartItemId()));

        // Verify cart item belongs to user's cart
        if (!cartItem.getCart().getId().equals(cart.getId())) {
            throw new IllegalArgumentException("Cart item does not belong to user's cart");
        }

        // Validate stock availability
        Product product = cartItem.getProduct();
        if (product.getStock() < request.getQuantity()) {
            throw new IllegalArgumentException("Insufficient stock. Only " + product.getStock() + " items available");
        }

        // Update quantity
        cartItem.setQuantity(request.getQuantity());
        cartItemRepository.save(cartItem);

        // Save cart to ensure updated_at timestamp is updated
        cart = cartRepository.save(cart);

        return convertToDTO(cart);
    }

    /**
     * Remove item from cart
     */
    public CartDTO removeCartItem(Long userId, Long cartItemId) {
        Cart cart = getOrCreateCart(userId);

        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found with id: " + cartItemId));

        // Verify cart item belongs to user's cart
        if (!cartItem.getCart().getId().equals(cart.getId())) {
            throw new IllegalArgumentException("Cart item does not belong to user's cart");
        }

        cart.removeItem(cartItem);
        cartItemRepository.delete(cartItem);

        return convertToDTO(cart);
    }

    /**
     * Clear all items from cart
     */
    @Transactional
    public void clearCart(Long userId) {
        log.info("Clearing cart for user: {}", userId);
        Cart cart = getOrCreateCart(userId);
        int itemCount = cart.getItems().size();
        log.info("Found cart with {} items", itemCount);

        // Clear items from collection first
        cart.clearItems();

        // Delete from database
        cartItemRepository.deleteByCartId(cart.getId());
        log.info("Deleted {} cart items from database", itemCount);

        // Remove coupon and shipping
        cart.removeCoupon();
        cart.clearShippingSelection();

        // Save cart
        cartRepository.save(cart);
        log.info("Cart cleared and saved successfully");
    }

    /**
     * Apply coupon to cart, calculate discount, and save.
     */
    public CartDTO applyCoupon(Long userId, String couponCode) {
        Cart cart = getOrCreateCart(userId);
        BigDecimal subtotal = cart.calculateSubtotal();

        // Validate and calculate discount using CouponService
        BigDecimal discountAmount = couponService.calculateDiscount(couponCode, subtotal);

        // Apply coupon code and calculated discount to the cart
        cart.applyCoupon(couponCode, discountAmount);
        cartRepository.save(cart);

        return convertToDTO(cart);
    }

    /**
     * Remove coupon from cart
     */
    public CartDTO removeCoupon(Long userId) {
        Cart cart = getOrCreateCart(userId);
        cart.removeCoupon();
        cartRepository.save(cart);
        return convertToDTO(cart);
    }

    /**
     * Validate cart items stock before checkout
     */
    public List<String> validateCartStock(Long userId) {
        Cart cart = getOrCreateCart(userId);
        List<String> warnings = new ArrayList<>();

        for (CartItem item : cart.getItems()) {
            Product product = item.getProduct();

            // Check if product is still active
            if (!product.getActive()) {
                warnings.add(product.getName() + " is no longer available and has been removed from your cart");
                cart.removeItem(item);
                cartItemRepository.delete(item);
                continue;
            }

            // Check stock availability
            if (product.getStock() < item.getQuantity()) {
                if (product.getStock() == 0) {
                    warnings.add(product.getName() + " is out of stock and has been removed from your cart");
                    cart.removeItem(item);
                    cartItemRepository.delete(item);
                } else {
                    warnings.add(product.getName() + " quantity has been reduced from " +
                               item.getQuantity() + " to " + product.getStock() + " due to limited stock");
                    item.setQuantity(product.getStock());
                    cartItemRepository.save(item);
                }
            }

            // Check if price has changed
            if (item.getPrice().compareTo(product.getPrice()) != 0) {
                warnings.add("The price of " + product.getName() + " has been updated");
                item.setPrice(product.getPrice());
                item.setDiscountPrice(product.getDiscountPrice());
                cartItemRepository.save(item);
            }
        }

        if (!warnings.isEmpty()) {
            cartRepository.save(cart);
        }

        return warnings;
    }

    /**
     * Get cart item count for user
     */
    @Transactional(readOnly = true)
    public int getCartItemCount(Long userId) {
        Cart cart = cartRepository.findByUserId(userId).orElse(null);
        if (cart == null) {
            return 0;
        }
        return cart.getTotalItems();
    }

    /**
     * Validate cart is ready for checkout
     * This method should be called before proceeding to checkout to ensure:
     * 1. Cart is not empty
     * 2. Shipping method is selected
     * 3. All items are in stock
     * 4. Coupon (if applied) is still valid
     * 5. Prices haven't changed significantly
     *
     * @param userId User ID
     * @throws IllegalStateException if cart is not ready for checkout
     */
    public void validateCheckoutReady(Long userId) {
        Cart cart = getOrCreateCart(userId);

        // 1. Check cart is not empty
        if (cart.getItems().isEmpty()) {
            throw new IllegalStateException("Cart is empty. Please add items before checkout.");
        }

        // 2. Check shipping method is selected
        if (!cart.hasShippingMethod()) {
            throw new IllegalStateException("Please select a shipping method before checkout.");
        }

        // 3. Validate stock and prices (this will auto-update cart if needed)
        List<String> stockWarnings = validateCartStock(userId);
        if (!stockWarnings.isEmpty()) {
            // If there were stock issues, throw exception with details
            String warningMessage = "Cart has been updated due to stock changes: " +
                                   String.join("; ", stockWarnings);
            throw new IllegalStateException(warningMessage);
        }

        // 4. Re-validate coupon if applied
        if (cart.getCouponCode() != null && cart.getDiscountAmount() != null) {
            // This will be handled by CouponService at checkout time
            // For now, just verify the coupon code exists
            // The actual discount validation happens during order creation
        }

        // All validations passed
    }

    /**
     * Select shipping method and calculate tax
     */
    public CartDTO selectShippingMethod(Long userId, String shippingMethod, BigDecimal shippingCost,
                                       String country, String state, String postcode, BigDecimal tax) {
        Cart cart = getOrCreateCart(userId);

        // Validate shipping cost is not negative
        if (shippingCost.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Shipping cost cannot be negative");
        }

        // Validate tax is not negative
        if (tax.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Tax amount cannot be negative");
        }

        // Select shipping method
        cart.selectShippingMethod(shippingMethod, shippingCost, tax, country, state, postcode);
        cartRepository.save(cart);

        return convertToDTO(cart);
    }

    /**
     * Clear shipping selection
     */
    public CartDTO clearShippingSelection(Long userId) {
        Cart cart = getOrCreateCart(userId);
        cart.clearShippingSelection();
        cartRepository.save(cart);
        return convertToDTO(cart);
    }

    /**
     * Convert Cart entity to DTO
     */
    private CartDTO convertToDTO(Cart cart) {
        CartDTO dto = new CartDTO();
        dto.setId(cart.getId());
        dto.setUserId(cart.getUser().getId());
        dto.setCouponCode(cart.getCouponCode());
        dto.setDiscountAmount(cart.getDiscountAmount());

        // Shipping fields
        dto.setSelectedShippingMethod(cart.getSelectedShippingMethod());
        dto.setShippingCost(cart.getShippingCost());
        dto.setTaxAmount(cart.getTaxAmount());
        dto.setShippingCountry(cart.getShippingCountry());
        dto.setShippingState(cart.getShippingState());
        dto.setShippingPostcode(cart.getShippingPostcode());
        dto.setHasShippingMethod(cart.hasShippingMethod());

        List<String> warnings = new ArrayList<>();
        List<CartItemDTO> itemDTOs = cart.getItems().stream()
                .map(item -> {
                    CartItemDTO itemDTO = convertItemToDTO(item);

                    // Check stock status
                    Product product = item.getProduct();
                    if (!product.getActive()) {
                        itemDTO.setInStock(false);
                        itemDTO.setStockMessage("Product no longer available");
                        warnings.add(product.getName() + " is no longer available");
                    } else if (product.getStock() < item.getQuantity()) {
                        itemDTO.setInStock(false);
                        itemDTO.setAvailableStock(product.getStock());
                        if (product.getStock() == 0) {
                            itemDTO.setStockMessage("Out of stock");
                            warnings.add(product.getName() + " is out of stock");
                        } else {
                            itemDTO.setStockMessage("Only " + product.getStock() + " available");
                            warnings.add(product.getName() + " has limited stock");
                        }
                    } else {
                        itemDTO.setInStock(true);
                        itemDTO.setAvailableStock(product.getStock());
                        itemDTO.setStockMessage("In stock");
                    }

                    return itemDTO;
                })
                .collect(Collectors.toList());

        dto.setItems(itemDTOs);
        dto.setSubtotal(cart.calculateSubtotal());
        dto.setTotal(cart.calculateTotal());
        dto.setFinalTotal(cart.calculateFinalTotal());
        dto.setTotalItems(cart.getTotalItems());
        dto.setHasOutOfStockItems(!warnings.isEmpty());
        dto.setStockWarnings(warnings);

        return dto;
    }

    /**
     * Convert CartItem entity to DTO
     */
    private CartItemDTO convertItemToDTO(CartItem item) {
        CartItemDTO dto = new CartItemDTO();
        dto.setId(item.getId());
        dto.setProductId(item.getProduct().getId());
        dto.setProductName(item.getProduct().getName());
        dto.setProductImageUrl(item.getProduct().getImageUrl());
        dto.setProductSku(item.getProduct().getSku());
        dto.setQuantity(item.getQuantity());
        dto.setSize(item.getSize());
        dto.setColor(item.getColor());
        dto.setPrice(item.getPrice());
        dto.setDiscountPrice(item.getDiscountPrice());
        dto.setEffectivePrice(item.getEffectivePrice());
        dto.setItemTotal(item.calculateItemTotal());
        dto.setSavings(item.getTotalSavings());
        return dto;
    }
}
