package g6.fashionFlex.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class CartSummaryDTO {
    private List<CartItemSummaryDTO> items = new ArrayList<>();
    private BigDecimal totalAmount = BigDecimal.ZERO;
    private Integer totalQuantity = 0;

    public CartSummaryDTO() {
    }

    public CartSummaryDTO(List<CartItemSummaryDTO> items, BigDecimal totalAmount, Integer totalQuantity) {
        this.items = items;
        this.totalAmount = totalAmount;
        this.totalQuantity = totalQuantity;
    }

    public List<CartItemSummaryDTO> getItems() {
        return items;
    }

    public void setItems(List<CartItemSummaryDTO> items) {
        this.items = items;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public Integer getTotalQuantity() {
        return totalQuantity;
    }

    public void setTotalQuantity(Integer totalQuantity) {
        this.totalQuantity = totalQuantity;
    }
}

