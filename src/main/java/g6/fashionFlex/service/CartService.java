package g6.fashionFlex.service;

import g6.fashionFlex.dto.CartItemSummaryDTO;
import g6.fashionFlex.dto.CartSummaryDTO;
import g6.fashionFlex.entity.Cart;
import g6.fashionFlex.entity.CartItem;
import g6.fashionFlex.entity.Customer;
import g6.fashionFlex.entity.Order;
import g6.fashionFlex.entity.OrderItem;
import g6.fashionFlex.entity.ProductVariant;
import g6.fashionFlex.repository.CartItemRepository;
import g6.fashionFlex.repository.CartRepository;
import g6.fashionFlex.repository.CustomerRepository;
import g6.fashionFlex.repository.ProductVariantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.UUID;

@Service
public class CartService {

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private ProductVariantRepository productVariantRepository;

    @Autowired
    private CustomerRepository customerRepository;

    public Cart getOrCreateCartBySession(String sessionId) {
        final String sid = (sessionId == null || sessionId.isBlank())
                ? UUID.randomUUID().toString()
                : sessionId;
        return cartRepository.findBySessionIDAndStatus(sid, Cart.CartStatus.active)
                .orElseGet(() -> {
                    Cart c = new Cart();
                    c.setSessionID(sid);
                    c.setStatus(Cart.CartStatus.active);
                    return cartRepository.save(c);
                });
    }

    public Cart getOrCreateCartByCustomer(Integer customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found"));
        return cartRepository.findByCustomer(customer)
                .orElseGet(() -> {
                    Cart c = new Cart();
                    c.setCustomer(customer);
                    c.setStatus(Cart.CartStatus.active);
                    return cartRepository.save(c);
                });
    }

    public List<CartItem> getItems(Cart cart) {
        return cartItemRepository.findByCart(cart);
    }

    @Transactional
    public CartSummaryDTO addItemAndGetSummary(Cart cart, Integer variantId, int quantity, String selectedSize) {
        addItem(cart, variantId, quantity, selectedSize);
        return getCartSummary(cart);
    }

    private int getAvailableStock(ProductVariant variant) {
        if (variant == null || variant.getProduct() == null) {
            return Integer.MAX_VALUE;
        }
        Integer stockQty = variant.getProduct().getStockQuantity();
        return stockQty != null ? Math.max(stockQty, 0) : Integer.MAX_VALUE;
    }

    @Transactional
    public void addItem(Cart cart, Integer variantId, int quantity, String selectedSize) {
        if (quantity <= 0) quantity = 1;
        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new RuntimeException("Variant not found"));

        int available = getAvailableStock(variant);
        if (available <= 0) {
            throw new RuntimeException("Product is out of stock");
        }

        Optional<CartItem> existing = cartItemRepository.findByCartAndVariant(cart, variant);
        if (existing.isPresent()) {
            CartItem item = existing.get();
            int newQty = item.getQuantity() + quantity;
            if (newQty > available) {
                throw new RuntimeException("Not enough stock for this product");
            }
            item.setQuantity(newQty);
            if (selectedSize != null && !selectedSize.isBlank()) {
                item.setSelectedSize(selectedSize);
            }
            cartItemRepository.save(item);
        } else {
            if (quantity > available) {
                throw new RuntimeException("Not enough stock for this product");
            }
            CartItem item = new CartItem();
            item.setCart(cart);
            item.setVariant(variant);
            item.setQuantity(quantity);
            if (selectedSize != null && !selectedSize.isBlank()) {
                item.setSelectedSize(selectedSize);
            }
            cartItemRepository.save(item);
        }
    }

    @Transactional
    public void removeItem(Cart cart, Integer cartItemId) {
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new RuntimeException("Cart item not found"));
        
        if (!item.getCart().getCartID().equals(cart.getCartID())) {
            throw new RuntimeException("Cart item does not belong to this cart");
        }
        
        cartItemRepository.delete(item);
    }

    @Transactional
    public void updateItemQuantity(Cart cart, Integer cartItemId, int quantity) {
        if (quantity <= 0) {
            quantity = 1;
        }
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new RuntimeException("Cart item not found"));
        if (!item.getCart().getCartID().equals(cart.getCartID())) {
            throw new RuntimeException("Cart item does not belong to this cart");
        }
        ProductVariant variant = item.getVariant();
        int available = getAvailableStock(variant);
        if (available <= 0) {
            throw new RuntimeException("Product is out of stock");
        }
        if (quantity > available) {
            throw new RuntimeException("Not enough stock for this product");
        }
        item.setQuantity(quantity);
        cartItemRepository.save(item);
    }

    public CartSummaryDTO getCartSummary(Cart cart) {
        List<CartItem> items = cartItemRepository.findByCart(cart);
        List<CartItemSummaryDTO> itemDTOs = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        int totalQuantity = 0;

        for (CartItem item : items) {
            ProductVariant variant = item.getVariant();
            BigDecimal price = variant.getPrice() != null ? variant.getPrice() : BigDecimal.ZERO;
            BigDecimal lineTotal = price.multiply(BigDecimal.valueOf(item.getQuantity()));
            totalAmount = totalAmount.add(lineTotal);
            totalQuantity += item.getQuantity();

            String image = variant.getVariantImage();
            if (image == null || image.isBlank()) {
                if (variant.getProduct() != null) {
                    image = variant.getProduct().getMainImage();
                }
            }

            String productName = variant.getProduct() != null ? variant.getProduct().getName() : "Product";
            String variantLabel = buildVariantLabel(variant);

            CartItemSummaryDTO dto = new CartItemSummaryDTO(
                    item.getCartItemID(),
                    variant.getVariantID(),
                    productName,
                    variant.getSku(),
                    variantLabel,
                    item.getQuantity(),
                    price,
                    lineTotal,
                    image
            );
            itemDTOs.add(dto);
        }

        return new CartSummaryDTO(itemDTOs, totalAmount, totalQuantity);
    }

    @Transactional
    public void clearCart(Cart cart) {
        if (cart == null) {
            return;
        }
        List<CartItem> items = cartItemRepository.findByCart(cart);
        if (items != null && !items.isEmpty()) {
            cartItemRepository.deleteAll(items);
        }
    }

    @Transactional
    public void rebuildCartFromOrder(Cart cart, Order order) {
        if (cart == null || order == null || order.getOrderItems() == null) {
            return;
        }
        clearCart(cart);
        for (OrderItem orderItem : order.getOrderItems()) {
            if (orderItem.getVariant() == null || orderItem.getVariant().getVariantID() == null) {
                continue;
            }
            Integer variantId = orderItem.getVariant().getVariantID();
            int quantity = orderItem.getQuantity() != null ? orderItem.getQuantity() : 1;
            String selectedSize = orderItem.getSelectedSize();
            addItem(cart, variantId, quantity, selectedSize);
        }
    }

    private String buildVariantLabel(ProductVariant variant) {
        if (variant == null) {
            return "";
        }
        StringJoiner joiner = new StringJoiner(" / ");
        if (variant.getColor() != null && !variant.getColor().isBlank()) {
            joiner.add(variant.getColor());
        }
        if (variant.getSize() != null && !variant.getSize().isBlank()) {
            joiner.add(variant.getSize());
        }
        if (joiner.length() > 0) {
            return joiner.toString();
        }
        return variant.getSku() != null ? variant.getSku() : "";
    }
}


