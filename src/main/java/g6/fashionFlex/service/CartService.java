package g6.fashionFlex.service;

import g6.fashionFlex.dto.CartItemSummaryDTO;
import g6.fashionFlex.dto.CartSummaryDTO;
import g6.fashionFlex.entity.Cart;
import g6.fashionFlex.entity.CartItem;
import g6.fashionFlex.entity.Customer;
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
    public CartSummaryDTO addItemAndGetSummary(Cart cart, Integer variantId, int quantity) {
        addItem(cart, variantId, quantity);
        return getCartSummary(cart);
    }

    @Transactional
    public void addItem(Cart cart, Integer variantId, int quantity) {
        if (quantity <= 0) quantity = 1;
        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new RuntimeException("Variant not found"));

        Optional<CartItem> existing = cartItemRepository.findByCartAndVariant(cart, variant);
        if (existing.isPresent()) {
            CartItem item = existing.get();
            item.setQuantity(item.getQuantity() + quantity);
            cartItemRepository.save(item);
        } else {
            CartItem item = new CartItem();
            item.setCart(cart);
            item.setVariant(variant);
            item.setQuantity(quantity);
            cartItemRepository.save(item);
        }
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

            CartItemSummaryDTO dto = new CartItemSummaryDTO(
                    item.getCartItemID(),
                    variant.getVariantID(),
                    productName,
                    variant.getSku(),
                    item.getQuantity(),
                    price,
                    lineTotal,
                    image
            );
            itemDTOs.add(dto);
        }

        return new CartSummaryDTO(itemDTOs, totalAmount, totalQuantity);
    }
}


