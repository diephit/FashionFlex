package g6.fashionFlex.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartItemDTO {

    private Long id;
    private Long productId;
    private String productName;
    private String productImageUrl;
    private String productSku;
    private Integer quantity;
    private String size;
    private String color;
    private BigDecimal price;
    private BigDecimal discountPrice;
    private BigDecimal effectivePrice;
    private BigDecimal itemTotal;
    private BigDecimal savings;
    private Boolean inStock;
    private Integer availableStock;
    private String stockMessage;
}
