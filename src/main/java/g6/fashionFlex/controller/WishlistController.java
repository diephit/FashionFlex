package g6.fashionFlex.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import g6.fashionFlex.entity.Customer;
import g6.fashionFlex.entity.Product;
import g6.fashionFlex.entity.ProductVariant;
import g6.fashionFlex.entity.Wishlist;
import g6.fashionFlex.entity.WishlistItem;
import g6.fashionFlex.repository.ProductRepository;
import g6.fashionFlex.repository.WishlistItemRepository;
import g6.fashionFlex.repository.WishlistRepository;
import jakarta.servlet.http.HttpSession;

@Controller
public class WishlistController extends BaseController {

    @Autowired
    private WishlistRepository wishlistRepository;

    @Autowired
    private WishlistItemRepository wishlistItemRepository;

    @Autowired
    private ProductRepository productRepository;

    @GetMapping("/wishlist")
    public String viewWishlist(HttpSession session, Model model) {
        addAuthenticationToModel(model, session);

        Optional<Customer> customerOpt = getAuthenticatedCustomer();
        if (customerOpt.isEmpty()) {
            return "redirect:/login";
        }
        Customer customer = customerOpt.get();
        Optional<Wishlist> wishlistOpt = wishlistRepository.findByCustomer(customer);

        if (wishlistOpt.isPresent()) {
            Wishlist wishlist = wishlistOpt.get();
            List<WishlistItem> items = wishlistItemRepository.findByWishlist(wishlist);
            model.addAttribute("wishlistItems", items);
        } else {
            model.addAttribute("wishlistItems", List.of());
        }

        model.addAttribute("customer", customer);
        return "wishlist";
    }

    @PostMapping(value = "/wishlist/add", produces = "application/json")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> addToWishlist(
            @RequestParam Integer productId,
            @RequestParam(required = false) Integer variantId,
            HttpSession session) {

        Map<String, Object> response = new HashMap<>();

        Optional<Customer> customerOpt = getAuthenticatedCustomer();
        if (customerOpt.isEmpty()) {
            response.put("success", false);
            response.put("message", "Please login to add to wishlist");
            return ResponseEntity.ok(response);
        }

        try {
            Customer customer = customerOpt.get();

            // Get or create wishlist
            Wishlist wishlist = wishlistRepository.findByCustomer(customer)
                    .orElseGet(() -> {
                        Wishlist w = new Wishlist();
                        w.setCustomer(customer);
                        return wishlistRepository.save(w);
                    });

            // Get product
            Optional<Product> productOpt = productRepository.findById(productId);
            if (productOpt.isEmpty()) {
                response.put("success", false);
                response.put("message", "Product not found");
                return ResponseEntity.ok(response);
            }

            Product product = productOpt.get();

            // Check if product already in wishlist
            Optional<WishlistItem> existingItem = wishlistItemRepository
                    .findByWishlistAndProduct(wishlist, product);

            if (existingItem.isPresent()) {
                // Remove if already exists (toggle)
                wishlistItemRepository.delete(existingItem.get());
                response.put("success", true);
                response.put("message", "Removed from wishlist");
                response.put("added", false);
            } else {
                // Add new item
                WishlistItem item = new WishlistItem();
                item.setWishlist(wishlist);
                item.setProduct(product);
                
                wishlistItemRepository.save(item);
                response.put("success", true);
                response.put("message", "Added to wishlist");
                response.put("added", true);
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error: " + e.getMessage());
            return ResponseEntity.ok(response);
        }
    }

    @PostMapping(value = "/wishlist/remove", produces = "application/json")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> removeFromWishlist(
            @RequestParam Integer wishlistItemId) {

        Map<String, Object> response = new HashMap<>();

        try {
            Optional<WishlistItem> itemOpt = wishlistItemRepository.findById(wishlistItemId);
            if (itemOpt.isEmpty()) {
                response.put("success", false);
                response.put("message", "Item not found");
                return ResponseEntity.ok(response);
            }

            wishlistItemRepository.delete(itemOpt.get());
            response.put("success", true);
            response.put("message", "Removed from wishlist");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error: " + e.getMessage());
            return ResponseEntity.ok(response);
        }
    }

    @GetMapping(value = "/wishlist/check/{productId}", produces = "application/json")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> checkWishlistStatus(@PathVariable Integer productId) {
        Map<String, Object> response = new HashMap<>();

        Optional<Customer> customerOpt = getAuthenticatedCustomer();
        if (customerOpt.isEmpty()) {
            response.put("inWishlist", false);
            return ResponseEntity.ok(response);
        }

        try {
            Customer customer = customerOpt.get();
            Optional<Wishlist> wishlistOpt = wishlistRepository.findByCustomer(customer);

            if (wishlistOpt.isPresent()) {
                Optional<Product> productOpt = productRepository.findById(productId);
                if (productOpt.isPresent()) {
                    Optional<WishlistItem> item = wishlistItemRepository
                            .findByWishlistAndProduct(wishlistOpt.get(), productOpt.get());
                    response.put("inWishlist", item.isPresent());
                } else {
                    response.put("inWishlist", false);
                }
            } else {
                response.put("inWishlist", false);
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("inWishlist", false);
            return ResponseEntity.ok(response);
        }
    }

    @GetMapping(value = "/wishlist/count", produces = "application/json")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getWishlistCount() {
        Map<String, Object> response = new HashMap<>();
        int count = 0;
        Optional<Customer> customerOpt = getAuthenticatedCustomer();
        if (customerOpt.isPresent()) {
            Optional<Wishlist> wishlistOpt = wishlistRepository.findByCustomer(customerOpt.get());
            if (wishlistOpt.isPresent()) {
                count = wishlistItemRepository.countByWishlist(wishlistOpt.get());
            }
        }
        response.put("count", count);
        return ResponseEntity.ok(response);
    }
    @GetMapping(value = "/api/product/{productId}/variants", produces = "application/json")
@ResponseBody
public ResponseEntity<List<Map<String, Object>>> getProductVariants(@PathVariable Integer productId) {
    try {
        Optional<Product> productOpt = productRepository.findById(productId);
        if (productOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Product product = productOpt.get();
        List<Map<String, Object>> variants = new ArrayList<>();
        
        for (ProductVariant variant : product.getVariants()) {
            Map<String, Object> variantData = new HashMap<>();
            variantData.put("variantID", variant.getVariantID());
            variantData.put("sku", variant.getSku());
            variantData.put("price", variant.getPrice());
            variants.add(variantData);
        }
        
        return ResponseEntity.ok(variants);
    } catch (Exception e) {
        return ResponseEntity.status(500).build();
    }
}
}