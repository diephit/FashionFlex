package g6.fashionFlex.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartDTO {

    private Long id;
    private Long userId;
    private List<CartItemDTO> items = new ArrayList<>();
    private BigDecimal subtotal;
    private BigDecimal discountAmount;
    private BigDecimal total;
    private Integer totalItems;
    private String couponCode;
    private Boolean hasOutOfStockItems;
    private List<String> stockWarnings;

    // Shipping fields
    private String selectedShippingMethod;
    private BigDecimal shippingCost;
    private BigDecimal taxAmount;
    private BigDecimal finalTotal;
    private String shippingCountry;
    private String shippingState;
    private String shippingPostcode;
    private Boolean hasShippingMethod;

    // For header cart sidebar - indicates if there are more items than displayed
    private Boolean hasMoreItems;
}
