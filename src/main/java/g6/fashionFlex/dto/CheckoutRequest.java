package g6.fashionFlex.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for checkout form data
 * Used when user submits checkout page
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutRequest {

    // Shipping address can be from saved addresses or new
    private Long savedAddressId; // If user selects from address book

    // If savedAddressId is null, use these fields for new address
    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotBlank(message = "Phone number is required")
    private String phoneNumber;

    @NotBlank(message = "Street address is required")
    private String street;

    @NotBlank(message = "City is required")
    private String city;

    private String state;

    private String zipCode;

    @NotBlank(message = "Country is required")
    private String country;

    // Option to save this address for future use
    private boolean saveAddress = false;

    // Payment method selection
    @NotBlank(message = "Payment method is required")
    private String paymentMethod; // CASH_ON_DELIVERY, CREDIT_CARD, etc.

    // Optional order notes
    private String notes;
}
