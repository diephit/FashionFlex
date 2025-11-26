package g6.fashionFlex.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Internal DTO for creating an order
 * Contains all necessary data from cart and checkout
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlaceOrderRequest {

    private Long userId;
    private Long cartId;

    // Shipping information
    private String shippingName;
    private String shippingPhone;
    private String shippingAddress; // Full address string
    private String shippingCity;
    private String shippingState;
    private String shippingZipCode;
    private String shippingCountry;

    // Payment method
    private String paymentMethod;

    // Order totals (from cart)
    private BigDecimal subtotal;
    private BigDecimal shippingCost;
    private BigDecimal tax;
    private BigDecimal discount;
    private BigDecimal totalAmount;

    // Coupon if applied
    private String couponCode;

    // Optional notes
    private String notes;

    // Option to save address
    private boolean saveAddress;

    // For new address creation
    private String fullName;
    private String phoneNumber;
    private String street;
    private String city;
    private String state;
    private String zipCode;
    private String country;
}
