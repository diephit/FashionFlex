package g6.fashionFlex.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for order confirmation page
 * Contains essential order details to show to customer after successful checkout
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderConfirmationDTO {

    private Long orderId;
    private String orderNumber;
    private LocalDateTime orderDate;

    // Customer info
    private String customerName;
    private String customerEmail;

    // Shipping info
    private String shippingAddress;
    private String shippingPhone;
    private String estimatedDelivery; // Calculated based on shipping method

    // Order items
    private List<OrderItemDTO> orderItems;
    private int totalItems;

    // Payment info
    private String paymentMethod;
    private String paymentStatus;

    // Order totals
    private BigDecimal subtotal;
    private BigDecimal shippingCost;
    private BigDecimal tax;
    private BigDecimal discount;
    private BigDecimal totalAmount;

    // Status
    private String orderStatus;

    // Messages
    private String successMessage;
    private String nextSteps;
}
