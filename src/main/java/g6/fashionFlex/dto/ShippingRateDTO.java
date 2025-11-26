package g6.fashionFlex.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShippingRateDTO {

    private String method;
    private String description;
    private BigDecimal cost;
    private String estimatedDelivery;
}
