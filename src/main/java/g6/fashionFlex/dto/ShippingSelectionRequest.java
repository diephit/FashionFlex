package g6.fashionFlex.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShippingSelectionRequest {

    @NotBlank(message = "Shipping method is required")
    private String shippingMethod;

    @NotNull(message = "Shipping cost is required")
    private BigDecimal shippingCost;

    @NotBlank(message = "Country is required")
    private String country;

    private String state;

    @NotBlank(message = "Postcode is required")
    private String postcode;
}
